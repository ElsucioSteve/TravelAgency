package com.travelagency.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "travel_package")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPackageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String destination;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private Integer durationDays;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(columnDefinition = "TEXT")
    private String includedServices;

    @Column(columnDefinition = "TEXT")
    private String conditions;

    @Column(columnDefinition = "TEXT")
    private String restrictions;

    @Column(nullable = false)
    private Integer totalSlots;

    @Column(nullable = false)
    private Integer availableSlots;

    private String travelType;     // NATIONAL, INTERNATIONAL
    private String season;         // HIGH, LOW, MEDIUM
    private String category;       // ADVENTURE, FAMILY, BEACH, CULTURAL, LUXURY

    @Column(nullable = false)
    private String packageStatus;  // AVAILABLE, SOLD_OUT, EXPIRED, CANCELED

    private Boolean isVisibleWeb;
}