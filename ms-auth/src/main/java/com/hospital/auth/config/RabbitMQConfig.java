package com.hospital.auth.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Exchange unique partagé par tous les microservices ────────────────────
    public static final String AUTH_EXCHANGE = "medsys.exchange";

    // ── Queue d'audit interne ms-auth ─────────────────────────────────────────
    public static final String AUTH_QUEUE = "auth.events.queue";

    // ── Routing keys ─────────────────────────────────────────────────────────
    public static final String ROUTING_USER_CREATED   = "user.created";
    public static final String ROUTING_USER_LOGGED_IN = "user.logged_in";

    @Bean
    public TopicExchange medsysExchange() {
        return ExchangeBuilder.topicExchange(AUTH_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue authQueue() {
        return QueueBuilder.durable(AUTH_QUEUE).build();
    }

    @Bean
    public Binding authQueueBinding(Queue authQueue, TopicExchange medsysExchange) {
        return BindingBuilder.bind(authQueue).to(medsysExchange).with("user.#");
    }

    // ── JSON serialization (interoperable with .NET) ──────────────────────────
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
