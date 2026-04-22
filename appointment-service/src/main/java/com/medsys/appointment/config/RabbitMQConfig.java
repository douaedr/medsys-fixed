package com.medsys.appointment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "medsys.exchange";
    public static final String QUEUE_APPOINTMENT_CREATED   = "appointment.created";
    public static final String QUEUE_APPOINTMENT_CONFIRMED = "appointment.confirmed";
    public static final String QUEUE_APPOINTMENT_CANCELLED = "appointment.cancelled";
    public static final String QUEUE_APPOINTMENT_NOSHOW    = "appointment.noshow";

    @Bean public TopicExchange medsysExchange() { return new TopicExchange(EXCHANGE, true, false); }

    @Bean public Queue appointmentCreatedQueue()   { return QueueBuilder.durable(QUEUE_APPOINTMENT_CREATED).build(); }
    @Bean public Queue appointmentConfirmedQueue() { return QueueBuilder.durable(QUEUE_APPOINTMENT_CONFIRMED).build(); }
    @Bean public Queue appointmentCancelledQueue() { return QueueBuilder.durable(QUEUE_APPOINTMENT_CANCELLED).build(); }
    @Bean public Queue appointmentNoShowQueue()    { return QueueBuilder.durable(QUEUE_APPOINTMENT_NOSHOW).build(); }

    @Bean public Binding createdBinding()   { return BindingBuilder.bind(appointmentCreatedQueue()).to(medsysExchange()).with("appointment.created"); }
    @Bean public Binding confirmedBinding() { return BindingBuilder.bind(appointmentConfirmedQueue()).to(medsysExchange()).with("appointment.confirmed"); }
    @Bean public Binding cancelledBinding() { return BindingBuilder.bind(appointmentCancelledQueue()).to(medsysExchange()).with("appointment.cancelled"); }
    @Bean public Binding noShowBinding()    { return BindingBuilder.bind(appointmentNoShowQueue()).to(medsysExchange()).with("appointment.noshow"); }

    @Bean public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(messageConverter());
        return t;
    }
}
