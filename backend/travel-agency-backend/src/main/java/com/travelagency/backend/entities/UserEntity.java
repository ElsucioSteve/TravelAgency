package com.travelagency.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Puente con Keycloak. Se llena con el 'sub' del JWT al registrar.
    @Column(unique = true)
    private String keycloakId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phoneNumber;

    @Column(unique = true)
    private String idDocument;

    private String nationality;

    private String addressStreet;
    private String addressNumber;
    private String district;

    private LocalDate birthDate;

    // Snapshot del rol para reportes (la fuente de verdad sigue siendo Keycloak)
    @Column(nullable = false)
    private String userRole;   // ADMIN o CLIENT

    @Column(nullable = false)
    private String accountStatus;   // ACTIVE, INACTIVE, BLOCKED

    private LocalDateTime registrationDate;
}