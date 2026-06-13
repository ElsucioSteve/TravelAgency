package com.travelagency.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "discount_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String valueType;          // PERCENTAGE o FIXED_AMOUNT

    @Column(nullable = false)
    private BigDecimal discountValue;  // Ej: 10 (si es 10%), o 50000 (si es monto fijo)

    private BigDecimal maxLimit;       // tope maximo del descuento

    @Column(nullable = false)
    private String discountStatus;     // ACTIVE o INACTIVE

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Column(nullable = false)
    private String applicationCriteria; // GROUP, FREQUENT, MULTI_PACKAGE, PROMO

    private Boolean isStackable;
}