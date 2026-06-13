package com.travelagency.backend.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booking")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "package_id", nullable = false)
    private TravelPackageEntity travelPackage;

    private LocalDateTime bookingDate;

    @Column(nullable = false)
    private Integer passengerCount;

    private BigDecimal grossAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    @Column(nullable = false)
    private String bookingStatus;

    private LocalDateTime paymentExpirationDate;

    @Column(unique = true)
    private String bookingCode;

    // ----- lista de pasajeros -----
    // cascade = ALL → al borrar booking, se borran sus pasajeros
    // orphanRemoval = true → si quito un pasajero de la lista, se elimina de la BD
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @EqualsAndHashCode.Exclude
    private List<BookingPassengerEntity> passengers = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @EqualsAndHashCode.Exclude
    private Set<BookingDiscountEntity> appliedDiscounts = new HashSet<>();
}