package software.bevel.networking

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.bevel.file_system_domain.BevelFilesPathResolver
import software.bevel.file_system_domain.services.FileHandler
import software.bevel.file_system_domain.web.LocalCommunicationInterface
import software.bevel.file_system_domain.web.Subscribable
import software.bevel.file_system_domain.web.WebClient

/**
 * Implements [LocalCommunicationInterface] to interact with a RESTful API, typically running locally.
 * It uses a [WebClient] to send HTTP requests to predefined endpoints.
 *
 * @property webClient The underlying [WebClient] used to make HTTP requests.
 * @property port The port number on which the target REST API is running (on localhost).
 */
class RestCommunicationInterface(
    private val webClient: WebClient<*>,
    private val port: String,
    private val logger: Logger = LoggerFactory.getLogger(RestCommunicationInterface::class.java)
): LocalCommunicationInterface {
    /**
     * The base URL for all API requests, constructed as `http://localhost:{port}/api`.
     */
    val baseUrl = "http://localhost:$port/api"

    /**
     * Sends a string message as a POST request to the `/command` endpoint and waits for a blocking response.
     * This method is part of the [LocalCommunicationInterface].
     *
     * @param message The string message to send in the request body.
     * @return The response string from the server.
     */
    override fun send(message: String): String {
        return webClient.sendPostRequestBlocking("$baseUrl/command", message)
    }

    /**
     * Serializes a generic message object to a JSON string, sends it as a POST request to the `/command` endpoint,
     * and waits for a blocking response.
     * This method is part of the [LocalCommunicationInterface].
     *
     * @param T The type of the message object.
     * @param message The message object to serialize and send.
     * @return The response string from the server.
     */
    override fun <T> send(message: T): String {
        //BevelLogger.logger.info("Sending batch, " + jacksonObjectMapper().writeValueAsString(message).length + " bytes")
        return webClient.sendPostRequestBlocking("$baseUrl/command", jacksonObjectMapper().writeValueAsString(message))
    }

    /**
     * Sends a string message as a POST request to the `/command` endpoint without waiting for a specific response content.
     * If the underlying [WebClient] returns a [Subscribable] response, it subscribes to trigger the request.
     * This method is part of the [LocalCommunicationInterface].
     *
     * @param message The string message to send in the request body.
     */
    override fun sendWithoutResponse(message: String) {
        val response = webClient.sendPostRequest("$baseUrl/command", message)
        if(response is Subscribable<*>) {
            response.subscribe()
        }
    }

    /**
     * Checks if the REST API is reachable and responsive by sending a GET request to the `/isAlive` endpoint.
     * This method is part of the [LocalCommunicationInterface].
     *
     * @return `true` if the server responds with `true` (as a JSON boolean), `false` otherwise or if an error occurs.
     */
    override fun isConnected(): Boolean {
        try {
            val result = webClient.sendGetRequestBlocking("$baseUrl/isAlive")
            if(result == "")
                return false
            return jacksonObjectMapper().readValue(result, Boolean::class.java)
        } catch (e: Exception) {
            logger.error("Failed to check connection, most likely connection partner not running")
            return false
        }
    }

    /**
     * Closes the communication interface. Currently, this method has no specific implementation for the REST interface.
     * This method is part of the [LocalCommunicationInterface].
     */
    override fun close() {

    }
}