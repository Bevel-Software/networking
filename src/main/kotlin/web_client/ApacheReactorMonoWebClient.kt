package software.bevel.networking.web_client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.net.URIBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import software.bevel.networking.ReactorMonoResponse
import java.net.URI

/**
 * An implementation of [ReactorMonoWebClient] that uses Apache HttpClient 5 for making HTTP requests.
 * It provides methods for sending POST and GET requests, both asynchronously (returning [ReactorMonoResponse])
 * and synchronously (blocking and returning a String).
 */
class ApacheReactorMonoWebClient(
    private val logger: Logger = LoggerFactory.getLogger(ApacheReactorMonoWebClient::class.java)
): ReactorMonoWebClient {
    /**
     * Sends an asynchronous POST request to the specified URL using Apache HttpClient.
     * The request body, headers, and URL parameters are configurable.
     * If a "Content-Type" header is not explicitly provided, it defaults to "application/json".
     *
     * @param url The target URL for the POST request.
     * @param body The string representation of the request body (typically JSON).
     * @param headers A list of key-value pairs for request headers.
     * @param parameters A list of key-value pairs for URL query parameters.
     * @return A [ReactorMonoResponse] containing a [Mono] that will emit the response body as a string upon success,
     *         or an error if the request fails or the server returns a non-200 status code.
     */
    override fun sendPostRequest(url: String, body: String, headers: List<Pair<String, String>>, parameters: List<Pair<String, String>>): ReactorMonoResponse<String> {
        return ReactorMonoResponse(Mono.create { sink ->
            try {
                val uri = if (parameters.isNotEmpty()) {
                    val uriBuilder = URIBuilder(URI.create(url))
                    parameters.forEach { (k, v) -> uriBuilder.addParameter(k, v) }
                    uriBuilder.build()
                } else {
                    URI.create(url)
                }

                val request = HttpPost(uri).apply {
                    entity = StringEntity(body, ContentType.APPLICATION_JSON)
                    headers.forEach { (key, value) -> addHeader(key, value) }
                    if (!headers.any { it.first.lowercase() == "content-type" }) {
                        addHeader("content-type", "application/json")
                    }
                }

                HttpClientBuilder.create().build().use { client ->
                    client.execute(request) { response ->
                        val responseBody = response.entity.content.bufferedReader().use { it.readText() }
                        if (response.code == 200) {
                            sink.success(responseBody)
                        } else {
                            logger.error("${response.code} $responseBody")
                            sink.error(Exception("Failed to get response from $url"))
                        }
                    }
                }
            } catch (e: Exception) {
                sink.error(e)
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
        try {
            return sendPostRequest(url, body, headers, parameters).response.block() ?: ""
        } catch (e: Exception) {
            logger.error("[ApacheReactorMonoWebClient] Failed to get response from $url", e)
        }
        return ""
    }

    /**
     * Sends an asynchronous GET request to the specified URL using Apache HttpClient.
     * Headers and URL parameters are configurable.
     * If a "Content-Type" header is not explicitly provided, it defaults to "application/json".
     * This method includes specific logic to parse a JSON response, expecting a structure similar to
     * OpenAI API responses (extracting `content` from `response.choices[0].message.content`).
     *
     * @param url The target URL for the GET request.
     * @param headers A list of key-value pairs for request headers.
     * @param parameters A list of key-value pairs for URL query parameters.
     * @return A [ReactorMonoResponse] containing a [Mono] that will emit the extracted content string upon success,
     *         or an error if the request fails, the server returns a non-200 status code, or parsing fails.
     */
    override fun sendGetRequest(url: String, headers: List<Pair<String, String>>, parameters: List<Pair<String, String>>): ReactorMonoResponse<String> {
        return ReactorMonoResponse(Mono.create { sink ->
            try {
                val uri = if (parameters.isNotEmpty()) {
                    val uriBuilder = URIBuilder(URI.create(url))
                    parameters.forEach { (k, v) -> uriBuilder.addParameter(k, v) }
                    uriBuilder.build()
                } else {
                    URI.create(url)
                }

                val request = HttpGet(uri).apply {
                    headers.forEach { (key, value) -> addHeader(key, value) }
                    if (!headers.any { it.first.lowercase() == "content-type" }) {
                        addHeader("content-type", "application/json")
                    }
                }

                HttpClientBuilder.create().build().use { client ->
                    client.execute(request) { response ->
                        val responseBody = response.entity.content.bufferedReader().use { it.readText() }
                        if (response.code == 200) {
                            try {
                                // Specific parsing for OpenAI-like response structure
                                val respBody = jacksonObjectMapper().readValue(responseBody, Map::class.java)
                                val content =
                                    (((respBody["choices"] as List<*>).first() as Map<*, *>)["message"] as Map<*, *>)["content"] as String
                                sink.success(content)
                            } catch (e: Exception) {
                                // If specific parsing fails, attempt to return the whole body or error out
                                logger.warn("Failed to parse specific GET response structure from $url, returning raw body or erroring: ${e.message}")
                                // Depending on requirements, could sink.success(responseBody) or sink.error(e)
                                sink.error(Exception("Failed to parse response from $url: ${e.message}", e))
                            }
                        } else {
                            logger.error("${response.code} $responseBody")
                            sink.error(Exception("Failed to get response from $url"))
                        }
                    }
                }
            } catch (e: Exception) {
                sink.error(e)
            }
        })
    }

    /**
     * Sends a synchronous GET request by calling the asynchronous [sendGetRequest] method and blocking for its result.
     *
     * @param url The target URL for the GET request.
     * @param headers A list of key-value pairs for request headers.
     * @param parameters A list of key-value pairs for URL query parameters.
     * @return The extracted content string from the response, or an empty string if the request fails or the underlying [Mono] is empty.
     */
    override fun sendGetRequestBlocking(url: String, headers: List<Pair<String, String>>, parameters: List<Pair<String, String>>): String {
        try {
            return sendGetRequest(url, headers, parameters).response.block() ?: ""
        } catch (e: Exception) {
            logger.error("[ApacheReactorMonoWebClient] Failed to get response from $url", e)
        }
        return ""
    }
}
