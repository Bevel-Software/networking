package software.bevel.networking

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.Socket
import java.io.BufferedReader
import java.io.PrintWriter
import java.io.InputStreamReader
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import software.bevel.file_system_domain.web.LocalCommunicationInterface

/**
 * Start token used to mark the beginning of a message sent over the socket.
 */
const val START_TOKEN = "_!START_"
/**
 * End token used to mark the end of a message sent over the socket.
 */
const val END_TOKEN = "_!END_"

/**
 * Implements [LocalCommunicationInterface] to communicate with a server over a local socket connection.
 * It handles sending and receiving string-based messages, framing them with [START_TOKEN] and [END_TOKEN].
 * JSON serialization is used for generic type messages.
 *
 * @param port The port number to connect to on localhost.
 */
class LocalSocket(
    port: Int,
    private val logger: Logger = LoggerFactory.getLogger(LocalSocket::class.java)
): LocalCommunicationInterface {
    private var clientSocket: Socket? = null
    private var out: PrintWriter? = null
    private var `in`: BufferedReader? = null
    private val messagesSink = Sinks.many().multicast().onBackpressureBuffer<String>()
    /**
     * A [Flux] that emits messages received from the socket.
     * Note: The current implementation does not actively push messages to this sink upon receipt.
     * This Flux is available for future enhancements where asynchronous message receiving might be implemented.
     */
    val messages: Flux<String> = messagesSink.asFlux()
    private var currentPort: Int? = null

    /**
     * Initializes the connection to the specified port upon instantiation.
     */
    init {
        connect(port)
    }

    /**
     * Establishes a connection to the server on the specified port at localhost.
     * Initializes the input and output streams for socket communication.
     *
     * @param port The port number to connect to.
     * @return `true` if the connection was successful, `false` otherwise.
     */
    fun connect(port: Int): Boolean {
        try {
            clientSocket = Socket("localhost", port)
            out = clientSocket?.getOutputStream()?.let { PrintWriter(it, true) }
            `in` = clientSocket?.getInputStream()?.let { BufferedReader(InputStreamReader(it)) }
            logger.info("Connected to server on port $port")
            currentPort = port
            return true
            // Start listening for messages
            //startMessageListener()
        } catch (e: Exception) {
            logger.info("Error connecting to server: ${e.message}")
            return false
        }
    }

    /**
     * Closes the input stream, output stream, and the client socket.
     * This method is part of the [LocalCommunicationInterface].
     */
    override fun close() {
        `in`?.close()
        out?.close()
        clientSocket?.close()
        logger.info("Closed connection to server on port $currentPort")
    }

    /**
     * Sends a raw string message directly over the socket without any special framing.
     * Throws [IllegalStateException] if the socket connection is not established.
     *
     * @param message The string message to send.
     */
    fun sendOverSocket(message: String) {
        out?.println(message) ?: throw IllegalStateException("Socket connection not established")
    }

    /**
     * Receives a raw string message (a single line) directly from the socket.
     * Throws [IllegalStateException] if the socket connection is not established.
     *
     * @return The string message received from the socket.
     */
    fun receive(): String {
        return `in`?.readLine() ?: throw IllegalStateException("Socket connection not established")
    }

    /**
     * Sends a string message, framed by [START_TOKEN] and [END_TOKEN], and waits for a response.
     * This method is part of the [LocalCommunicationInterface].
     *
     * @param message The string message to send.
     * @return The response string received from the server.
     */
    override fun send(message: String): String {
        sendOverSocket(START_TOKEN)
        sendOverSocket(message)
        sendOverSocket(END_TOKEN)
        return receive()
    }

    /**
     * Serializes a generic message object to a JSON string, sends it framed by [START_TOKEN] and [END_TOKEN],
     * and waits for a response.
     * This method is part of the [LocalCommunicationInterface].
     *
     * @param T The type of the message object.
     * @param message The message object to send.
     * @return The response string received from the server.
     */
    override fun <T> send(message: T): String {
        val stringMessage = jacksonObjectMapper().writeValueAsString(message)
        return send(stringMessage)
    }

    /**
     * Sends a string message, framed by [START_TOKEN] and [END_TOKEN], without waiting for a response.
     * This method is part of the [LocalCommunicationInterface].
     *
     * @param message The string message to send.
     */
    override fun sendWithoutResponse(message: String) {
        sendOverSocket(START_TOKEN)
        sendOverSocket(message)
        sendOverSocket(END_TOKEN)
    }

    /**
     * Checks if the socket is currently connected and not closed.
     * This method is part of the [LocalCommunicationInterface].
     *
     * @return `true` if the socket is connected, `false` otherwise.
     */
    override fun isConnected(): Boolean {
        return clientSocket?.isConnected == true && !clientSocket?.isClosed!!
    }
}
