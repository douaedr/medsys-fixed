package com.medsys.billing.service;

import com.medsys.billing.entity.Invoice;
import com.medsys.billing.entity.InvoiceItem;
import com.medsys.billing.entity.Payment;
import com.medsys.billing.enums.InvoiceStatus;
import com.medsys.billing.enums.PaymentMethod;
import com.medsys.billing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final InvoiceRepository invoiceRepo;

    // ── Création facture ───────────────────────────────────────────────────
    public Invoice createInvoice(Map<String, Object> req) {
        Invoice invoice = Invoice.builder()
                .patientId(longVal(req, "patientId"))
                .patientNom(str(req, "patientNom"))
                .patientPrenom(str(req, "patientPrenom"))
                .patientCin(str(req, "patientCin"))
                .appointmentId(longVal(req, "appointmentId"))
                .medecinId(longVal(req, "medecinId"))
                .medecinNom(str(req, "medecinNom"))
                .notes(str(req, "notes"))
                .status(InvoiceStatus.PENDING)
                .build();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsReq = (List<Map<String, Object>>) req.getOrDefault("items", List.of());

        List<InvoiceItem> items = new ArrayList<>();
        for (Map<String, Object> ir : itemsReq) {
            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .description(str(ir, "description"))
                    .code(str(ir, "code"))
                    .categorie(str(ir, "categorie"))
                    .quantite(intVal(ir, "quantite", 1))
                    .prixUnitaire(bigDecimal(ir, "prixUnitaire"))
                    .build();
            item.calculateTotal();
            items.add(item);
        }

        invoice.setItems(items);
        invoice.setMontantTotal(items.stream()
                .map(i -> i.getPrixTotal() != null ? i.getPrixTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal remise = bigDecimal(req, "remise");
        if (remise != null) invoice.setRemise(remise);

        Invoice saved = invoiceRepo.save(invoice);
        log.info("[BILLING] Facture créée: {} pour patientId={}", saved.getNumeroFacture(), saved.getPatientId());
        return saved;
    }

    public Invoice getById(Long id) {
        return invoiceRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Facture non trouvée: " + id));
    }

    public List<Invoice> getByPatient(Long patientId) {
        return invoiceRepo.findByPatientId(patientId);
    }

    public List<Invoice> getAll() {
        return invoiceRepo.findAll();
    }

    public List<Invoice> getByStatus(InvoiceStatus status) {
        return invoiceRepo.findByStatus(status);
    }

    // ── Paiement ───────────────────────────────────────────────────────────
    public Invoice addPayment(Long invoiceId, Map<String, Object> req) {
        Invoice invoice = getById(invoiceId);

        BigDecimal montant = bigDecimal(req, "montant");
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Montant de paiement invalide");

        Payment payment = Payment.builder()
                .invoice(invoice)
                .montant(montant)
                .methode(parseEnum(PaymentMethod.class, str(req, "methode"), PaymentMethod.CASH))
                .datePaiement(LocalDate.now())
                .reference(str(req, "reference"))
                .notes(str(req, "notes"))
                .build();

        invoice.getPayments().add(payment);

        BigDecimal totalPaye = invoice.getMontantPaye().add(montant);
        invoice.setMontantPaye(totalPaye);

        BigDecimal netDu = invoice.getMontantTotal().subtract(invoice.getRemise());
        if (totalPaye.compareTo(netDu) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        Invoice saved = invoiceRepo.save(invoice);
        log.info("[BILLING] Paiement de {} ajouté à la facture {}", montant, invoice.getNumeroFacture());
        return saved;
    }

    public Invoice cancel(Long id) {
        Invoice invoice = getById(id);
        if (invoice.getStatus() == InvoiceStatus.PAID)
            throw new IllegalStateException("Impossible d'annuler une facture payée");
        invoice.setStatus(InvoiceStatus.CANCELLED);
        return invoiceRepo.save(invoice);
    }

    // ── Statistiques ───────────────────────────────────────────────────────
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalFactures", invoiceRepo.count());
        stats.put("enAttente", invoiceRepo.countByStatus(InvoiceStatus.PENDING));
        stats.put("payees", invoiceRepo.countByStatus(InvoiceStatus.PAID));
        stats.put("partiellementPayees", invoiceRepo.countByStatus(InvoiceStatus.PARTIALLY_PAID));
        stats.put("annulees", invoiceRepo.countByStatus(InvoiceStatus.CANCELLED));
        stats.put("revenuTotal", invoiceRepo.sumRevenuTotal());
        stats.put("montantEnAttente", invoiceRepo.sumMontantEnAttente());
        stats.put("nouvellesCeMois", invoiceRepo.countByCreatedAtAfter(
                LocalDateTime.now().withDayOfMonth(1).withHour(0)));
        return stats;
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key); return v != null ? v.toString() : null;
    }

    private Long longVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private int intVal(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v == null) return def;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }

    private BigDecimal bigDecimal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return null; }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String val, E def) {
        if (val == null) return def;
        try { return Enum.valueOf(cls, val.toUpperCase()); } catch (Exception e) { return def; }
    }
}
