package tech.joaovitor.ws.pubsub;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import tech.joaovitor.ws.config.RedisConfig;
import tech.joaovitor.ws.dtos.ChatMessage;
import tech.joaovitor.ws.handler.WebSocketHandler;

@Component
public class Subscriber {
    private final static Logger LOGGER = Logger.getLogger(Subscriber.class.getName());

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @PostConstruct
    private void init() {
        this.redisTemplate
            .listenTo(ChannelTopic.of(RedisConfig.CHAT_MESSAGES_CHANNEL))
            .map(ReactiveSubscription.Message::getMessage)
            .subscribe(this::onChatMessage);
    }

    private void onChatMessage(final String chatMessageSerialized) {
        LOGGER.info("chat message was received");

        try {
            ChatMessage chatMessage = new ObjectMapper().readValue(chatMessageSerialized, ChatMessage.class);
            webSocketHandler.notify(chatMessage);
        } catch (JsonProcessingException e) {
           e.printStackTrace();
           throw new RuntimeException(e);
        }
    }
}
