# Networking (Kotlin Library for Bevel)

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/software.bevel/networking.svg?label=Maven%20Central)](https://search.maven.org/artifact/software.bevel/networking/1.0.0/jar)

The `networking` library is a Kotlin/JVM component within the Bevel suite of developer tools. It provides client-side networking capabilities, including abstractions for reactive HTTP/REST communication and simple local socket-based inter-process communication. This library is designed to integrate seamlessly with other Bevel components, particularly by implementing communication interfaces defined in the `file-system-domain` library.

## Relationship to `file-system-domain`

The `networking` library builds upon and provides concrete implementations for several communication interfaces defined in the [Bevel `file-system-domain`](https://github.com/Bevel-Software/file-system-domain) library. These include:

*   `software.bevel.file_system_domain.web.LocalCommunicationInterface`: Implemented by `RestCommunicationInterface` and `LocalSocket`.
*   `software.bevel.file_system_domain.web.WebClient`: Implemented by various reactive web clients in this library (e.g., `ApacheReactorMonoWebClient`, `JavaNetReactorMonoWebClient`).
*   `software.bevel.file_system_domain.web.WebResponse`: Extended by `ReactorWebResponse` and its variants.
*   `software.bevel.file_system_domain.web.CommunicationInterfaceCreator`: Implemented by `RestCommunicationInterfaceCreator`.

This design promotes loose coupling and allows other Bevel tools to depend on the `file-system-domain` interfaces while using `networking` as a runtime implementation.

## Key Features

*   **Reactive HTTP Client Abstractions:** Provides `ReactorWebClient` and `ReactorMonoWebClient` interfaces for building non-blocking, reactive HTTP clients.
*   **Multiple HTTP Client Backends:**
    *   `JavaNetReactorMonoWebClient`: Uses Java's built-in `java.net.http.HttpClient` (Java 11+), supporting system proxy settings (e.g., `HTTPS_PROXY`).
    *   `ApacheReactorMonoWebClient`: Uses Apache HttpClient 5, offering robust HTTP communication capabilities.
*   **REST Communication Interface:** `RestCommunicationInterface` simplifies interaction with local RESTful APIs, built on top of the `WebClient` abstraction.
*   **Local Socket Communication:** `LocalSocket` provides a simple, framed TCP/IP socket communication mechanism for local inter-process messaging.
*   **Project Reactor Integration:** Leverages Project Reactor (`Mono`, `Flux`) for asynchronous and reactive programming patterns via `ReactorMonoResponse` and `ReactorFluxResponse`.
*   **JSON Serialization:** Utilizes Jackson for serializing and deserializing objects (e.g., for message payloads).
*   **Clear Dependency Structure:** Built to work with interfaces from `software.bevel:file-system-domain`.
*   **Maven Central Availability:** Easily integrate into any JVM project.

## Installation / Getting Started

### Prerequisites

*   Java Development Kit (JDK) 17 or higher.
*   A build tool like Gradle or Maven.

### Adding as a Dependency

The library is available on Maven Central. You will also need to include `file-system-domain` as it provides core interfaces used by `networking`.

**Gradle (Kotlin DSL - `build.gradle.kts`):**
```kotlin
dependencies {
    implementation("software.bevel:networking:1.0.0")
    implementation("software.bevel:file-system-domain:1.0.0") // Required peer dependency
    // Other dependencies like Jackson, Reactor, SLF4J are transitive
}
```

**Gradle (Groovy DSL - `build.gradle`):**
```groovy
dependencies {
    implementation 'software.bevel:networking:1.0.0'
    implementation 'software.bevel:file-system-domain:1.0.0' // Required peer dependency
}
```

**Maven (`pom.xml`):**
```xml
<dependencies>
    <dependency>
        <groupId>software.bevel</groupId>
        <artifactId>networking</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>software.bevel</groupId>
        <artifactId>file-system-domain</artifactId>
        <version>1.0.0</version> <!-- Required peer dependency -->
    </dependency>
</dependencies>
```
*(Replace `1.0.0` with the latest version if necessary. Check Maven Central for the latest versions of both libraries.)*

## Usage Instructions & API Highlights

### 1. Using `RestCommunicationInterface`

This interface is used to communicate with a local REST API. You'll typically instantiate it with a `WebClient` implementation.

```kotlin
import software.bevel.networking.RestCommunicationInterface
import software.bevel.networking.web_client.JavaNetReactorMonoWebClient // Or ApacheReactorMonoWebClient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun main() {
    val webClient = JavaNetReactorMonoWebClient()
    val servicePort = "8080" // Port of your local REST service

    // Create the REST communication interface
    val restComms = RestCommunicationInterface(webClient, servicePort)

    // Check if the service is alive (assuming an /api/isAlive endpoint)
    if (restComms.isConnected()) {
        println("Service on port $servicePort is alive.")

        // Example: Send a command object (assuming it's serialized to JSON)
        val commandPayload = mapOf("command" to "myAction", "params" to listOf("arg1", "arg2"))
        val responseString = restComms.send(commandPayload) // Sends as POST to /api/command
        println("Response from service: $responseString")

        // Example: Send a string message without waiting for a detailed response
        val simpleMessage = "{\"event\":\"ping\"}"
        restComms.sendWithoutResponse(simpleMessage)
        println("Sent simple message without expecting detailed response.")

    } else {
        println("Service on port $servicePort is not reachable.")
    }

    restComms.close() // Currently a no-op for RestCommunicationInterface
}
```

### 2. Using `LocalSocket`

For simple, framed communication over a local TCP socket. Messages are framed with `_!START_` and `_!END_` tokens.

```kotlin
import software.bevel.networking.LocalSocket
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun main() {
    val port = 12345 // Port your local server is listening on

    // Connect to the local server
    val localSocket = LocalSocket(port)

    if (localSocket.isConnected()) {
        println("Connected to local server on port $port.")

        // Send a string message
        val response1 = localSocket.send("Hello Server!")
        println("Server response to string: $response1")

        // Send an object (serialized to JSON)
        val dataObject = mapOf("id" to 1, "value" to "test data")
        val response2 = localSocket.send(dataObject)
        println("Server response to object: $response2")

        // Send a message without expecting a response
        localSocket.sendWithoutResponse("Notification: process started.")
        println("Sent notification.")

        // Close the connection
        localSocket.close()
        println("Socket connection closed.")

    } else {
        println("Failed to connect to local server on port $port.")
    }
}
```

### 3. Using `ReactorWebResponse` (via Web Clients)

The `WebClient` implementations return `ReactorMonoResponse<String>`, which wraps a Project Reactor `Mono<String>`.

```kotlin
import software.bevel.networking.web_client.JavaNetReactorMonoWebClient
import software.bevel.networking.ReactorMonoResponse

fun main() {
    val webClient = JavaNetReactorMonoWebClient()
    val targetUrl = "http://localhost:8080/api/data" // Example endpoint

    // Asynchronous POST request
    val postResponseMono: ReactorMonoResponse<String> = webClient.sendPostRequest(
        url = targetUrl,
        body = "{\"key\":\"value\"}"
    )

    // Option 1: Block to get the result (use with caution in reactive flows)
    val responseBodyBlocking: String? = postResponseMono.block()
    println("Blocking POST response: $responseBodyBlocking")

    // Option 2: Subscribe for asynchronous processing
    postResponseMono.subscribe(
        { responseBodyAsync -> println("Async POST response: $responseBodyAsync") }, // onNext
        { error -> System.err.println("Async POST error: ${error.message}") }        // onError
    )

    // Asynchronous GET request
    val getResponseMono: ReactorMonoResponse<String> = webClient.sendGetRequest(targetUrl)
    getResponseMono.subscribe(
        { data -> println("Async GET response: $data") },
        { err -> System.err.println("Async GET error: ${err.message}") }
    )

    // Allow time for async operations to complete in this simple example
    Thread.sleep(2000)
}
```

### 4. Using `RestCommunicationInterfaceCreator`

If you need to defer the creation of `RestCommunicationInterface` instances.

```kotlin
import software.bevel.networking.RestCommunicationInterfaceCreator
import software.bevel.networking.web_client.JavaNetReactorMonoWebClient

fun main() {
    val webClient = JavaNetReactorMonoWebClient()
    val servicePort = "8080"

    val creator = RestCommunicationInterfaceCreator(webClient, servicePort)
    val restCommsInstance = creator.create()

    println("Created RestCommunicationInterface. Is connected: ${restCommsInstance.isConnected()}")
    // Use restCommsInstance as shown in example 1
}
```

## Core Components

*   **`software.bevel.networking.RestCommunicationInterface`**:
    *   Implements `LocalCommunicationInterface` for RESTful interactions, typically with a locally running service.
    *   Uses a provided `WebClient` for HTTP requests (e.g., to `/api/command`, `/api/isAlive`).
*   **`software.bevel.networking.LocalSocket`**:
    *   Implements `LocalCommunicationInterface` for direct TCP socket communication on `localhost`.
    *   Messages are framed with `START_TOKEN` (`_!START_`) and `END_TOKEN` (`_!END_`).
    *   Provides a `messages: Flux<String>` for observing incoming messages (though active pushing to this Flux on receipt is a potential future enhancement).
*   **`software.bevel.networking.web_client` package**:
    *   **`ReactorWebClient<T>`**: Generic interface for web clients returning `ReactorWebResponse`.
    *   **`ReactorMonoWebClient`**: Specialization of `ReactorWebClient` for responses of type `ReactorMonoResponse<String>`.
    *   **`JavaNetReactorMonoWebClient`**: Implementation using Java's `java.net.http.HttpClient`. Supports `HTTPS_PROXY` environment variable.
    *   **`ApacheReactorMonoWebClient`**: Implementation using Apache HttpClient 5. Includes specific parsing logic for OpenAI-like GET responses.
*   **`software.bevel.networking.ReactorWebResponse`**:
    *   Sealed class hierarchy wrapping Project Reactor publishers (`Mono`, `Flux`).
    *   **`ReactorMonoResponse<T>`**: For single-item responses (wraps `Mono<T>`). Implements `Blockable<T>` and `Subscribable<T>`.
    *   **`ReactorFluxResponse<T>`**: For multi-item stream responses (wraps `Flux<T>`). Implements `Blockable<List<T>>` and `Subscribable<T>`.
*   **`software.bevel.networking.RestCommunicationInterfaceCreator`**:
    *   A factory for creating `RestCommunicationInterface` instances, implementing `CommunicationInterfaceCreator`.


## Building from Source

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/Bevel-Software/networking.git
    cd networking
    ```
2.  **Build the project using Gradle:**
    ```bash
    ./gradlew build
    ```
    This will compile the source code, run tests, and build the JAR file (typically found in `build/libs/`).
3.  **Run tests:**
    ```bash
    ./gradlew test
    ```

## Contributing

Contributions are welcome! If you'd like to contribute, please follow these general guidelines:

1.  **Report Issues:** If you encounter a bug or have a feature request, please open an issue on the GitHub repository.
2.  **Fork & Branch:** Fork the repository and create a new branch for your changes.
3.  **Develop:** Make your changes, adhering to Kotlin coding conventions and ensuring your code is well-formatted.
4.  **Test:** Add unit tests for new functionality and ensure all tests pass (`./gradlew test`).
5.  **Pull Request:** Submit a pull request with a clear description of your changes.

Please be respectful in all interactions.

## License

This project is open source and available under the **Mozilla Public License Version 2.0**. See the [LICENSE](LICENSE) file for the full license text.

Dependency license information (generated by the `com.github.jk1.dependency-license-report` plugin) can typically be found in a `NOTICE` file within the build artifacts or project outputs if configured to generate one. Standard dependencies include:
*   Kotlin Standard Library (Apache License 2.0)
*   Project Reactor (Apache License 2.0)
*   Jackson Databind, Module Kotlin, Dataformat YAML (Apache License 2.0)
*   Apache HttpClient5 (Apache License 2.0)
*   SLF4J API (MIT License)
*   JUnit Jupiter API (Eclipse Public License 2.0)
