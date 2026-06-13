package com.travelagency.backend.services;

import com.travelagency.backend.entities.UserEntity;
import com.travelagency.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private UserService userService;

    private UserEntity sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new UserEntity();
        sampleUser.setId(1L);
        sampleUser.setEmail("test@example.com");
        sampleUser.setFullName("Test User");
        sampleUser.setKeycloakId("kc-id-123");
        sampleUser.setUserRole("CLIENT");
        sampleUser.setAccountStatus("ACTIVE");
    }

    @Test
    @DisplayName("getAllUsers() should return the list of users")
        // Devuelve todos los usuarios del repositorio
    void getAllUsers_ShouldReturnAll() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<UserEntity> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("getUserById() should return the user when it exists")
        // Busca un usuario por ID que si existe
    void getUserById_WhenExists_ShouldReturn() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserEntity result = userService.getUserById(1L);

        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("getUserById() should throw when user does not exist")
        // Lanza excepcion cuando el usuario no existe
    void getUserById_WhenNotExists_ShouldThrow() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("getUserByEmail() should return Optional with user when exists")
        // Busca por email y verifica que devuelve Optional con valor
    void getUserByEmail_WhenExists_ShouldReturnOptional() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        Optional<UserEntity> result = userService.getUserByEmail("test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("saveUser() should call repository.save()")
        // Verifica que delega al repositorio
    void saveUser_ShouldCallRepository() {
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);

        UserEntity result = userService.saveUser(sampleUser);

        assertThat(result).isEqualTo(sampleUser);
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("updateUserAsAdmin() should update fields correctly")
        // Como admin, actualiza varios campos
    void updateUserAsAdmin_ShouldUpdateFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserEntity updates = new UserEntity();
        updates.setFullName("Updated Name");
        updates.setPhoneNumber("+56999999999");

        UserEntity result = userService.updateUserAsAdmin(1L, updates);

        assertThat(result.getFullName()).isEqualTo("Updated Name");
        assertThat(result.getPhoneNumber()).isEqualTo("+56999999999");
        // Email y rol no se modificaron porque no se enviaron
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("updateUserAsAdmin() ACTIVE->INACTIVE should sync Keycloak and cancel PENDINGs")
        // Cuando admin desactiva: sincroniza Keycloak Y cancela reservas pendientes
    void updateUserAsAdmin_WhenDeactivating_ShouldSyncKeycloakAndCancelBookings() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(keycloakAdminService.setUserEnabled(anyString(), eq(false))).thenReturn(true);
        when(bookingService.cancelPendingBookingsForUser(1L)).thenReturn(3);

        UserEntity updates = new UserEntity();
        updates.setAccountStatus("INACTIVE");

        userService.updateUserAsAdmin(1L, updates);

        verify(keycloakAdminService).setUserEnabled("test@example.com", false);
        verify(bookingService).cancelPendingBookingsForUser(1L);
    }

    @Test
    @DisplayName("updateUserAsAdmin() INACTIVE->ACTIVE should enable in Keycloak")
        // Cuando admin reactiva: solo habilita en Keycloak (no toca reservas)
    void updateUserAsAdmin_WhenActivating_ShouldEnableInKeycloak() {
        sampleUser.setAccountStatus("INACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(keycloakAdminService.setUserEnabled(anyString(), eq(true))).thenReturn(true);

        UserEntity updates = new UserEntity();
        updates.setAccountStatus("ACTIVE");

        userService.updateUserAsAdmin(1L, updates);

        verify(keycloakAdminService).setUserEnabled("test@example.com", true);
        verify(bookingService, never()).cancelPendingBookingsForUser(anyLong());
    }

    @Test
    @DisplayName("updateUserAsAdmin() without status change should NOT touch Keycloak")
        // Si solo cambian otros campos (sin tocar estado), no se sincroniza con Keycloak
    void updateUserAsAdmin_WhenStatusUnchanged_ShouldNotTouchKeycloak() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserEntity updates = new UserEntity();
        updates.setFullName("Only name change");

        userService.updateUserAsAdmin(1L, updates);

        verify(keycloakAdminService, never()).setUserEnabled(anyString(), anyBoolean());
        verify(bookingService, never()).cancelPendingBookingsForUser(anyLong());
    }

    @Test
    @DisplayName("updateUserAsSelf() should only update NON-sensitive fields")
        // Cliente solo puede cambiar datos suyos no sensibles (no rol, no email, no status)
    void updateUserAsSelf_ShouldOnlyUpdateAllowedFields() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserEntity updates = new UserEntity();
        updates.setPhoneNumber("+56988887777");
        updates.setDistrict("Las Condes");
        updates.setUserRole("ADMIN");           // Intento malicioso, NO debe cambiar
        updates.setAccountStatus("INACTIVE");   // Tampoco
        updates.setEmail("hacker@evil.com");    // Tampoco

        UserEntity result = userService.updateUserAsSelf("test@example.com", updates);

        assertThat(result.getPhoneNumber()).isEqualTo("+56988887777");
        assertThat(result.getDistrict()).isEqualTo("Las Condes");
        // Estos no cambian
        assertThat(result.getUserRole()).isEqualTo("CLIENT");
        assertThat(result.getAccountStatus()).isEqualTo("ACTIVE");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("updateUserAsSelf() should throw when email not found")
        // Lanza excepcion si el email no existe en BD
    void updateUserAsSelf_WhenUserNotFound_ShouldThrow() {
        when(userRepository.findByEmail("nope@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserAsSelf("nope@test.com", new UserEntity()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("deleteUser() should call repository.deleteById()")
        // Verifica que delega la eliminacion al repositorio
    void deleteUser_ShouldCallRepository() {
        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("updateUserAsAdmin() should log warning when Keycloak deactivation fails")
        // Si Keycloak falla al deshabilitar, igual procede con la cancelacion de PENDINGs en BD
    void updateUserAsAdmin_WhenKeycloakDeactivationFails_ShouldStillCancelBookings() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(keycloakAdminService.setUserEnabled(anyString(), eq(false))).thenReturn(false);
        when(bookingService.cancelPendingBookingsForUser(1L)).thenReturn(2);

        UserEntity updates = new UserEntity();
        updates.setAccountStatus("INACTIVE");

        userService.updateUserAsAdmin(1L, updates);

        // Aunque Keycloak fallo, las reservas PENDING SI se cancelan
        verify(bookingService).cancelPendingBookingsForUser(1L);
    }

    @Test
    @DisplayName("updateUserAsAdmin() should log warning when Keycloak activation fails")
        // Si Keycloak falla al habilitar, no se rompe el flujo
    void updateUserAsAdmin_WhenKeycloakActivationFails_ShouldNotThrow() {
        sampleUser.setAccountStatus("INACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(keycloakAdminService.setUserEnabled(anyString(), eq(true))).thenReturn(false);

        UserEntity updates = new UserEntity();
        updates.setAccountStatus("ACTIVE");

        UserEntity result = userService.updateUserAsAdmin(1L, updates);

        assertThat(result.getAccountStatus()).isEqualTo("ACTIVE");
    }
}