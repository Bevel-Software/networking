package software.bevel.networking

import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.bevel.file_system_domain.web.Blockable
import software.bevel.file_system_domain.web.Subscribable
import software.bevel.file_system_domain.web.WebResponse
import java.util.function.Consumer

/**
 * A sealed class representing a web response that is backed by a Project Reactor [Publisher] (e.g., [Mono] or [Flux]).
 * This class acts as a common base for specific Reactor-based response types.
 *
 * @param T The type of the data contained in the response.
 * @param PUBLISHER_TYPE The specific type of the Reactor [Publisher] (e.g., `Mono<T>`, `Flux<T>`).
 * @property response The underlying Reactor [Publisher] instance holding the response data.
 */
sealed class ReactorWebResponse<T, out PUBLISHER_TYPE: Publisher<out T>>(
    val response: PUBLISHER_TYPE
): WebResponse<T>

/**
 * A [ReactorWebResponse] specifically for responses represented by a [Mono].
 * Implements [Blockable] to allow synchronous retrieval of the single emitted item (or null)
 * and [Subscribable] to allow for asynchronous consumption of the item.
 *
 * @param T The type of the data contained in the [Mono] response.
 * @param response The underlying [Mono] instance.
 */
class ReactorMonoResponse<T>(response: Mono<T>): ReactorWebResponse<T, Mono<T>>(response), Blockable<T>, Subscribable<T> {
    /**
     * Blocks indefinitely until the underlying [Mono] emits an item or completes, then returns that item.
     * Returns `null` if the [Mono] completes without emitting an item.
     * Implements [Blockable.block].
     *
     * @return The item emitted by the [Mono], or `null` if none.
     */
    override fun block(): T? {
        return response.block()
    }

    /**
     * Subscribes to the underlying [Mono] and invokes the given [Consumer] with the emitted item.
     * Implements [Subscribable.subscribe].
     *
     * @param consumer The [Consumer] to be called with the item emitted by the [Mono].
     */
    override fun subscribe(consumer: Consumer<T>) {
        response.subscribe(consumer)
    }

    /**
     * Subscribes to the underlying [Mono] to trigger its execution, without providing a specific consumer.
     * Useful when the side effects of the [Mono]'s execution are desired, but not its result.
     * Implements [Subscribable.subscribe].
     */
    override fun subscribe() {
        response.subscribe()
    }
}

/**
 * A [ReactorWebResponse] specifically for responses represented by a [Flux].
 * Implements [Blockable] to allow synchronous retrieval of all emitted items as a [List]
 * and [Subscribable] to allow for asynchronous consumption of individual items.
 *
 * @param T The type of the data items contained in the [Flux] response.
 * @param response The underlying [Flux] instance.
 */
class ReactorFluxResponse<T>(response: Flux<T>): ReactorWebResponse<T, Flux<T>>(response), Blockable<List<T>>, Subscribable<T> {
    /**
     * Blocks indefinitely until the underlying [Flux] completes, collects all emitted items into a [List], and returns it.
     * Returns `null` if the [Flux] completes without emitting any items (resulting in an empty list which `block()` might return as null, though typically it's an empty list).
     * Implements [Blockable.block] for a [List] of [T].
     *
     * @return A [List] containing all items emitted by the [Flux], or `null` (or an empty list) if none.
     */
    override fun block(): List<T>? {
        return response.collectList().block()
    }

    /**
     * Subscribes to the underlying [Flux] and invokes the given [Consumer] with each emitted item.
     * Implements [Subscribable.subscribe].
     *
     * @param consumer The [Consumer] to be called with each item emitted by the [Flux].
     */
    override fun subscribe(consumer: Consumer<T>) {
        response.subscribe(consumer)
    }

    /**
     * Subscribes to the underlying [Flux] to trigger its execution, without providing a specific consumer.
     * Useful when the side effects of the [Flux]'s execution are desired, but not its results directly.
     * Implements [Subscribable.subscribe].
     */
    override fun subscribe() {
        response.subscribe()
    }
}