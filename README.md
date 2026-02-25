# Phrase Language AI - Bring Your Own Engine Adapter (Java)

This repository provides a sample implementation of a custom machine translation (MT) adapter for the [Phrase Language AI "Bring Your Own Engine" (BYOE)](https://support.phrase.com/hc/en-us/articles/5709660879516-Phrase-Language-AI-TMS#byo-engine-0-6) feature. It is built using Java 21 and the Spring Boot framework.

The purpose of this demo adapter is to serve as a reference and a starting point for developers looking to integrate their own MT engines with Phrase TMS. The current implementation simulates a translation process by simply appending the target language code to the source text..

NOTE: Please make sure to adhere to the [OpenAPI schema](https://developers.phrase.com/public/assets/openapi/phrase-byo-mt.yaml) when implementing your own adapter.

**Technical Recommendations for Stability and Performance**
- DNS Infrastructure & Routing: Please ensure your DNS resolution is stable and entries are correctly configured. Misconfigurations or propagation delays can lead to intermittent resolution failures, high latency, or request timeouts.
- Application & Proxy Server Tuning (e.g., Nginx): Verify that your proxy/gateway servers are optimized for high-concurrency traffic. Specifically, check keep-alive settings and worker process limits to prevent TCP connection resets or intermittent "cut-offs" under load.
- Load & Concurrency Testing: We strongly recommend performing synthetic load tests from external endpoints. Your adapter should be validated against a sustained concurrency of 100–200 requests per second (RPS) to ensure performance remains linear and stable.

## Features

*   **Complete API Implementation**: Implements all required endpoints of the Phrase BYOE REST API specification.
*   **Synchronous & Asynchronous Translation**: Supports both `/translate` for immediate translations and `/translateAsync` for long-running jobs.
*   **Asynchronous Job Management**: Uses an in-memory Caffeine cache to track the status and results of asynchronous translation jobs.
*   **Service Health & Capabilities**: Includes `/status` to report engine readiness and `/languages` to declare supported language pairs.
*   **Containerized**: Comes with a `Dockerfile` for easy containerization and deployment.
*   **Cloud-Ready**: Includes a `render.yaml` file for seamless deployment to the Render platform.

## API Endpoints

The adapter implements the following endpoints as required by Phrase:

*   `POST /status`: Checks the operational status of the adapter. Returns `{"status": "ok"}` when ready.
*   `POST /languages`: Returns a list of supported source and target language pairs.
*   `POST /translate`: Accepts a source text and language pair, and returns the translation synchronously.
*   `POST /translateAsync`: Initiates an asynchronous translation job and returns a job ID.
*   `GET /translateAsyncStatus/{jobId}`: Reports the status of an asynchronous job (`running`, `done`, `failed`).
*   `GET /translateAsyncResult/{jobId}`: Retrieves the translation result for a completed asynchronous job.

## Getting Started

### Prerequisites

*   Java 21 JDK
*   Gradle

### Running Locally

To run the application locally, use the Gradle wrapper:

```bash
./gradlew bootRun
```

The server will start on `http://localhost:8080`.

### Building the Application

To build a self-contained JAR file, run:

```bash
./gradlew clean bootJar
```

The JAR file will be located in `build/libs/`.

## Deployment

### Docker

A `Dockerfile` is provided for containerizing the application. It uses a multi-stage build to create a lean final image.

1.  **Build the Docker image:**
    ```bash
    docker build -t pzemanek73/custom.adapter .
    ```

2.  **Run the Docker container:**
    ```bash
    docker run -p 8080:8080 pzemanek73/custom.adapter
    ```

### Render

This repository is configured for easy testing deployment on [Render](https://render.com/). The `render.yaml` file defines a "Blueprint" that instructs Render how to build and run the service from the `Dockerfile`.

You can deploy this service by creating a new "Blueprint" in your Render dashboard and connecting this GitHub repository.

## Customization

To integrate your actual machine translation engine, you will need to modify the following parts of the code:

1.  **`TranslationService.java`**: This is the core component to update.
    *   In the `translate()` and `translateAsync()` methods, replace the simulation logic (which uses `sleep()` and string formatting) with API calls to your own MT engine.
    *   Ensure that any errors from your engine are caught and that `translateAsync()` returns a meaningful `failureDetail` in the `AsyncJobResult`.
    *   Ensure that you are able to process up to 500 segments for both the synchronous and asynchronous translation methods.

2.  **`Controller.java`**:
    *   Update the `languages()` method to return the actual language pairs supported by your engine. The current implementation contains a static list of pairs: `en -> de`, `en -> cs`, and `en -> zh_tw`.
    *   If your engine requires authentication, you can implement the necessary logic in the `processHeaders()` method, which intercepts all incoming requests.

3.  **`application.properties`**:
    *   Add any required configuration properties for your MT engine, such as API keys, endpoints, or timeouts. You can access these properties in your service using Spring's `@Value` annotation.

## Testing

The project includes unit and integration tests for the controller and service layers. To run the tests, execute the following command:

```bash
./gradlew test