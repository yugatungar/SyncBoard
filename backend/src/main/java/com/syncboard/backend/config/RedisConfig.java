package com.syncboard.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.syncboard.backend.service.RedisEventSubscriber;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${syncboard.redis.workspace-channel:syncboard:workspace-events}")
    private String workspaceChannelName;

    @Value("${syncboard.redis.presence-channel:syncboard:presence-events}")
    private String presenceChannelName;

    @Bean
    public ChannelTopic workspaceTopic() {
        return new ChannelTopic(workspaceChannelName);
    }

    @Bean
    public ChannelTopic presenceTopic() {
        return new ChannelTopic(presenceChannelName);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(RedisEventSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter messageListenerAdapter,
            ChannelTopic workspaceTopic,
            ChannelTopic presenceTopic) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer() {
            @Override
            public void start() {
                try {
                    super.start();
                } catch (Exception e) {
                    LoggerFactory.getLogger(RedisMessageListenerContainer.class)
                            .warn("Could not start Redis message listener container (Redis may be offline): {}", e.getMessage());
                }
            }
        };
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListenerAdapter, workspaceTopic);
        container.addMessageListener(messageListenerAdapter, presenceTopic);
        container.setErrorHandler(t -> LoggerFactory.getLogger(RedisMessageListenerContainer.class)
                .warn("Redis listener error: {}", t.getMessage()));
        return container;
    }
}
