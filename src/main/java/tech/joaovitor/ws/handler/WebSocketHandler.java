package tech.joaovitor.ws.handler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import tech.joaovitor.ws.data.User;
import tech.joaovitor.ws.dtos.ChatMessage;
import tech.joaovitor.ws.events.Event;
import tech.joaovitor.ws.events.EventType;
import tech.joaovitor.ws.pubsub.Publisher;
import tech.joaovitor.ws.services.TicketService;
import tech.joaovitor.ws.services.UserService;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final static Logger LOGGER = Logger.getLogger(WebSocketHandler.class.getName());
    private final TicketService ticketService;
    private final Publisher publisher;
    private final UserService userService;
    private final Map<String, WebSocketSession> sessions;
    private final Map<String, String> userIds;

    public WebSocketHandler(
            TicketService ticketService,
            Publisher publisher,
            UserService userService
    ) {
        this.ticketService = ticketService;
        this.publisher = publisher;
        this.userService = userService;
        sessions = new ConcurrentHashMap<>();
        userIds = new ConcurrentHashMap<>();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        LOGGER.info("[afterConnectionEstablished] session id " + session.getId());

        Optional<String> ticket = ticketOf(session);

        if (ticket.isEmpty() || ticket.get().isBlank()) {
                LOGGER.warning("session " + session.getId() + " without ticket");
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
        }

        Optional<String> userId = ticketService.getUserByTicket(ticket.get());

        if (userId.isEmpty()) {
            LOGGER.warning("session" + session.getId() + " with invalid ticker");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        sessions.put(userId.get(), session);
        userIds.put(session.getId(), userId.get());
        LOGGER.info("session" + session.getId() + " was bind to user " + userId.get());
        sendChatUsers(session);
    } 

    private Optional<String> ticketOf(WebSocketSession session) {
        return Optional
                .ofNullable(session.getUri())
                .map(UriComponentsBuilder::fromUri)
                .map(UriComponentsBuilder::build)
                .map(UriComponents::getQueryParams)
                .map(it -> it.get("ticket"))
                .flatMap(it -> it.stream().findFirst())
                .map(String::trim);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        LOGGER.info("[handleTextMessage] text " + message.getPayload());
        if(message.getPayload().equals("ping")){ session.sendMessage(new TextMessage("pong")); return; }
        MessagePayload payload = new ObjectMapper().readValue(message.getPayload(), MessagePayload.class);
        System.out.println(payload);
        String userIdFrom = userIds.get(session.getId());
        System.out.println(userIdFrom);
        publisher.publishChatMessage(userIdFrom, payload.to(), payload.text());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status)  {
        LOGGER.info("[afterConnectionClosed] session id " + session.getId());
        String userId = userIds.get(session.getId());
        sessions.remove(userId);
        userIds.remove(session.getId());
    }
    
    private void sendChatUsers(WebSocketSession session) {
        List<User> chatUsers = userService.findChatUsers();
        Event<List<User>> event = new Event<>(EventType.CHAT_USERS_WERE_UPDATED, chatUsers); 
        sendEvent(session, event);
        System.out.println(chatUsers);
    }

    public void notify(ChatMessage chatMessage) {
        Event<ChatMessage> event = new Event<>(EventType.CHAT_MESSAGE_WAS_CREATED, chatMessage);
        List<String> userIds = List.of(chatMessage.from().id(), chatMessage.to().id());
        userIds.stream()
                .distinct()
                .map(sessions::get)
                .filter(Objects::nonNull)
                .forEach(session -> sendEvent(session, event));
        LOGGER.info("chat message was notified");
    }

    private void sendEvent(WebSocketSession session, Event<?> event) {
        try {
            String eventSerialized = new ObjectMapper().writeValueAsString(event);
            session.sendMessage(new TextMessage(eventSerialized));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}

