package software.bevel.networking.web_client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import reactor.core.publisher.Mono
import software.bevel.domain.BevelLogger
import software.bevel.networking.ReactorMonoResponse
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * An implementation of [ReactorMonoWebClient] that uses Java's built-in `java.net.http.HttpClient`
 * for making HTTP requests. It supports asynchronous operations by wrapping `CompletableFuture`
 * responses from the HTTP client into Project Reactor `Mono` objects.
 * The client is configured with a default connection timeout and supports proxy configuration
 * via the `HTTPS_PROXY` environment variable.
 */
class JavaNetReactorMonoWebClient: ReactorMonoWebClient {
    /**
     * The URI for the HTTPS proxy, derived from the `HTTPS_PROXY` environment variable.
     * If the environment variable is not set, this will be `null`.
     */
    val proxyURI = System.getenv("HTTPS_PROXY")?.let { URI.create(it) }

    /**
     * The configured `java.net.http.HttpClient` instance used for all HTTP requests.
     * It is built with a connection timeout of 600 seconds.
     * If [proxyURI] is not null, the client is configured to use the specified proxy.
     */
    val client = HttpClient.newBuilder()
        .let {
            if(proxyURI != null) {
                it.proxy(ProxySelector.of(InetSocketAddress(proxyURI.host, proxyURI.port)))
            } else {
                it
            }
        }
        .connectTimeout(Duration.ofSeconds(600))
        .build()

    /**
     * Sends an asynchronous POST request to the specified URL using `java.net.http.HttpClient`.
     * The request body, headers, and URL parameters are configurable. A 600-second timeout is applied to the request.
     * If a "Content-Type" header is not explicitly provided, it defaults to "application/json".
     *
     * @param url The target URL for the POST request.
     * @param body The string representation of the request body (typically JSON).
     * @param headers A list of key-value pairs for request headers.
     * @param parameters A list of key-value pairs for URL query parameters.
     * @return A [ReactorMonoResponse] containing a [Mono] that will emit the response body as a string upon success (HTTP 200),
     *         or an error if the request fails or the server returns a non-200 status code.
     */
    override fun sendPostRequest(url: String, body: String, headers: List<Pair<String, String>>, parameters: List<Pair<String, String>>): ReactorMonoResponse<String> {
        return ReactorMonoResponse(Mono.create { sink ->
            val uri = if(parameters.isNotEmpty()) {
                val urlParams = parameters.map {(k, v) -> "$k=$v"}.joinToString("&")
                URI.create("$url?$urlParams")
            } else {
                URI.create(url)
            }


            var requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(600))

            for ((key, value) in headers) {
                requestBuilder = requestBuilder.header(key, value)
            }
            if(!headers.any { it.first.lowercase() == "content-type"})
                requestBuilder = requestBuilder.header("content-type", "application/json")

            val request = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept { response ->
                    if (response.statusCode() == 200) {
                        sink.success(response.body())
                    } else {
                        BevelLogger.logger.error("${response.statusCode()} ${response.body()}")
                        sink.error(Exception("Failed to get response from $url"))
                    }
                }
                .exceptionally { throwable ->
                    sink.error(throwable)
                    null
                }
        })
    }

    /**
     * Sends a synchronous POST request by calling the asynchronous [sendPostRequest] method and blocking for its result.
     *
     * @param url The target URL for the POST request.
     * @param body The string representation of the request body (typically JSON).
     * @param headers A list of key-value pairs for request headers.
     * @param parameters A list of key-value pairs for URL query parameters.
     * @return The response body as a string, or an empty string if the request fails or the underlying [Mono] is empty.
     */
    override fun sendPostRequestBlocking(url: String, body: String, headers: List<Pair<String, String>>, parameters: List<Pair<String, String>>): String {
        return sendPostRequest(url, body, headers, parameters).response.block() ?: ""
    }

    /**
     * Sends an asynchronous GET request to the specified URL using `java.net.http.HttpClient`.
     * Headers and URL parameters are configurable. A 600-second timeout is applied to the request.
     * If a "Content-Type" header is not explicitly provided, it defaults to "application/json".
     *
     * @param url The target URL for the GET request.
     * @param headers A list of key-value pairs for request headers.
     * @param parameters A list of key-value pairs for URL query parameters.
     * @return A [ReactorMonoResponse] containing a [Mono] that will emit the response body as a string upon success (HTTP 200),
     *         or an error if the request fails or the server returns a non-200 status code.
     */
    override fun sendGetRequest(url: String, headers: List<Pair<String, String>>, parameters: List<Pair<String, String>>): ReactorMonoResponse<String> {
        return ReactorMonoResponse(Mono.create { sink ->
            val uri = if(parameters.isNotEmpty()) {
                val urlParams = parameters.map {(k, v) -> "$k=$v"}.joinToString("&")
                URI.create("$url?$urlParams")
            } else {
                URI.create(url)
            }

            var requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(600))

            for ((key, value) in headers) {
                requestBuilder = requestBuilder.header(key, value)
            }
            if(!headers.any { it.first.lowercase() == "content-type"})
                requestBuilder = requestBuilder.header("content-type", "application/json")

            val request = requestBuilder
                .GET()
                .build()

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept { response ->
                    if (response.statusCode() == 200) {
                        sink.success(response.body())
                    } else {
                        BevelLogger.logger.error("${response.statusCode()} ${response.body()}")
                        sink.error(Exception("Failed to get response from $url"))
                    }
                }
                .exceptionally { throwable ->
                    sink.error(throwable)
                    null
                }
        })
    }

    /**
     * Sends a synchronous GET request by calling the asynchronous [sendGetRequest] method and blocking for its result.
     *
     * @param url The target URL for the GET request.
     * @param headers A list of key-value pairs for request headers.
     * @param parameters A list of key-value pairs for URL query parameters.
     * @return The response body as a string, or an empty string if the request fails or the underlying [Mono] is empty.
     */
    override fun sendGetRequestBlocking(url: String, headers: List<Pair<String, String>>, parameters: List<Pair<String, String>>): String {
        return sendGetRequest(url, headers, parameters).response.block() ?: ""
    }
}