package com.travelagency.backend.controllers;

import com.travelagency.backend.entities.BookingEntity;
import com.travelagency.backend.entities.PaymentEntity;
import com.travelagency.backend.entities.UserEntity;
import com.travelagency.backend.repositories.UserRepository;
import com.travelagency.backend.services.BookingService;
import com.travelagency.backend.services.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final UserRepository userRepository;

    public PaymentController(PaymentService paymentService,
                             BookingService bookingService,
                             UserRepository userRepository) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    // ----- ADMIN: ver todos los pagos -----

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<PaymentEntity> getAllPayments() {
        return paymentService.getAllPayments();
    }

    // ----- USUARIO AUTENTICADO -----

    @GetMapping("/{id}")
    public PaymentEntity getPaymentById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        PaymentEntity payment = paymentService.getPaymentById(id);
        ensureOwnerOrAdmin(payment.getBooking(), jwt);
        return payment;
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentEntity> getPaymentByBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal Jwt jwt) {
        BookingEntity booking = bookingService.getBookingById(bookingId);
        ensureOwnerOrAdmin(booking, jwt);

        Optional<PaymentEntity> payment = paymentService.getPaymentByBookingId(bookingId);
        return payment.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // ----- PROCESAR PAGO -----

    @PostMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentEntity> processPayment(
            @PathVariable Long bookingId,
            @RequestBody PaymentService.PaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        BookingEntity booking = bookingService.getBookingById(bookingId);
        ensureOwnerOrAdmin(booking, jwt);

        PaymentEntity payment = paymentService.processPayment(bookingId, request);
        return new ResponseEntity<>(payment, HttpStatus.CREATED);
    }

    // ----- METODO DE SEGURIDAD -----

    private void ensureOwnerOrAdmin(BookingEntity booking, Jwt jwt) {
        boolean isAdmin = jwt.getClaimAsMap("realm_access") != null &&
                ((List<?>) jwt.getClaimAsMap("realm_access").get("roles")).contains("ADMIN");

        if (isAdmin) return;

        String email = jwt.getClaimAsString("email");
        if (!booking.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("You can only access your own payments");
        }
    }
}