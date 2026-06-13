package com.travelagency.backend.repositories;

import com.travelagency.backend.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // Spring will automatically create the SQL: SELECT * FROM user_entity WHERE email = ?
    Optional<UserEntity> findByEmail(String email);

}
