package com.solra.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for UserAccountEntity.
 */
@Repository
public interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, String> {
    Optional<UserAccountEntity> findByPhone(String phone);
    Optional<UserAccountEntity> findByUsername(String username);
    Optional<UserAccountEntity> findByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
