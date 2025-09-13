/* 
* Copyright 2025 - 2025 the original author or authors.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
* https://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.springframework.ai.mcp.sample.server;

import java.time.LocalDateTime;
import java.util.List;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ModelPreferences;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.SamplingMessage;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springaicommunity.mcp.annotation.McpProgressToken;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * @author Christian Tzolov
 */
@Service
public class WeatherService {

	private final RestClient restClient = RestClient.create();

	/**
	 * The response format from the Open-Meteo API
	 */
	public record WeatherResponse(Current current) {
		public record Current(LocalDateTime time, int interval, double temperature_2m) {
		}
	}

	@McpTool(description = "Get the temperature (in celsius) for a specific location")
	public String getTemperature(McpSyncServerExchange exchange,
			@McpToolParam(description = "The location latitude") double latitude,
			@McpToolParam(description = "The location longitude") double longitude,
			@McpProgressToken String progressToken) {

		exchange.loggingNotification(LoggingMessageNotification.builder()
				// .level(LoggingLevel.INFO)
				.data("Call getTemperature Tool with latitude: " + latitude + " and longitude: " + longitude)
				.build());

		// 0% progress	
		exchange.progressNotification(
					new ProgressNotification(progressToken, 0.0, 1.0, "Retrieving weather forecast"));			

		WeatherResponse weatherResponse = restClient
				.get()
				.uri("https://api.open-meteo.com/v1/forecast?latitude={latitude}&longitude={longitude}&current=temperature_2m",
						latitude, longitude)
				.retrieve()
				.body(WeatherResponse.class);
		

		String epicPoem = "MCP client doesn't provide sampling capability.";

		if (exchange.getClientCapabilities().sampling() != null) {
			
			// 50% progress
			exchange.progressNotification(
					new ProgressNotification(progressToken, 0.5, 1.0, "Start sampling"));			

			String samplingMessage = """
					For a weather forecast (temperature is in Celsius): %s.
					At location with latitude: %s and longitude: %s.
					Please write an epic poem about this forecast using a Shakespearean style.
					""".formatted(weatherResponse.current().temperature_2m(), latitude, longitude);

			CreateMessageResult samplingResponse = exchange.createMessage(CreateMessageRequest.builder()
					.systemPrompt("You are a poet!")
					.messages(List.of(new SamplingMessage(Role.USER,
							new TextContent(samplingMessage))))
					.modelPreferences(ModelPreferences.builder().addHint("anthropic").build())
					.build());

			epicPoem = ((TextContent) samplingResponse.content()).text();

			System.out.println("Weather Poem: " + epicPoem);
		}	
		
		// 100% progress
		exchange.progressNotification(
				new ProgressNotification(progressToken, 1.0, 1.0, "Task completed"));

		return """
			Weather Poem: %s			
			about the weather: %s°C at location with latitude: %s and longitude: %s		
			""".formatted(epicPoem, weatherResponse.current().temperature_2m(), latitude, longitude);
	}
}