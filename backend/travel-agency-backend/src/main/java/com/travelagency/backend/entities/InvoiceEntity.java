package com.travelagency.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cada factura corresponde a UN pago (relacion 1-1)
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private PaymentEntity payment;

    @Column(unique = true, nullable = false)
    private String folioNumber;          // F-2026-00001

    private LocalDateTime issueDate;

    @Column(nullable = false)
    private String invoiceType;          // RECEIPT (boleta) o INVOICE (factura)

    @Column(columnDefinition = "TEXT")
    private String snapshotDetails;      // Texto/JSON con detalles congelados
}