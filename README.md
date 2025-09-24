# Spring AI MCP Weather Example

This repository contains the example code for the Spring AI Model Context Protocol (MCP) blog post, demonstrating how to build both MCP Servers and Clients using Spring Boot.

## Overview

The Model Context Protocol (MCP) standardizes how AI applications interact with external tools and resources. This example showcases:

- **MCP Weather Server**: A Spring Boot server that exposes weather forecast capabilities as MCP tools
- **MCP Weather Client**: A Spring Boot AI application that connects to MCP servers and uses an LLM (Anthropic Claude)

## Architecture

```
┌────────────────────────────────────────────────────────────┐
│                    MCP Weather Client                      │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │  ChatClient │──│ MCP Client   │──│ Weather Server   │   │
│  │  (Claude)   │  │  Transport   │  │ (Streamable-HTTP)│   │
│  └─────────────┘  └──────────────┘  └──────────────────┘   │
│                           │                                │
│                   ┌──────────────┐  ┌──────────────────┐   │
│                   │ MCP Client   │──│  Brave Search    │   │
│                   │  Transport   │  │     (STDIO)      │   │
│                   └──────────────┘  └──────────────────┘   │
└────────────────────────────────────────────────────────────┘
```

## Features Demonstrated

### MCP Server Features
- **Tools**: Weather forecast retrieval by coordinates
- **Logging**: Structured log messages sent to clients
- **Progress Tracking**: Real-time progress updates for operations
- **Sampling**: Request AI-generated content from the client's LLM

### MCP Client Features
- **Multi-Server Connection**: Connect to multiple MCP servers simultaneously
- **Transport Support**: Both Streamable-HTTP and STDIO transports
- **Handler Annotations**: Process server notifications and requests
- **LLM Integration**: Seamless integration with Anthropic Claude

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Spring AI 1.1.0-SNAPSHOT
- Node.js and NPM (for optional Brave Search integration)
- Anthropic API key (for the client)
- Brave Search API key (optional, for web search)

## Project Structure

```
spring-ai-mcp-blogpost/
├── mcp-weather-server/     # MCP Server implementation
│   ├── src/main/java/
│   │   └── .../server/
│   │       ├── McpServerApplication.java
│   │       └── WeatherService.java
│   └── pom.xml
├── mcp-weather-client/     # MCP Client implementation
│   ├── src/main/java/
│   │   └── .../client/
│   │       ├── McpClientApplication.java
│   │       └── McpClientHandlers.java
│   └── pom.xml
└── pom.xml                 # Parent POM
```

## Quick Start

### 1. Configure Maven Repository

Both projects use Spring AI snapshots. The required repository is already configured in the `pom.xml` files:
```xml
<repository>
    <id>central-portal-snapshots</id>
    <name>Central Portal Snapshots</name>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

### 2. Build the Projects

```bash
# Build both server and client
mvn clean install -DskipTests
```

### 3. Start the MCP Weather Server

```bash
cd mcp-weather-server
java -jar target/mcp-weather-server-0.0.1-SNAPSHOT.jar
```

The server will start on port 8080 and expose the weather forecast tool via Streamable-HTTP transport. Server logs will be written to `./mcp-weather-server/target/server.log` for debugging purposes.

### 4. Configure and Start the MCP Weather Client

Set your Anthropic API key:
```bash
export ANTHROPIC_API_KEY=your-api-key-here
```

(Optional) Set your Brave Search API key for web search capabilities:
```bash
export BRAVE_API_KEY=your-brave-api-key-here
```

Start the client:
```bash
cd mcp-weather-client
java -jar target/mcp-weather-client-0.0.1-SNAPSHOT.jar
```

The client will:
1. Connect to the weather server
2. Request Amsterdam's weather forecast
3. Receive an AI-generated poem about the weather (via sampling)
4. Optionally search for poetry publishers online (if Brave Search is configured)

## MCP Weather Server Details

### Configuration

The server configuration (`application.properties`):
```properties
# Server identification
spring.ai.mcp.server.name=mcp-weather-server

# Transport protocol (STREAMABLE or STDIO)
spring.ai.mcp.server.protocol=STREAMABLE

# Server port for Streamable-HTTP transport
spring.ai.mcp.server.port=8080

# Request timeout configuration
spring.ai.mcp.server.request-timeout=1h

# Logging configuration
logging.file.name=./mcp-weather-server/target/server.log

# Required for STDIO mode (must be uncommented when using STDIO)
# spring.main.web-application-type=none
# spring.main.banner-mode=off
# logging.pattern.console=
```

Note: When using STDIO transport, you must disable the Spring Boot banner and console logging pattern to prevent interference with the STDIO communication protocol.

### Weather Service Implementation

The `WeatherService` class demonstrates advanced MCP features:

```java
@McpTool(description = "Get the temperature for a specific location")
public String getTemperature(
    McpSyncServerExchange exchange,
    @McpToolParam(description = "The location latitude") double latitude,
    @McpToolParam(description = "The location longitude") double longitude,
    @McpProgressToken String progressToken)
