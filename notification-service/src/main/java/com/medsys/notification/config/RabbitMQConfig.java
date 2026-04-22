package com.medsys.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "medsys.exchange";

    @Bean public TopicExchange medsysExchange() { return new TopicExchange(EXCHANGE, true, false); }

    @Bean public Queue notifyAppointmentConfirmedQueue() {
        return QueueBuilder.durable("notify.appointment.confirmed").build();
    }
    @Bean public Queue notifyAppointmentCancelledQueue() {
        return QueueBuilder.durable("notify.appointment.cancelled").build();
    }
    @Bean public Queue notifyUserCreatedQueue() {
        return QueueBuilder.durable("notify.user.created").build();
    }

    @Bean public Binding notifyConfirmedBinding() {
        return BindingBuilder.bind(notifyAppointmentConfirmedQueue()).to(medsysExchange()).with("appointment.confirmed");
    }
    @Bean public Binding notifyCancelledBinding() {
        return BindingBuilder.bind(notifyAppointmentCancelledQueue()).to(medsysExchange()).with("appointment.cancelled");
    }
    @Bean public Binding notifyUserBinding() {
        return BindingBuilder.bind(notifyUserCreatedQueue()).to(medsysExchange()).with("user.created");
    }

    @Bean public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(messageConverter());
        return t;
    }
}
