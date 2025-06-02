# Networking (Kotlin Library for Bevel)

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/software.bevel/networking.svg?label=Maven%20Central&version=1.1.0)](https://search.maven.org/artifact/software.bevel/networking/1.1.0/jar)

Welcome to the `networking` library, a powerful Kotlin/JVM component designed for the Bevel suite of developer tools! This library equips your applications with versatile client-side networking capabilities. It offers elegant abstractions for reactive HTTP/REST communication and provides a straightforward mechanism for local socket-based inter-process communication (IPC).

A key design principle of `networking` is its seamless integration with other Bevel components, particularly by implementing communication interfaces defined in the `file-system-domain` library. This ensures a decoupled and modular architecture within the Bevel ecosystem.

## Why Use This Library? Key Features

*   🚀 **Reactive HTTP Clients**: Build non-blocking, high-performance HTTP clients with `ReactorWebClient` and `ReactorMonoWebClient` interfaces.
*   🔩 **Multiple HTTP Backends**:
    *   **`JavaNetReactorMonoWebClient`**: Leverages Java's modern `java.net.http.HttpClient` (Java 11+), automatically supporting system proxy settings (e.g., `HTTPS_PROXY`).
    *   **`ApacheReactorMonoWebClient`**: Utilizes the robust Apache HttpClient 5 for versatile HTTP communication, including specific parsing for OpenAI-like GET responses.
*   💬 **Simplified REST Communication**: The `RestCommunicationInterface` makes interacting with local RESTful APIs a breeze, abstracting away the complexities of HTTP calls.
*   🔗 **Local Socket IPC**: `LocalSocket` offers a simple, framed TCP/IP socket solution for direct messaging between processes on the same machine.
*   ✨ **Project Reactor Integration**: Fully embraces Project Reactor (`Mono`, `Flux`) for asynchronous and reactive programming paradigms, exposed via `ReactorMonoResponse` and `ReactorFluxResponse`.
*   🔄 **Effortless JSON Handling**: Uses Jackson for smooth serialization and deserialization of objects, perfect for message payloads.
*   🧩 **Modular Design**: Integrates cleanly with interfaces from `software.bevel:file-system-domain`, promoting loose coupling.
*   🌍 **Maven Central Availability**: Easily incorporate into any JVM project using Gradle or Maven.

## The `file-system-domain` Connection

The `networking` library is not just a standalone utility; it's a crucial implementer of contracts defined in the [Bevel `file-system-domain`](https://github.com/Bevel-Software/file-system-domain) library. This relationship is fundamental to its design:

*   `networking` provides concrete implementations for interfaces like `software.bevel.file_system_domain.web.LocalCommunicationInterface`, `software.bevel.file_system_domain.web.WebClient`, and `software.bevel.file_system_domain.web.CommunicationInterfaceCreator`.
*   This allows other Bevel tools (and your applications) to depend on the stable interfaces from `file-system-domain` while `networking` serves as a powerful, interchangeable runtime implementation for actual communication.

You'll need to include `file-system-domain` as a peer dependency when using `networking`.

## Core Concepts & Components

Let's dive into the heart of the `networking` library:

### 1. Communication Interfaces

*   **`LocalCommunicationInterface` (from `file-system-domain`)**: This is the contract for local communication. `networking` provides two main implementations:
    *   **`RestCommunicationInterface`**:
        *   Ideal for interacting with local REST APIs (e.g., a sidecar service).
        *   Uses a `WebClient` (see below) to send HTTP requests to standard endpoints like `/api/command` and `/api/isAlive`.
        *   Simplifies sending data and checking service availability.
    *   **`LocalSocket`**:
        *   Provides direct TCP/IP socket communication on `localhost`.
        *   Messages are framed using `_!START_` and `_!END_` tokens to ensure message integrity.
        *   Suitable for simple, low-overhead IPC.
        *   The `messages: Flux<String>` property is available for observing incoming messages. Note: For typical request-response, use `send()`. This Flux is for potential future enhancements or passive listening.

### 2. WebClient Abstractions

These interfaces and classes form the foundation for HTTP communication:

*   **`ReactorWebClient<T>` (Interface)**: A generic interface for web clients that return a `ReactorWebResponse`.
*   **`ReactorMonoWebClient` (Interface)**: A specialization of `ReactorWebClient` for responses that are `ReactorMonoResponse<String>`, meaning they wrap a `Mono<String>`.
*   **Implementations**:
    *   **`JavaNetReactorMonoWebClient`**:
        *   Uses Java's built-in `java.net.http.HttpClient` (available since Java 11).
        *   **Feature**: Automatically respects system-wide proxy configurations (e.g., `HTTPS_PROXY` environment variable).
        *   A good default choice for modern Java environments.
    *   **`ApacheReactorMonoWebClient`**:
        *   Built on the mature Apache HttpClient 5 library.
        *   **Feature**: Includes specific parsing logic for GET responses structured similarly to OpenAI API outputs (extracting `content` from `response.choices[0].message.content`).
        *   Offers robust and configurable HTTP communication.

### 3. Reactive Web Responses (`ReactorWebResponse`)

How you handle responses matters in a reactive world:

*   **`ReactorWebResponse<T, PUBLISHER_TYPE>` (Sealed Class)**: The base for responses backed by Project Reactor's `Publisher`.
*   **`ReactorMonoResponse<T>`**:
    *   Represents a response expected to yield a single item (or none), wrapped in a `Mono<T>`.
    *   Implements `Blockable<T>`: `block()` to synchronously get the result.
    *   Implements `Subscribable<T>`: `subscribe(...)` for asynchronous processing.
*   **`ReactorFluxResponse<T>`**:
    *   For responses that stream multiple items, wrapped in a `Flux<T>`.
    *   Implements `Blockable<List<T>>`: `block()` to synchronously get all items as a list.
    *   Implements `Subscribable<T>`: `subscribe(...)` to process each item as it arrives.

### 4. Interface Creation

*   **`RestCommunicationInterfaceCreator`**:
    *   A factory implementing `CommunicationInterfaceCreator` (from `file-system-domain`).
    *   Useful for scenarios where you need to defer the creation of `RestCommunicationInterface` instances or manage them through a standardized creator pattern.

## Installation

Get up and running with the `networking` library in your JVM project.

### Prerequisites

*   Java Development Kit (JDK) 17 or higher.
*   A build tool like Gradle or Maven.

### Adding Dependencies

The library is available on Maven Central. Remember to also include `file-system-domain` as it provides the core interfaces.

**Gradle (Kotlin DSL - `build.gradle.kts`):**
```kotlin
dependencies {
    implementation("software.bevel:networking:1.1.0")
    implementation("software.bevel:file-system-domain:1.1.0") // Required peer dependency
    // Transitive dependencies like Jackson, Reactor, SLF4J, Apache HttpClient are included.
}
```

**Gradle (Groovy DSL - `build.gradle`):**
```groovy
dependencies {
    implementation 'software.bevel:networking:1.1.0'
    implementation 'software.bevel:file-system-domain:1.1.0' // Required peer dependency
}
```

**Maven (`pom.xml`):**
```xml
<dependencies>
    <dependency>
        <groupId>software.bevel</groupId>
        <artifactId>networking</artifactId>
        <version>1.1.0</version>
    </dependency>
    <dependency>
        <groupId>software.bevel</groupId>
        <artifactId>file-system-domain</artifactId>
        <version>1.1.0</version> <!-- Required peer dependency -->
    </dependency>
</dependencies>
```
*(Always check Maven Central for the latest versions of `networking` and `file-system-domain` and update accordingly.)*

## Quick Start & Usage Examples

Let's see the `networking` library in action!

### Example 1: Communicating with a Local REST API

Use `RestCommunicationInterface` to talk to a service running on `localhost`.

```kotlin
import software.bevel.networking.RestCommunicationInterface
import software.bevel.networking.web_client.JavaNetReactorMonoWebClient // Or ApacheReactorMonoWebClient

fun main() {
    // Choose your WebClient implementation
    val webClient = JavaNetReactorMonoWebClient()
    val servicePort = "8080" // The port your local REST service listens on

    val restApi = RestCommunicationInterface(webClient, servicePort)

    if (restApi.isConnected()) {
        println("Successfully connected to service on port $servicePort.")

        // Example: Send a structured command (serialized to JSON)
        val command = mapOf("action" to "processFile", "filePath" to "/path/to/file.txt")
        try {
            val responseJson = restApi.send(command) // POSTs to /api/command
            println("Service response: $responseJson")
            // You'd typically deserialize responseJson here
        } catch (e: Exception) {
            System.err.println("Error sending command: ${e.message}")
        }

        // Example: Send a simple string message and don't wait for detailed content
        restApi.sendWithoutResponse("{\"event\":\"heartbeat\"}")
        println("Sent heartbeat event.")

    } else {
        println("Could not connect to service on port $servicePort. Is it running?")
    }

    restApi.close() // For RestCommunicationInterface, this is currently a no-op but good practice.
}
```

### Example 2: Simple Inter-Process Communication with `LocalSocket`

For direct, framed messaging between local processes.

```kotlin
import software.bevel.networking.LocalSocket
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper // For object serialization

fun main() {
    val targetPort = 12345 // Port your local server process is listening on

    // Attempt to connect
    val socketComm = LocalSocket(targetPort)

    if (socketComm.isConnected()) {
        println("Connected to local server via socket on port $targetPort.")

        // Send a plain string message
        val response1 = socketComm.send("PING")
        println("Server replied to PING: '$response1'")

        // Send an object (will be JSON serialized)
        val dataObject = mapOf("id" to 101, "payload" to "important data")
        val response2 = socketComm.send(dataObject)
        println("Server replied to object: '$response2'")
        // You might deserialize response2 if it's JSON

        // Send a fire-and-forget message
        socketComm.sendWithoutResponse("System update: Process A started.")
        println("Sent a notification message.")

        // Important: Close the connection when done
        socketComm.close()
        println("Socket connection closed.")

    } else {
        println("Failed to establish socket connection on port $targetPort.")
    }
}
```

### Example 3: Making Asynchronous HTTP Requests

Directly use a `WebClient` like `JavaNetReactorMonoWebClient` for fine-grained control.

```kotlin
import software.bevel.networking.web_client.JavaNetReactorMonoWebClient
import software.bevel.networking.ReactorMonoResponse // To work with the response type

fun main() {
    val webClient = JavaNetReactorMonoWebClient()
    val apiUrl = "http://localhost:8080/api/resource" // Your target endpoint

    // Asynchronous POST request
    val postPayload = "{\"name\":\"gizmo\",\"value\":42}"
    val postResponse: ReactorMonoResponse<String> = webClient.sendPostRequest(
        url = apiUrl,
        body = postPayload,
        headers = listOf("X-Custom-Header" to "MyValue")
    )

    println("POST request sent. Subscribing for response...")
    postResponse.subscribe(
        { responseBody -> println("Async POST Response Body: $responseBody") }, // onNext
        { error -> System.err.println("Async POST Error: ${error.message}") },    // onError
        { println("Async POST Completed.") }                                    // onComplete (Mono only has onNext or onError for value)
    )

    // Asynchronous GET request
    val getResponse: ReactorMonoResponse<String> = webClient.sendGetRequest(
        url = "$apiUrl/123", // Example with a path parameter
        parameters = listOf("filter" to "active")
    )

    println("GET request sent. Subscribing for response...")
    getResponse.subscribe(
        { data -> println("Async GET Response Data: $data") },
        { err -> System.err.println("Async GET Error: ${err.message}") }
    )

    // In a real app, you wouldn't use Thread.sleep.
    // This is just to keep the main thread alive for demo purposes.
    println("Waiting for async operations to complete...")
    Thread.sleep(5000)
    println("Demo finished.")
}
```

### Example 4: Deferred Creation with `RestCommunicationInterfaceCreator`

Useful for dependency injection or when setup needs to be delayed.

```kotlin
import software.bevel.networking.RestCommunicationInterfaceCreator
import software.bevel.networking.web_client.ApacheReactorMonoWebClient

fun main() {
    val webClient = ApacheReactorMonoWebClient()
    val servicePort = "9090"

    // Create the factory
    val interfaceCreator = RestCommunicationInterfaceCreator(webClient, servicePort)

    // Create the instance when needed
    val restComms = interfaceCreator.create()

    println("RestCommunicationInterface created. Is connected: ${restComms.isConnected()}")
    // Now use 'restComms' as shown in Example 1
    // ...
    restComms.close()
}
```

## Building from Source

Want to build the library yourself or contribute? Here's how:

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/Bevel-Software/networking.git
    cd networking
    ```

2.  **Build with Gradle:**
    The Gradle wrapper (`gradlew`) is included.
    ```bash
    ./gradlew build
    ```
    This command compiles the code, runs tests, and creates the JAR file (usually in `build/libs/`).

3.  **Run Tests:**
    ```bash
    ./gradlew test
    ```

## Contributing

We welcome contributions! If you're interested in helping improve the `networking` library:

1.  **Found a Bug or Have an Idea?** Open an issue on the GitHub repository. Clear descriptions are greatly appreciated!
2.  **Ready to Code?**
    *   Fork the repository.
    *   Create a new branch for your feature or bug fix (e.g., `feature/new-client` or `fix/socket-timeout`).
    *   Make your changes. Please adhere to Kotlin coding conventions and ensure your code is well-formatted.
    *   **Write Tests!** New functionality should be accompanied by unit tests. Ensure all tests pass (`./gradlew test`).
    *   Commit your changes with clear, concise messages.
    *   Push your branch to your fork.
    *   Open a Pull Request against the main repository, detailing your changes.

We value respectful and constructive collaboration.

## License

This project is open source and distributed under the **Mozilla Public License Version 2.0**.
You can find the full license text in the [LICENSE](LICENSE) file.

Key dependencies and their licenses include:
*   Kotlin Standard Library: Apache License 2.0
*   Project Reactor: Apache License 2.0
*   Jackson (Databind, Module Kotlin, Dataformat YAML): Apache License 2.0
*   Apache HttpClient 5: Apache License 2.0
*   SLF4J API: MIT License
*   JUnit Jupiter API: Eclipse Public License 2.0

For a comprehensive list of dependencies and their licenses, you can refer to the reports generated by the `com.github.jk1.dependency-license-report` Gradle plugin (often found in build outputs).
