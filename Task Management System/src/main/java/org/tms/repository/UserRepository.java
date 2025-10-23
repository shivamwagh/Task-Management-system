package org.tms.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.tms.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
}

