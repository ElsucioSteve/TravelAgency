package com.travelagency.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class KeycloakAdminService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminService.class);

    @Value("${keycloak.admin.server-url:http://localhost:9090}")
    private String serverUrl;

    @Value("${keycloak.admin.realm:sisgr-realm}")
    private String realm;

    @Value("${keycloak.admin.client-id:sisgr-backend}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    private final RestClient restClient = RestClient.create();

    // ---------- 1. OBTENER TOKEN DE ADMIN ----------

    private String getAdminToken() {
        String url = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        try {
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new RuntimeException("Could not obtain admin token from Keycloak");
            }
            return (String) response.get("access_token");
        } catch (Exception e) {
            logger.error("Error obteniendo token admin de Keycloak", e);
            throw new RuntimeException("Failed to get Keycloak admin token: " + e.getMessage());
        }
    }

    // ---------- 2. BUSCAR USUARIO POR EMAIL ----------

    public String findUserIdByEmail(String email) {
        String token = getAdminToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users?email=" + email + "&exact=true";

        try {
            List<Map<String, Object>> users = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(List.class);

            if (users == null || users.isEmpty()) {
                logger.warn("Usuario no encontrado en Keycloak: {}", email);
                return null;
            }

            return (String) users.get(0).get("id");
        } catch (Exception e) {
            logger.error("Error buscando usuario {} en Keycloak", email, e);
            return null;
        }
    }

    // ---------- 3. HABILITAR / DESHABILITAR USUARIO ----------

    public boolean setUserEnabled(String email, boolean enabled) {
        String userId = findUserIdByEmail(email);
        if (userId == null) {
            logger.warn("No se puede actualizar usuario inexistente en Keycloak: {}", email);
            return false;
        }

        String token = getAdminToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId;

        Map<String, Object> body = Map.of("enabled", enabled);

        try {
            restClient.put()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            logger.info("Usuario {} en Keycloak ahora enabled={}", email, enabled);
            return true;
        } catch (Exception e) {
            logger.error("Error actualizando enabled={} para {} en Keycloak", enabled, email, e);
            return false;
        }
    }

    // ---------- 4. CREAR USUARIO EN KEYCLOAK (para registro publico mas tarde) ----------

    public String createUser(String email, String fullName, String password, String role) {
        String token = getAdminToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users";

        // Separar fullName en firstName y lastName (mejor esfuerzo)
        String[] parts = fullName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "";

        Map<String, Object> userPayload = Map.of(
                "username", email,  // Usamos email como username
                "email", email,
                "emailVerified", true,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false
                ))
        );

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userPayload)
                    .retrieve()
                    .toBodilessEntity();

            // Keycloak responde 201 y devuelve la URL del nuevo usuario en el header Location
            String location = response.getHeaders().getFirst("Location");
            String keycloakId = location != null ? location.substring(location.lastIndexOf("/") + 1) : null;

            // Asignar rol del realm
            if (keycloakId != null && role != null) {
                assignRealmRole(keycloakId, role);
            }

            logger.info("Usuario {} creado en Keycloak con id {}", email, keycloakId);
            return keycloakId;
        } catch (Exception e) {
            logger.error("Error creando usuario {} en Keycloak", email, e);
            throw new RuntimeException("Failed to create user in Keycloak: " + e.getMessage());
        }
    }

    // ---------- 5. ASIGNAR ROL DE REALM ----------

    private void assignRealmRole(String userId, String roleName) {
        String token = getAdminToken();

        // Primero obtenemos info del rol
        String roleUrl = serverUrl + "/admin/realms/" + realm + "/roles/" + roleName;
        Map<String, Object> role = restClient.get()
                .uri(roleUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(Map.class);

        if (role == null) {
            logger.warn("Rol {} no existe en Keycloak", roleName);
            return;
        }

        // Asignar al usuario
        String assignUrl = serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
        restClient.post()
                .uri(assignUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();

        logger.info("Rol {} asignado al usuario {}", roleName, userId);
    }
}