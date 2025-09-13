package org.springframework.ai.mcp.samples.client;

import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpLogging;
import org.springaicommunity.mcp.annotation.McpProgress;
import org.springaicommunity.mcp.annotation.McpSampling;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class McpClientHandlers {

	private static final Logger logger = LoggerFactory.getLogger(McpClientHandlers.class);

	private final ChatClient chatClient;

	public McpClientHandlers(@Lazy ChatClient chatClient) { // Lazy is needed to avoid
															// circular dependency
		this.chatClient = chatClient;
	}

	@McpProgress(clients = "my-weather-server")
	public void progressHandler(ProgressNotification progressNotification) {
		logger.info("MCP PROGRESS: [{}] progress: {} total: {} message: {}", progressNotification.progressToken(),
				progressNotification.progress(), progressNotification.total(), progressNotification.message());
	}

	@McpLogging(clients = "my-weather-server")
	public void loggingHandler(LoggingMessageNotification loggingMessage) {
		logger.info("MCP LOGGING: [{}] {}", loggingMessage.level(), loggingMessage.data());
	}

	@McpSampling(clients = "my-weather-server")
	public CreateMessageResult samplingHandler(CreateMessageRequest llmRequest) {

		logger.info("MCP SAMPLING: {}", llmRequest);

		String llmResponse = chatClient.prompt()
			.system(llmRequest.systemPrompt())
			.user(((TextContent) llmRequest.messages().get(0).content()).text())
			.call()
			.content();

		return CreateMessageResult.builder().content(new TextContent(llmResponse)).build();
	};

}
