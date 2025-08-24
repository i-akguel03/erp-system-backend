package com.erp.backend.repository;

import com.erp.backend.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<UserEntity, String> {

    Optional<UserEntity> findByUsername(String username);

    // ← diese Methode für userExists()
    boolean existsByUsername(String username);
}
