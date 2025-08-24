package com.erp.backend.repository;

import com.erp.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    // für userExists()
    boolean existsByUsername(String username);
}
