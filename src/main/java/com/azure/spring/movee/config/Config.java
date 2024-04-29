package com.azure.spring.movee.config;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.spring.movee.vectorstore.DocumentIndexPlanner;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.RedisVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.socket.client.standard.WebSocketContainerFactoryBean;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
public class Config {

    @Value("${spring.ai.azure.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.azure.openai.endpoint}")
    private String endpoint;

    @Value("${spring.ai.azure.openai.chat.options.model}")
    private String deploymentOrModelId;

    @Value("${spring.data.redis.host}")
    private String redisUrl;

    @Bean
    public SimpleVectorStore simpleVectorStore(EmbeddingClient embeddingClient)  {
        return new SimpleVectorStore(embeddingClient);
    }

    @Bean
    public DocumentIndexPlanner planner(SimpleVectorStore simpleVectorStore) {
        return new DocumentIndexPlanner(simpleVectorStore);
    }

    @Bean
    public RedisTemplate redisTemplate(LettuceConnectionFactory redisConnectionFactory){
        RedisTemplate<Object,Object> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    OpenAIClient openAIClient() {
        return new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .buildClient();
    }

    @Bean
    public ServletServerContainerFactoryBean createServletServerContainerFactoryBean() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(32768);
        container.setMaxBinaryMessageBufferSize(32768);
        return container;
    }

    @Bean
    public VectorStore vectorStore(EmbeddingClient embeddingClient) {
        RedisVectorStore.RedisVectorStoreConfig config = RedisVectorStore.RedisVectorStoreConfig.builder()
                .withURI("redis://localhost:6379")
                // Define the metadata fields to be used
                // in the similarity search filters.
                .withMetadataFields(
                        RedisVectorStore.MetadataField.tag("movieName"))
                .build();

        return new RedisVectorStore(config, embeddingClient);
    }

}
