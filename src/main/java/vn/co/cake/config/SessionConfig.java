package vn.co.cake.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import vn.co.cake.queue.RedisMessageSubscriber;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1296000)
public class SessionConfig {

    private final RedisMessageSubscriber redisMessageSubscriber;

    public SessionConfig(RedisMessageSubscriber redisMessageSubscriber) {
        this.redisMessageSubscriber = redisMessageSubscriber;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        if (redisConnectionFactory == null) {
            throw new IllegalStateException("RedisConnectionFactory is null! Check Redis configuration.");
        }
        template.setConnectionFactory(redisConnectionFactory);
        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
        return template;
    }

    @Bean
    public static ConfigureRedisAction configureRedisAction() {
        return ConfigureRedisAction.NO_OP;
    }

    @Bean
    public MessageListenerAdapter messageListener() {
        return new MessageListenerAdapter(redisMessageSubscriber);
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListener(), topic());
        return container;
    }

    @Bean
    public ChannelTopic topic() {
        return new ChannelTopic("pubsub:queue");
    }
}
