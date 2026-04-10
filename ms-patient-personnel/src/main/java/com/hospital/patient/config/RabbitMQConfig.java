package com.hospital.patient.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologie RabbitMQ de ms-patient-personnel.
 *
 * <p>Tous les services partagent un seul exchange : {@code medsys.exchange}.
 * ms-patient-personnel utilise des queues avec des noms uniques pour ne pas
 * entrer en compétition avec ms-notify (qui écoute ses propres queues).</p>
 *
 * <p>Flux entrants :</p>
 * <ul>
 *   <li>{@code appointment.created / appointment.cancelled}
 *       → {@code patient.rdv.queue} : mise à jour du dossier local</li>
 *   <li>{@code user.created}
 *       → {@code user.created.patient.queue} : synchronisation compte patient</li>
 * </ul>
 *
 * <p>Flux sortants :</p>
 * <ul>
 *   <li>{@code patient.notification} → ms-notify (optionnel)</li>
 *   <li>{@code appointment.rebook}   → ms-rdv</li>
 * </ul>
 */
@Configuration
public class RabbitMQConfig {

    // ── Exchange unique partagé ────────────────────────────────────────────────
    public static final String EXCHANGE_NAME = "medsys.exchange";

    // ── Queues entrantes (noms uniques pour éviter la compétition avec ms-notify)
    /** Reçoit les événements de rendez-vous publiés par ms-rdv. */
    public static final String PATIENT_RDV_QUEUE          = "patient.rdv.queue";
    /** Reçoit les événements user.created publiés par ms-auth. */
    public static final String USER_CREATED_PATIENT_QUEUE = "user.created.patient.queue";

    // ── Routing keys entrants ─────────────────────────────────────────────────
    public static final String ROUTING_APPOINTMENT_CREATED   = "appointment.created";
    public static final String ROUTING_APPOINTMENT_CANCELLED = "appointment.cancelled";
    public static final String ROUTING_USER_CREATED          = "user.created";

    // ── Routing keys sortants ─────────────────────────────────────────────────
    public static final String ROUTING_PATIENT_NOTIFICATION = "patient.notification";
    public static final String ROUTING_APPOINTMENT_REBOOK   = "appointment.rebook";

    // ── Exchange ──────────────────────────────────────────────────────────────
    @Bean
    public TopicExchange medsysExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
    }

    // ── Queues entrantes ──────────────────────────────────────────────────────
    @Bean
    public Queue patientRdvQueue() {
        return QueueBuilder.durable(PATIENT_RDV_QUEUE).build();
    }

    @Bean
    public Queue userCreatedPatientQueue() {
        return QueueBuilder.durable(USER_CREATED_PATIENT_QUEUE).build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    /** ms-rdv → appointment.created → patient.rdv.queue */
    @Bean
    public Binding appointmentCreatedBinding(Queue patientRdvQueue,
                                              TopicExchange medsysExchange) {
        return BindingBuilder.bind(patientRdvQueue)
                .to(medsysExchange)
                .with(ROUTING_APPOINTMENT_CREATED);
    }

    /** ms-rdv → appointment.cancelled → patient.rdv.queue */
    @Bean
    public Binding appointmentCancelledBinding(Queue patientRdvQueue,
                                                TopicExchange medsysExchange) {
        return BindingBuilder.bind(patientRdvQueue)
                .to(medsysExchange)
                .with(ROUTING_APPOINTMENT_CANCELLED);
    }

    /** ms-auth → user.created → user.created.patient.queue */
    @Bean
    public Binding userCreatedBinding(Queue userCreatedPatientQueue,
                                       TopicExchange medsysExchange) {
        return BindingBuilder.bind(userCreatedPatientQueue)
                .to(medsysExchange)
                .with(ROUTING_USER_CREATED);
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
