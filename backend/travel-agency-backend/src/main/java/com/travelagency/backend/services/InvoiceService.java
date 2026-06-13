package com.travelagency.backend.services;

import com.travelagency.backend.entities.BookingEntity;
import com.travelagency.backend.entities.InvoiceEntity;
import com.travelagency.backend.entities.PaymentEntity;
import com.travelagency.backend.repositories.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    // ----- LECTURAS -----

    public List<InvoiceEntity> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public InvoiceEntity getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
    }

    public Optional<InvoiceEntity> getInvoiceByPaymentId(Long paymentId) {
        return invoiceRepository.findByPaymentId(paymentId);
    }

    public Optional<InvoiceEntity> getInvoiceByFolio(String folio) {
        return invoiceRepository.findByFolioNumber(folio);
    }

    // ----- GENERACION AUTOMATICA TRAS PAGO -----
    // Lo llama PaymentService despues de confirmar el pago.

    public InvoiceEntity generateInvoiceForPayment(PaymentEntity payment, String invoiceType) {
        // Validar que no exista ya una factura para este pago
        if (invoiceRepository.findByPaymentId(payment.getId()).isPresent()) {
            throw new RuntimeException("Invoice already exists for payment id: " + payment.getId());
        }

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setPayment(payment);
        invoice.setIssueDate(LocalDateTime.now());
        invoice.setInvoiceType(invoiceType);
        invoice.setFolioNumber(generateFolioNumber(invoiceType));
        invoice.setSnapshotDetails(buildSnapshot(payment));

        return invoiceRepository.save(invoice);
    }

    // ----- UTILIDADES -----

    private String generateFolioNumber(String invoiceType) {
        // Formato: F-{year}-{numero correlativo de 5 digitos}
        // Usamos el COUNT actual de facturas de ese tipo como base para el correlativo
        long count = invoiceRepository.countByInvoiceType(invoiceType) + 1;
        String prefix = "RECEIPT".equals(invoiceType) ? "B" : "F";
        return String.format("%s-%d-%05d", prefix, LocalDateTime.now().getYear(), count);
    }

    private String buildSnapshot(PaymentEntity payment) {
        // Generamos un texto simple (en produccion seria JSON serializado).
        // Esto congela los datos al momento de emitir la factura.
        BookingEntity booking = payment.getBooking();

        StringBuilder snapshot = new StringBuilder();
        snapshot.append("=== BOLETA / FACTURA ===\n");
        snapshot.append("Folio: (se asigna al guardar)\n");
        snapshot.append("Fecha emision: ").append(LocalDateTime.now()).append("\n\n");

        snapshot.append("--- CLIENTE ---\n");
        snapshot.append("Nombre: ").append(booking.getUser().getFullName()).append("\n");
        snapshot.append("Email: ").append(booking.getUser().getEmail()).append("\n");
        snapshot.append("Documento: ").append(booking.getUser().getIdDocument()).append("\n\n");

        snapshot.append("--- RESERVA ---\n");
        snapshot.append("Codigo: ").append(booking.getBookingCode()).append("\n");
        snapshot.append("Paquete: ").append(booking.getTravelPackage().getName()).append("\n");
        snapshot.append("Destino: ").append(booking.getTravelPackage().getDestination()).append("\n");
        snapshot.append("Pasajeros: ").append(booking.getPassengerCount()).append("\n");
        snapshot.append("Fecha viaje: ").append(booking.getTravelPackage().getStartDate())
                .append(" al ").append(booking.getTravelPackage().getEndDate()).append("\n\n");

        snapshot.append("--- MONTOS ---\n");
        snapshot.append("Monto bruto: $").append(booking.getGrossAmount()).append("\n");
        snapshot.append("Descuento aplicado: $").append(booking.getDiscountAmount()).append("\n");
        snapshot.append("Monto pagado: $").append(payment.getAmountPaid()).append("\n\n");

        snapshot.append("--- PAGO ---\n");
        snapshot.append("Metodo: ").append(payment.getPaymentMethod()).append("\n");
        snapshot.append("Tarjeta: ").append(payment.getMaskedCardNumber()).append("\n");
        snapshot.append("Transaction ID: ").append(payment.getInternalTransactionId()).append("\n");
        snapshot.append("Fecha pago: ").append(payment.getPaymentDate()).append("\n");

        return snapshot.toString();
    }
}