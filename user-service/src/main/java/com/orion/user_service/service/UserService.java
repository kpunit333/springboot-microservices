package com.orion.user_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orion.user_service.dto.UserInternalDto;
import com.orion.user_service.dto.UserRequest;
import com.orion.user_service.dto.UserResponse;
import com.orion.user_service.model.User;
import com.orion.user_service.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<UserResponse> getAll() {
        log.debug("Fetching all users");
        List<User> list = userRepository.findAll();
        log.debug("Found {} users", list.size());
        return list.stream().map(this::mapToResponse).toList();
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------
    @Transactional
    public UserResponse create(UserRequest request) {
        log.debug("Creating user: username={}", request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Username already exists: {}", request.getUsername());
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .status("ACTIVE")
                .role(request.getRole() != null ? request.getRole() : "USER")
                .build();

        user = userRepository.save(user);
        log.info("User saved: id={}, username={}", user.getUserId(), user.getUsername());
        return this.mapToResponse(user);
    }

    // -------------------------------------------------------------------------
    // UPDATE (partial — only non-null fields are changed)
    // -------------------------------------------------------------------------
    @Transactional
    public UserResponse update(UserRequest request) {
        log.debug("Updating user: username={}", request.getUsername());
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Update failed — user not found: username={}", request.getUsername());
                    return new IllegalArgumentException("User not found with username: " + request.getUsername());
                });

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            log.debug("Password updated for user: username={}", request.getUsername());
        }
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(request.getRole());
        }

        user = userRepository.save(user);
        log.info("User updated: username={}", user.getUsername());
        return this.mapToResponse(user);
    }

    // -------------------------------------------------------------------------
    // GET by username
    // -------------------------------------------------------------------------
    public UserResponse getByUsername(String username) {
        log.debug("Looking up user: username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: username={}", username);
                    return new IllegalArgumentException("User not found with username: " + username);
                });
        return this.mapToResponse(user);
    }

    // -------------------------------------------------------------------------
    // DELETE by username
    // -------------------------------------------------------------------------
    @Transactional
    public void deleteByUsername(String username) {
        log.debug("Deleting user: username={}", username);
        userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Delete failed — user not found: username={}", username);
                    return new IllegalArgumentException("User not found with username: " + username);
                });
        userRepository.deleteByUsername(username);
        log.info("User deleted: username={}", username);
    }

    // -------------------------------------------------------------------------
    // Internal — used by the API Gateway for authentication
    // -------------------------------------------------------------------------
    public UserInternalDto getByUsernameForAuth(String username) {
        log.debug("Auth lookup for user: username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Auth lookup failed — user not found: username={}", username);
                    return new IllegalArgumentException("User not found with username: " + username);
                });
        return UserInternalDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .name(user.getName())
                .password(user.getPassword())
                .status(user.getStatus())
                .unlockAttempts(user.getUnlockAttempts())
                .unlockAt(user.getUnlockAt())
                .createdAt(user.getCreatedAt())
                .expireAt(user.getExpireAt())
                .role(user.getRole())
                .build();
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .username(user.getUsername())
                .name(user.getName())
                .build();
    }
}
