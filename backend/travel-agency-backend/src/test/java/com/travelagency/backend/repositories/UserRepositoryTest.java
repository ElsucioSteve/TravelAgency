package com.travelagency.backend.repositories;

import com.travelagency.backend.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private UserEntity sampleUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        sampleUser = new UserEntity();
        sampleUser.setEmail("test@example.com");
        sampleUser.setFullName("Test User");
        sampleUser.setKeycloakId("kc-id-123");
        sampleUser.setUserRole("CLIENT");
        sampleUser.setAccountStatus("ACTIVE");
        sampleUser.setRegistrationDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("save() debe persistir un usuario y asignarle un ID")
    void save_ShouldPersistAndAssignId() {
        UserEntity saved = userRepository.save(sampleUser);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("findByEmail() debe encontrar un usuario existente")
    void findByEmail_WhenUserExists_ShouldReturnUser() {
        userRepository.save(sampleUser);

        Optional<UserEntity> found = userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("findByEmail() debe devolver vacio si no existe")
    void findByEmail_WhenUserNotExists_ShouldReturnEmpty() {
        Optional<UserEntity> found = userRepository.findByEmail("noexiste@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findAll() debe devolver todos los usuarios")
    void findAll_ShouldReturnAllUsers() {
        userRepository.save(sampleUser);

        UserEntity user2 = new UserEntity();
        user2.setEmail("user2@example.com");
        user2.setFullName("User 2");
        user2.setKeycloakId("kc-id-456");
        user2.setUserRole("ADMIN");
        user2.setAccountStatus("ACTIVE");
        userRepository.save(user2);

        List<UserEntity> all = userRepository.findAll();

        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("delete() debe eliminar un usuario")
    void delete_ShouldRemoveUser() {
        UserEntity saved = userRepository.save(sampleUser);
        Long id = saved.getId();

        userRepository.deleteById(id);

        assertThat(userRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("update via save() debe modificar campos existentes")
    void update_ShouldModifyFields() {
        UserEntity saved = userRepository.save(sampleUser);
        saved.setFullName("Updated Name");
        saved.setPhoneNumber("+56987654321");
        userRepository.save(saved);

        UserEntity found = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getFullName()).isEqualTo("Updated Name");
        assertThat(found.getPhoneNumber()).isEqualTo("+56987654321");
    }
}