```

Key features:
- **Tool Exposure**: `@McpTool` annotation exposes the method as an MCP tool
- **Parameter Documentation**: `@McpToolParam` provides parameter descriptions
- **Progress Tracking**: `@McpProgressToken` enables progress updates
- **Server Exchange**: Access to client capabilities and bidirectional communication

### Testing the Server

You can test the server independently using:

1. **MCP Inspector**:
   ```bash
   npx @modelcontextprotocol/inspector
   ```
   - Set Transport Type: `Streamable HTTP`
   - URL: `http://localhost:8080/mcp`
   - Click Connect

2. **Claude Desktop** (STDIO mode):
   Add to Claude Desktop configuration:
   ```json
   {
     "mcpServers": {
       "spring-ai-mcp-weather": {
         "command": "java",
         "args": [
           "-Dspring.ai.mcp.server.stdio=true",
           "-Dspring.main.web-application-type=none",
           "-Dlogging.pattern.console=",
           "-jar",
           "/path/to/mcp-weather-server.jar"
         ]
       }
     }
   }
   ```

## MCP Weather Client Details

### Configuration

The client configuration (`application.yml`) connects to multiple MCP servers:

```yaml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
    mcp:
      client:
        streamable-http:
          connections:
            my-weather-server:
              url: http://localhost:8080
        stdio:
          connections:
            brave-search:
              command: npx
              args: ["-y", "@modelcontextprotocol/server-brave-search"]
```

### Client Handlers

The `McpClientHandlers` class processes server notifications:

```java
@McpProgress(clients = "my-weather-server")
public void progressHandler(ProgressNotification notification) { }

@McpLogging(clients = "my-weather-server")
public void loggingHandler(LoggingMessageNotification message) { }

@McpSampling(clients = "my-weather-server")
public CreateMessageResult samplingHandler(CreateMessageRequest request) { }
```

### ChatClient Integration

The client uses Spring AI's `ChatClient` with MCP tools:

```java
chatClient.prompt(userPrompt)
    .toolContext(Map.of("progressToken", "token-" + new Random().nextInt()))
    .toolCallbacks(mcpToolProvider)
    .call()
    .content()
```

## Advanced Features

### Bidirectional AI Communication

The most powerful feature demonstrated is **sampling**, where:
1. The weather server requests the client's LLM to generate a poem
2. The client's handler processes the request using its ChatClient
3. The generated poem is returned to the server
4. The server incorporates it into the final response

This creates sophisticated AI-to-AI interactions beyond simple tool invocation.

### Progress Tracking

The server reports operation progress:
- 0% - Starting weather retrieval
- 50% - Beginning AI sampling
- 100% - Task completed

### Structured Logging

The server sends structured log messages that the client can use for:
- Debugging server operations
- Audit trails
- Monitoring dashboards

## Extending the Example

### Adding New Tools

Add new methods to `WeatherService` with `@McpTool`:

```java
@McpTool(description = "Get weather forecast for multiple days")
public ForecastResponse getForecast(
    @McpToolParam(description = "Location latitude") double latitude,
    @McpToolParam(description = "Location longitude") double longitude,
    @McpToolParam(description = "Number of days") int days) {
    // Implementation
}
```

### Connecting Additional MCP Servers

Add new server connections in `application.yml`:

```yaml
spring:
  ai:
    mcp:
      client:
        stdio:
          connections:
            your-server:
              command: your-command
              args: [your-args]
```

### Custom Handler Logic

Create specialized handlers for different servers:

```java
@McpProgress(clients = {"weather-server", "database-server"})
public void multiServerProgressHandler(ProgressNotification notification) {
    // Handle progress from multiple servers
}
```

## Resources

- [Blog Post: Connect Your AI to Everything: Spring AI's MCP Boot Starters](https://spring.io/blog/2025/09/16/spring-ai-mcp-intro-blog)
- [Spring AI MCP Documentation](https://docs.spring.io/spring-ai/reference/1.1-SNAPSHOT/api/mcp/mcp-overview.html)
- [Model Context Protocol Specification](https://modelcontextprotocol.io/specification/)
- [MCP Java SDK](https://modelcontextprotocol.io/sdk/java/mcp-overview)
- [Spring AI Reference Documentation](https://docs.spring.io/spring-ai/reference/)

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For questions and support:
- Create an issue in this repository
- Check the [Spring AI documentation](https://docs.spring.io/spring-ai/reference/)
- Visit the [Spring community forums](https://community.spring.io/)
