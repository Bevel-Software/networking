package software.bevel.networking

import software.bevel.file_system_domain.web.CommunicationInterfaceCreator
import software.bevel.networking.web_client.ReactorWebClient

/**
 * A factory class responsible for creating instances of [RestCommunicationInterface].
 * It implements the [CommunicationInterfaceCreator] interface, providing a standardized way
 * to obtain a communication interface.
 *
 * @property webClient The [ReactorWebClient] instance that will be used by the created [RestCommunicationInterface].
 *                     This client is passed to the [RestCommunicationInterface] upon creation.
 * @property port The port number that the created [RestCommunicationInterface] will use to communicate
 *                with the target REST API. This is also passed to the [RestCommunicationInterface].
 */
class RestCommunicationInterfaceCreator(
    private val webClient: ReactorWebClient<*>,
    private val port: String
): CommunicationInterfaceCreator<RestCommunicationInterface> {
    /**
     * Creates and returns a new instance of [RestCommunicationInterface].
     * This method implements [CommunicationInterfaceCreator.create].
     *
     * @return A new [RestCommunicationInterface] configured with the `webClient` and `port`
     *         provided during the creator's instantiation.
     */
    override fun create(): RestCommunicationInterface {
        return RestCommunicationInterface(webClient, port)
    }
}