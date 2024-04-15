package tech.joaovitor.ws.pubsub;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tech.joaovitor.ws.config.RedisConfig;
import tech.joaovitor.ws.data.User;
import tech.joaovitor.ws.data.UserRepository;
import tech.joaovitor.ws.dtos.ChatMessage;

@Component
public class Publisher {
    private final static Logger LOGGER = Logger.getLogger(Publisher.class.getName());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    public void publishChatMessage(String userIdFrom, String userIdTo, String text) throws JsonProcessingException {
        User from = userRepository.findById(userIdFrom).orElseThrow();
        User to = userRepository.findById(userIdTo).orElseThrow();
        ChatMessage chatMessage = new ChatMessage(from, to, text);
        String chatMessageSerialized = new ObjectMapper().writeValueAsString(chatMessage);
        redisTemplate
                .convertAndSend(RedisConfig.CHAT_MESSAGES_CHANNEL, chatMessageSerialized)
                .subscribe();
        LOGGER.info("chat message was published");
    }
}
