package com.medsys.auth.messaging;

import com.medsys.auth.config.RabbitMQConfig;
import com.medsys.auth.entity.UserAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishUserCreated(UserAccount user) {
        try {
            Map<String, Object> event = buildEvent(user);
            event.put("eventType", "USER_CREATED");
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "user.created", event);
            log.info("[RABBITMQ] Published user.created for userId={}", user.getId());
        } catch (Exception e) {
            log.warn("[RABBITMQ] Failed to publish user.created: {}", e.getMessage());
        }
    }

    public void publishUserLoggedIn(UserAccount user) {
        try {
            Map<String, Object> event = buildEvent(user);
            event.put("eventType", "USER_LOGGED_IN");
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "user.logged_in", event);
        } catch (Exception e) {
            log.warn("[RABBITMQ] Failed to publish user.logged_in: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildEvent(UserAccount user) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", user.getId());
        event.put("email", user.getEmail());
        event.put("nom", user.getNom());
        event.put("prenom", user.getPrenom());
        event.put("role", user.getRole().name());
        event.put("timestamp", LocalDateTime.now().toString());
        return event;
    }
}
