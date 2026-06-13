package com.travelagency.backend.services;

import com.travelagency.backend.entities.UserEntity;
import com.travelagency.backend.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final BookingService bookingService;

    public UserService(UserRepository userRepository,
                       KeycloakAdminService keycloakAdminService,
                       BookingService bookingService) {
        this.userRepository = userRepository;
        this.keycloakAdminService = keycloakAdminService;
        this.bookingService = bookingService;
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<UserEntity> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public UserEntity getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public UserEntity saveUser(UserEntity user) {
        return userRepository.save(user);
    }

    // ----- ADMIN: actualizar cualquier campo (con sincronizacion Keycloak) -----
    @Transactional
    public UserEntity updateUserAsAdmin(Long id, UserEntity updates) {
        UserEntity existing = getUserById(id);

        String oldStatus = existing.getAccountStatus();
        String oldEmail = existing.getEmail();

        if (updates.getFullName() != null) existing.setFullName(updates.getFullName());
        if (updates.getEmail() != null) existing.setEmail(updates.getEmail());
        if (updates.getPhoneNumber() != null) existing.setPhoneNumber(updates.getPhoneNumber());
        if (updates.getIdDocument() != null) existing.setIdDocument(updates.getIdDocument());
        if (updates.getNationality() != null) existing.setNationality(updates.getNationality());
        if (updates.getAddressStreet() != null) existing.setAddressStreet(updates.getAddressStreet());
        if (updates.getAddressNumber() != null) existing.setAddressNumber(updates.getAddressNumber());
        if (updates.getDistrict() != null) existing.setDistrict(updates.getDistrict());
        if (updates.getBirthDate() != null) existing.setBirthDate(updates.getBirthDate());
        if (updates.getUserRole() != null) existing.setUserRole(updates.getUserRole());
        if (updates.getAccountStatus() != null) existing.setAccountStatus(updates.getAccountStatus());

        UserEntity saved = userRepository.save(existing);

        String newStatus = saved.getAccountStatus();

        // ACTIVE -> INACTIVE: deshabilitar Keycloak + cancelar PENDINGs
        if ("ACTIVE".equals(oldStatus) && "INACTIVE".equals(newStatus)) {
            logger.info("Usuario {} cambiado de ACTIVE a INACTIVE", oldEmail);

            boolean kcResult = keycloakAdminService.setUserEnabled(oldEmail, false);
            if (!kcResult) {
                logger.warn("No se pudo deshabilitar en Keycloak: {}", oldEmail);
            }

            int canceled = bookingService.cancelPendingBookingsForUser(saved.getId());
            logger.info("Reservas PENDING canceladas para usuario {}: {}", oldEmail, canceled);
        }

        // INACTIVE -> ACTIVE: reactivar en Keycloak
        if ("INACTIVE".equals(oldStatus) && "ACTIVE".equals(newStatus)) {
            logger.info("Usuario {} reactivado", saved.getEmail());
            boolean kcResult = keycloakAdminService.setUserEnabled(saved.getEmail(), true);
            if (!kcResult) {
                logger.warn("No se pudo habilitar en Keycloak: {}", saved.getEmail());
            }
        }

        return saved;
    }

    // ----- CLIENT: solo datos NO sensibles -----
    public UserEntity updateUserAsSelf(String email, UserEntity updates) {
        UserEntity existing = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        if (updates.getPhoneNumber() != null) existing.setPhoneNumber(updates.getPhoneNumber());
        if (updates.getAddressStreet() != null) existing.setAddressStreet(updates.getAddressStreet());
        if (updates.getAddressNumber() != null) existing.setAddressNumber(updates.getAddressNumber());
        if (updates.getDistrict() != null) existing.setDistrict(updates.getDistrict());
        if (updates.getNationality() != null) existing.setNationality(updates.getNationality());

        return userRepository.save(existing);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}