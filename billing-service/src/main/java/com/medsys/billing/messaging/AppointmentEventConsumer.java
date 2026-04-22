package com.medsys.billing.messaging;

import com.medsys.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Écoute les événements de rendez-vous pour créer automatiquement
 * une facture draft lorsqu'un RDV est complété.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventConsumer {

    private final BillingService billingService;

    @RabbitListener(queues = "appointment.confirmed")
    public void onAppointmentConfirmed(Map<String, Object> event) {
        try {
            log.info("[BILLING] Reçu appointment.confirmed: {}", event.get("appointmentId"));
            // La facture sera créée manuellement par le secrétaire/médecin
            // Ceci est juste un log pour traçabilité
        } catch (Exception e) {
            log.warn("[BILLING] Erreur traitement appointment.confirmed: {}", e.getMessage());
        }
    }
}
