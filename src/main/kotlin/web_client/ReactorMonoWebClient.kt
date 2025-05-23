package software.bevel.networking.web_client

import software.bevel.networking.ReactorMonoResponse

/**
 * An interface that extends [ReactorWebClient], specializing the response type to [ReactorMonoResponse] of String.
 * Implementations of this interface are expected to handle HTTP requests and return responses wrapped in [ReactorMonoResponse],
 * which encapsulates a Project Reactor [reactor.core.publisher.Mono] for asynchronous processing of the string response body.
 */
interface ReactorMonoWebClient: ReactorWebClient<ReactorMonoResponse<String>>