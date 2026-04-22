package com.medsys.auth.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "medsys.exchange";
    public static final String QUEUE_USER_CREATED = "user.created";
    public static final String QUEUE_USER_LOGGED_IN = "user.logged_in";

    @Bean
    public TopicExchange medsysExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder.durable(QUEUE_USER_CREATED).build();
    }

    @Bean
    public Queue userLoggedInQueue() {
        return QueueBuilder.durable(QUEUE_USER_LOGGED_IN).build();
    }

    @Bean
    public Binding userCreatedBinding() {
        return BindingBuilder.bind(userCreatedQueue()).to(medsysExchange()).with("user.created");
    }

    @Bean
    public Binding userLoggedInBinding() {
        return BindingBuilder.bind(userLoggedInQueue()).to(medsysExchange()).with("user.logged_in");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
