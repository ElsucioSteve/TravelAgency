package com.travelagency.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Un pago pertenece a UNA reserva (relacion 1-1).
    // unique = true a nivel BD asegura que no haya 2 pagos para la misma reserva.
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private BookingEntity booking;

    private LocalDateTime paymentDate;

    @Column(nullable = false)
    private BigDecimal amountPaid;

    @Column(nullable = false)
    private String paymentMethod;        // CREDIT_CARD (por ahora solo este)

    @Column(nullable = false)
    private String paymentStatus;        // APPROVED (siempre)

    @Column(unique = true)
    private String internalTransactionId;

    // Datos simulados de la tarjeta (NUNCA guardar tarjetas reales asi en produccion)
    private String maskedCardNumber;     // **** **** **** 4444
    private String cardExpirationDate;   // MM/YY
    private String simulatedCvv;
}