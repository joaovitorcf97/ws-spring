package tech.joaovitor.ws.events;

public record Event<T>(
    EventType type,
    T payload
) {
    
}
