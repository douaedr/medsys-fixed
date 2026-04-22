package com.medsys.billing.config;

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

    @Bean
    public Queue billingAppointmentConfirmedQueue() {
        return QueueBuilder.durable("billing.appointment.confirmed").build();
    }

    @Bean
    public Binding billingConfirmedBinding() {
        return BindingBuilder.bind(billingAppointmentConfirmedQueue())
                .to(medsysExchange()).with("appointment.confirmed");
    }

    @Bean public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(messageConverter());
        return t;
    }
}
