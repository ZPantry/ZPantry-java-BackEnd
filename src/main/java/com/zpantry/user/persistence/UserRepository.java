package com.zpantry.user.persistence;

import com.zpantry.user.domain.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByIdAndDeletedFalse(UUID id);
    Page<UserEntity> findAllByDeletedFalse(Pageable pageable);
    long countByDeletedFalse();
    Optional<UserEntity> findByEmailAndDeletedFalse(String email);
    Optional<UserEntity> findByRefreshTokenHashAndDeletedFalse(String refreshTokenHash);
    boolean existsByRoleIgnoreCaseAndDeletedFalse(String role);
    boolean existsByRoleIgnoreCase(String role);
}
