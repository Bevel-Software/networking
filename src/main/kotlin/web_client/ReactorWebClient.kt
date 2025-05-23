package software.bevel.networking.web_client

import software.bevel.file_system_domain.web.WebClient
import software.bevel.networking.ReactorWebResponse

/**
 * A generic web client interface that extends [WebClient].
 * It is designed to work with response types that are subtypes of [ReactorWebResponse],
 * specifically those that handle a String response body and any type of reactive publisher (e.g., Mono, Flux).
 *
 * @param T The specific type of [ReactorWebResponse] that this client will handle.
 *          It must be a [ReactorWebResponse] that processes a [String] body and uses some reactive publisher.
 */
interface ReactorWebClient<T: ReactorWebResponse<String, *>>: WebClient<T>