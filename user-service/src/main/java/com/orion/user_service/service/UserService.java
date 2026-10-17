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

    /**
     * Fetch all users in the system.
     */
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
        log.debug("Creating user: username={}, email={}", 
                request != null ? request.getUsername() : null, 
                request != null ? request.getEmail() : null);

        if (request == null) {
            throw new IllegalArgumentException("User request body is required");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }

        if (userRepository.existsByUsername(username) || userRepository.existsByUsernameIgnoreCase(username)) {
            log.warn("Username already exists: {}", username);
            throw new IllegalArgumentException("User already exists with username: " + username);
        }
        if (userRepository.existsByEmail(email) || userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Email already exists: {}", email);
            throw new IllegalArgumentException("User already exists with email: " + email);
        }

        User user = User.builder()
                .username(username)
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .status(request.getStatus() != null && !request.getStatus().isBlank()
                        ? request.getStatus().trim()
                        : "ACTIVE")
                .role(request.getRole() != null && !request.getRole().isBlank()
                        ? request.getRole().trim()
                        : "USER")
                .expireAt(request.getExpireAt())
                .unlockAt(request.getUnlockAt())
                .unlockAttempts((short) 0)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: id={}, username={}, email={}", user.getUserId(), user.getUsername(), user.getEmail());
        return this.mapToResponse(user);
    }

    // -------------------------------------------------------------------------
    // UPDATE (partial — allows username or email as lookup identifier)
    // -------------------------------------------------------------------------
    @Transactional
    public UserResponse update(UserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("User request body is required for update");
        }

        String identifier = (request.getUsername() != null && !request.getUsername().isBlank())
                ? request.getUsername().trim()
                : (request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim() : null);

        if (identifier == null) {
            throw new IllegalArgumentException("Username or email is required for update");
        }

        log.debug("Updating user with identifier: {}", identifier);
        User user = findUserByIdentifierOrThrow(identifier);

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                if (userRepository.existsByEmail(newEmail) || userRepository.existsByEmailIgnoreCase(newEmail)) {
                    log.warn("Update failed — email already in use: {}", newEmail);
                    throw new IllegalArgumentException("Email already in use: " + newEmail);
                }
                user.setEmail(newEmail);
            }
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            log.debug("Password updated for user: username={}", user.getUsername());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            user.setStatus(request.getStatus().trim());
        }
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(request.getRole().trim());
        }
        if (request.getExpireAt() != null) {
            user.setExpireAt(request.getExpireAt());
        }
        if (request.getUnlockAt() != null) {
            user.setUnlockAt(request.getUnlockAt());
        }

        user = userRepository.save(user);
        log.info("User updated successfully: username={}, email={}", user.getUsername(), user.getEmail());
        return this.mapToResponse(user);
    }

    // -------------------------------------------------------------------------
    // GET by identifier (username OR email)
    // -------------------------------------------------------------------------
    public UserResponse getByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Username or email is required");
        }
        String trimmed = identifier.trim();
        log.debug("Looking up user by identifier: {}", trimmed);
        User user = findUserByIdentifierOrThrow(trimmed);
        return this.mapToResponse(user);
    }

    public UserResponse getByUsername(String username) {
        return getByIdentifier(username);
    }

    // -------------------------------------------------------------------------
    // DELETE by identifier (username OR email)
    // -------------------------------------------------------------------------
    @Transactional
    public void deleteByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Username or email is required for deletion");
        }
        String trimmed = identifier.trim();
        log.debug("Deleting user by identifier: {}", trimmed);
        User user = findUserByIdentifierOrThrow(trimmed);
        userRepository.delete(user);
        log.info("User deleted successfully: username={}, email={}", user.getUsername(), user.getEmail());
    }

    public void deleteByUsername(String username) {
        deleteByIdentifier(username);
    }

    // -------------------------------------------------------------------------
    // Internal auth lookup — supports either username or email
    // -------------------------------------------------------------------------
    public UserInternalDto getByIdentifierForAuth(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Username or email is required for authentication lookup");
        }
        String trimmed = identifier.trim();
        log.debug("Auth lookup for user: identifier={}", trimmed);
        User user = findUserByIdentifierOrThrow(trimmed);

        return UserInternalDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword())
                .status(user.getStatus())
                .unlockAttempts(user.getUnlockAttempts())
                .unlockAt(user.getUnlockAt())
                .createdAt(user.getCreatedAt())
                .expireAt(user.getExpireAt())
                .role(user.getRole())
                .build();
    }

    public UserInternalDto getByUsernameForAuth(String username) {
        return getByIdentifierForAuth(username);
    }

    private User findUserByIdentifierOrThrow(String identifier) {
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByUsernameIgnoreCase(identifier))
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> {
                    log.warn("User not found with username or email: {}", identifier);
                    return new IllegalArgumentException("User not found with username or email: " + identifier);
                });
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .status(user.getStatus())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .expireAt(user.getExpireAt())
                .build();
    }
}
