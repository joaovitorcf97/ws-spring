package tech.joaovitor.ws.dtos;

import tech.joaovitor.ws.data.User;

public record ChatMessage(
    User from,
    User to,
    String text
) {
    
}
