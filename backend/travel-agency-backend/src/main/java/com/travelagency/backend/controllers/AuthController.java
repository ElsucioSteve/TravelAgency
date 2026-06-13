package com.travelagency.backend.controllers;

import com.travelagency.backend.entities.UserEntity;
import com.travelagency.backend.repositories.UserRepository;
import com.travelagency.backend.services.KeycloakAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;

    public AuthController(UserRepository userRepository,
                          KeycloakAdminService keycloakAdminService) {
        this.userRepository = userRepository;
        this.keycloakAdminService = keycloakAdminService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        // ----- VALIDACIONES BASICAS -----

        if (request.email == null || request.email.trim().isEmpty()) {
            return errorResponse("Email is required");
        }
        if (!request.email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return errorResponse("Invalid email format");
        }
        if (request.password == null || request.password.length() < 6) {
            return errorResponse("Password must be at least 6 characters");
        }
        if (request.fullName == null || request.fullName.trim().isEmpty()) {
            return errorResponse("Full name is required");
        }

        String email = request.email.trim().toLowerCase();

        // ----- VERIFICAR QUE NO EXISTA -----

        if (userRepository.findByEmail(email).isPresent()) {
            return errorResponse("Email already registered. Try logging in instead.");
        }

        // ----- 1. CREAR EN KEYCLOAK -----

        String keycloakId;
        try {
            keycloakId = keycloakAdminService.createUser(
                    email,
                    request.fullName.trim(),
                    request.password,
                    "CLIENT"
            );
        } catch (Exception e) {
            logger.error("Error creando usuario en Keycloak: {}", email, e);
            return errorResponse("Could not register in identity provider: " + e.getMessage());
        }

        if (keycloakId == null) {
            return errorResponse("Could not register in identity provider (no ID returned)");
        }

        // ----- 2. CREAR EN BD -----

        UserEntity newUser = new UserEntity();
        newUser.setEmail(email);
        newUser.setFullName(request.fullName.trim());
        newUser.setKeycloakId(keycloakId);
        newUser.setPhoneNumber(request.phoneNumber);
        newUser.setIdDocument(request.idDocument);
        newUser.setNationality(request.nationality != null ? request.nationality : "Chilena");
        newUser.setDistrict(request.district);
        newUser.setUserRole("CLIENT");
        newUser.setAccountStatus("ACTIVE");
        newUser.setRegistrationDate(LocalDateTime.now());

        try {
            UserEntity saved = userRepository.save(newUser);
            logger.info("Usuario registrado: {} (id={})", email, saved.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Account created successfully. You can now log in.",
                    "userId", saved.getId(),
                    "email", saved.getEmail()
            ));
        } catch (Exception e) {
            logger.error("Error guardando usuario en BD, intentando rollback en Keycloak", e);

            // Rollback: si fallo guardar en BD, deshabilitar en Keycloak para evitar inconsistencia
            try {
                keycloakAdminService.setUserEnabled(email, false);
            } catch (Exception rollbackEx) {
                logger.error("Rollback en Keycloak tambien fallo para {}", email, rollbackEx);
            }

            return errorResponse("Could not save user. Please contact support.");
        }
    }

    private ResponseEntity<Map<String, String>> errorResponse(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    // ----- DTO -----

    public static class RegisterRequest {
        public String email;
        public String password;
        public String fullName;
        public String phoneNumber;
        public String idDocument;
        public String nationality;
        public String district;
    }
}