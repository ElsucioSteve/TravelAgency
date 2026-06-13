package com.travelagency.backend.services;

import com.travelagency.backend.entities.BookingEntity;
import com.travelagency.backend.entities.PaymentEntity;
import com.travelagency.backend.repositories.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.travelagency.backend.entities.InvoiceEntity;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final InvoiceService invoiceService;

    public PaymentService(PaymentRepository paymentRepository, BookingService bookingService, InvoiceService invoiceService) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.invoiceService = invoiceService;
    }

    // ----- LECTURAS -----

    public List<PaymentEntity> getAllPayments() {
        return paymentRepository.findAll();
    }

    public PaymentEntity getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
    }

    public Optional<PaymentEntity> getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    // ----- PROCESAR PAGO -----
    // @Transactional: si algo falla, todo se revierte (incluyendo el cambio de estado del booking)

    @Transactional
    public PaymentEntity processPayment(Long bookingId, PaymentRequest request) {
        // 1. Obtener la reserva
        BookingEntity booking = bookingService.getBookingById(bookingId);

        // 2. Validar que la reserva este en estado pagable
        validateBookingIsPayable(booking);

        // 3. Validar que no exista ya un pago para esta reserva
        if (paymentRepository.findByBookingId(bookingId).isPresent()) {
            throw new RuntimeException("This booking already has a payment registered");
        }

        // 4. Validar monto
        if (request.amount == null || request.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Payment amount must be greater than zero");
        }

        // 5. Validar que el monto coincida con el final de la reserva
        if (request.amount.compareTo(booking.getFinalAmount()) != 0) {
            throw new RuntimeException(
                    "Payment amount (" + request.amount + ") does not match booking final amount (" + booking.getFinalAmount() + "). " +
                            "Partial payments are not allowed."
            );
        }

        // 6. Validar datos minimos de tarjeta simulada
        validateCardData(request);

        // 7. Crear el registro de pago (siempre APPROVED por ser simulado)
        PaymentEntity payment = new PaymentEntity();
        payment.setBooking(booking);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setAmountPaid(request.amount);
        payment.setPaymentMethod("CREDIT_CARD");
        payment.setPaymentStatus("APPROVED");
        payment.setInternalTransactionId(generateTransactionId());
        payment.setMaskedCardNumber(maskCardNumber(request.cardNumber));
        payment.setCardExpirationDate(request.cardExpirationDate);
        payment.setSimulatedCvv(request.cvv);

        PaymentEntity saved = paymentRepository.save(payment);

        // 8. Confirmar la reserva (cambiar de PENDING a CONFIRMED)
        bookingService.confirmBookingAfterPayment(bookingId);

        // 9. generar factura automaticamente
        String invoiceType = request.invoiceType != null ? request.invoiceType : "RECEIPT";
        if (!"RECEIPT".equals(invoiceType) && !"INVOICE".equals(invoiceType)) {
            invoiceType = "RECEIPT";  // valor por defecto seguro
        }
        invoiceService.generateInvoiceForPayment(saved, invoiceType);

        return saved;
    }

    // ----- VALIDACIONES -----

    private void validateBookingIsPayable(BookingEntity booking) {
        String status = booking.getBookingStatus();

        if ("CANCELED".equals(status)) {
            throw new RuntimeException("Cannot pay a canceled booking");
        }
        if ("EXPIRED".equals(status)) {
            throw new RuntimeException("Cannot pay an expired booking");
        }
        if ("CONFIRMED".equals(status)) {
            throw new RuntimeException("Booking is already paid and confirmed");
        }
        if (!"PENDING".equals(status)) {
            throw new RuntimeException("Booking is in invalid state for payment: " + status);
        }

        // Validar que no haya expirado el plazo de pago
        if (booking.getPaymentExpirationDate() != null &&
                LocalDateTime.now().isAfter(booking.getPaymentExpirationDate())) {
            throw new RuntimeException("Payment deadline has expired");
        }
    }

    private void validateCardData(PaymentRequest request) {
        if (request.cardNumber == null || request.cardNumber.replaceAll("\\s", "").length() < 13) {
            throw new RuntimeException("Invalid card number");
        }
        if (request.cardExpirationDate == null || !request.cardExpirationDate.matches("\\d{2}/\\d{2}")) {
            throw new RuntimeException("Invalid card expiration date (expected MM/YY)");
        }
        if (request.cvv == null || !request.cvv.matches("\\d{3,4}")) {
            throw new RuntimeException("Invalid CVV");
        }
    }

    // ----- UTILIDADES -----

    private String maskCardNumber(String cardNumber) {
        String clean = cardNumber.replaceAll("\\s", "");
        if (clean.length() < 4) return "****";
        String lastFour = clean.substring(clean.length() - 4);
        return "**** **** **** " + lastFour;
    }

    private String generateTransactionId() {
        // Formato: TXN-{anio}-{8 caracteres random}
        return "TXN-" + LocalDateTime.now().getYear() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ----- DTO PARA EL REQUEST -----

    public static class PaymentRequest {
        public BigDecimal amount;
        public String cardNumber;
        public String cardExpirationDate;  // MM/YY
        public String cvv;
        public String invoiceType;
    }
}