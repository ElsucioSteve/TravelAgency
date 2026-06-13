package com.travelagency.backend.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Entity
@Table(name = "booking_passenger")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingPassengerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RELACION inversa con Booking.
    // @JsonBackReference evita el loop infinito cuando Jackson serializa.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @JsonBackReference
    @EqualsAndHashCode.Exclude
    private BookingEntity booking;

    @Column(nullable = false)
    private String fullName;

    private String idDocument;
    private LocalDate birthDate;
    private String nationality;

    @Column(columnDefinition = "TEXT")
    private String observations;
}