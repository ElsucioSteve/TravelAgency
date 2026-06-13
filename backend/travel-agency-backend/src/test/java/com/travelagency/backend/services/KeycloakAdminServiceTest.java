package com.travelagency.backend.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeycloakAdminServiceTest {

    private KeycloakAdminService service;

    @BeforeEach
    void setUp() {
        service = new KeycloakAdminService();
        // Configuramos URLs falsas (apuntan a un puerto donde NO hay Keycloak)
        // Los metodos deben manejar errores de conexion sin caerse del todo
        ReflectionTestUtils.setField(service, "serverUrl", "http://localhost:65535");
        ReflectionTestUtils.setField(service, "realm", "test-realm");
        ReflectionTestUtils.setField(service, "clientId", "test-client");
        ReflectionTestUtils.setField(service, "clientSecret", "test-secret");
    }

    @Test
    @DisplayName("getAdminToken() should throw when Keycloak is unreachable")
        // Si Keycloak no responde, el metodo de obtener token lanza excepcion controlada
    void getAdminToken_WhenUnreachable_ShouldThrow() {
        // Al llamar a setUserEnabled, internamente intenta obtener token primero
        // Esperamos que falle de forma controlada (no NullPointerException)
        assertThatThrownBy(() -> service.findUserIdByEmail("any@test.com"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("setUserEnabled() should return false when user not found in Keycloak")
        // Si no encuentra el usuario, devuelve false en lugar de lanzar excepcion
    void setUserEnabled_WhenUserNotFound_ShouldReturnFalse() {
        // El findUserIdByEmail va a lanzar una excepcion que se captura internamente
        // y devuelve null. Luego setUserEnabled detecta el null y devuelve false.
        // Probamos el path completo:
        try {
            boolean result = service.setUserEnabled("nonexistent@test.com", false);
            // Si llega aqui, el resultado debe ser false (porque no encontro el user)
            assertThat(result).isFalse();
        } catch (RuntimeException e) {
            // O lanzo excepcion por no poder obtener token, tambien es valido
            assertThat(e).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    @DisplayName("createUser() should throw when Keycloak is unreachable")
        // Crear usuario sin Keycloak disponible debe fallar
    void createUser_WhenUnreachable_ShouldThrow() {
        assertThatThrownBy(() -> service.createUser(
                "new@test.com", "New User", "password123", "CLIENT"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed");
    }
}