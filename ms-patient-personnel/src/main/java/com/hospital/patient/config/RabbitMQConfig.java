package com.hospital.patient.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Exchanges ─────────────────────────────────────────────────────────────
    // medsys.exchange : événements RDV publiés par ms-rdv
    public static final String MEDSYS_EXCHANGE  = "medsys.exchange";
    // auth.exchange : événements auth publiés par ms-auth
    public static final String AUTH_EXCHANGE    = "auth.exchange";
    // patient.exchange : notifications sortantes vers ms-notify
    public static final String PATIENT_EXCHANGE = "patient.exchange";

    // ── Queues ────────────────────────────────────────────────────────────────
    // patient.queue         : reçoit les événements RDV (appointment.created / cancelled) depuis ms-rdv
    public static final String PATIENT_QUEUE              = "patient.queue";
    // patient.auth.queue    : reçoit les événements user.created depuis ms-auth
    public static final String PATIENT_AUTH_QUEUE         = "patient.auth.queue";
    // patient.notification.queue : notifications sortantes
    public static final String PATIENT_NOTIFICATION_QUEUE = "patient.notification.queue";

    // ── Routing keys (inbound RDV) ────────────────────────────────────────────
    public static final String ROUTING_APPOINTMENT_CREATED   = "appointment.created";
    public static final String ROUTING_APPOINTMENT_CANCELLED = "appointment.cancelled";

    // ── Routing keys (inbound Auth) ───────────────────────────────────────────
    public static final String ROUTING_USER_CREATED = "user.created";

    // ── Routing keys (outbound) ───────────────────────────────────────────────
    public static final String ROUTING_PATIENT_NOTIFICATION = "patient.notification";
    public static final String ROUTING_APPOINTMENT_REBOOK   = "appointment.rebook";

    // ── Exchanges ─────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange medsysExchange() {
        return ExchangeBuilder.topicExchange(MEDSYS_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange authExchange() {
        return ExchangeBuilder.topicExchange(AUTH_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange patientExchange() {
        return ExchangeBuilder.topicExchange(PATIENT_EXCHANGE).durable(true).build();
    }

    // ── Queues ────────────────────────────────────────────────────────────────

    @Bean
    public Queue patientQueue() {
        return QueueBuilder.durable(PATIENT_QUEUE).build();
    }

    @Bean
    public Queue patientAuthQueue() {
        return QueueBuilder.durable(PATIENT_AUTH_QUEUE).build();
    }

    @Bean
    public Queue patientNotificationQueue() {
        return QueueBuilder.durable(PATIENT_NOTIFICATION_QUEUE).build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    // RDV créé : medsys.exchange → patient.queue
    @Bean
    public Binding appointmentCreatedBinding(Queue patientQueue,
                                              TopicExchange medsysExchange) {
        return BindingBuilder.bind(patientQueue)
                .to(medsysExchange)
                .with(ROUTING_APPOINTMENT_CREATED);
    }

    // RDV annulé : medsys.exchange → patient.queue
    @Bean
    public Binding appointmentCancelledBinding(Queue patientQueue,
                                                TopicExchange medsysExchange) {
        return BindingBuilder.bind(patientQueue)
                .to(medsysExchange)
                .with(ROUTING_APPOINTMENT_CANCELLED);
    }

    // user.created : auth.exchange → patient.auth.queue (queue dédiée pour éviter conflit de type)
    @Bean
    public Binding userCreatedBinding(Queue patientAuthQueue,
                                       TopicExchange authExchange) {
        return BindingBuilder.bind(patientAuthQueue)
                .to(authExchange)
                .with(ROUTING_USER_CREATED);
    }

    // Notifications sortantes : patient.exchange → patient.notification.queue
    @Bean
    public Binding patientNotificationBinding(Queue patientNotificationQueue,
                                               TopicExchange patientExchange) {
        return BindingBuilder.bind(patientNotificationQueue)
                .to(patientExchange)
                .with(ROUTING_PATIENT_NOTIFICATION);
    }

    // ── JSON serialization (interoperable avec .NET) ──────────────────────────

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
