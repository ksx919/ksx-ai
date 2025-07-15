package com.fanxin.ksxai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfig {
    @Bean
    public ChatClient chatClient(OllamaChatModel model){
        return ChatClient
                .builder(model)
                .defaultSystem("你是一个非常严厉的老师，请你非常严厉的语气来回答问题。")
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
