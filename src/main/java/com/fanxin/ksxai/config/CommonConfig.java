package com.fanxin.ksxai.config;

import com.fanxin.ksxai.constants.SystemConstants;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.ollama.management.PullModelStrategy;

import java.time.Duration;
import java.util.List;

@Configuration
public class CommonConfig {

    @Bean
    public ChatMemory chatMemory(){
        return new InMemoryChatMemory();
    }

    @Bean
    public OllamaEmbeddingModel embeddingModel(){
        OllamaOptions options = new OllamaOptions();
        options.setModel("nomic-embed-text:latest");
        ModelManagementOptions modelManagementOptions = new ModelManagementOptions(
                PullModelStrategy.WHEN_MISSING, // 模型不存在时自动拉取
                List.of(),                      // 额外加载的模型列表（可为空）
                Duration.ofSeconds(30),         // 请求超时时间（30秒）
                3                               // 最大重试次数
        );
        return new OllamaEmbeddingModel(
            new OllamaApi("http://localhost:11434"),
            options,
            ObservationRegistry.NOOP,
            modelManagementOptions
            );
    }

    @Bean
    public ChatClient chatClient(OllamaChatModel model,ChatMemory chatMemory){
        return ChatClient
                .builder(model)
                .defaultSystem("你是一个热心、可爱的智能助手，你的名字叫小团团，请以小团团的身份和语气回复")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory)
                        )
                .build();
    }

    @Bean
    public ChatClient gameChatClient(OpenAiChatModel model, ChatMemory chatMemory){
        return ChatClient
                .builder(model)
                .defaultSystem(SystemConstants.GAME_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory)
                )
                .build();
    }
}