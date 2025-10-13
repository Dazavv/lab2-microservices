package com.hs.lab2.userservice.repository;

import com.hs.lab2.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
}
