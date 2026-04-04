package dev.langchain4j.carrental.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

    @Bean
    TokenCountEstimator tokenCountEstimator() {
        return new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_5);
    }

    @Bean
    ChatMemoryProvider chatMemoryProvider(TokenCountEstimator tokenCountEstimator) {
        return memoryId -> TokenWindowChatMemory.builder()
                .id(memoryId)
                .maxTokens(5000, tokenCountEstimator)
                .build();
    }


}
