package com.orion.user_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orion.user_service.dto.ApiResponse;
import com.orion.user_service.dto.UserRequest;
import com.orion.user_service.dto.UserResponse;
import com.orion.user_service.service.UserService;

import lombok.extern.slf4j.Slf4j;

/**
 * V1 CRUD controller for user operations.
 * Mounted at /api/users/v1/user — falls under the /api/users/** gateway route.
 */
@Slf4j
@RestController
@RequestMapping("/api/users/v1/user")
public class UserV1Controller {

    @Autowired
    private UserService userService;

    /**
     * POST /api/users/v1/user
     * Create a new user account. No authentication required.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@RequestBody UserRequest request) {
        log.info("Registering new user: username={}", request.getUsername());
        UserResponse response = userService.create(request);
        log.info("User registered: username={}", response.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered successfully", response));
    }

    /**
     * PATCH /api/users/v1/user
     * Partially update an existing user. Auth required.
     */
    @PatchMapping
    public ResponseEntity<ApiResponse<UserResponse>> update(@RequestBody UserRequest request) {
        log.info("Updating user: username={}", request.getUsername());
        UserResponse response = userService.update(request);
        log.info("User updated: username={}", response.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", response));
    }

    /**
     * GET /api/users/v1/user/{username}
     * Fetch a user by their username. Auth required.
     */
    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<UserResponse>> getByUsername(@PathVariable String username) {
        log.info("Fetching user: username={}", username);
        UserResponse response = userService.getByUsername(username);
        return ResponseEntity.ok(ApiResponse.ok("User fetched successfully", response));
    }

    /**
     * DELETE /api/users/v1/user/{username}
     * Delete a user by their username. Auth required.
     */
    @DeleteMapping("/{username}")
    public ResponseEntity<ApiResponse<Void>> deleteByUsername(@PathVariable String username) {
        log.info("Deleting user: username={}", username);
        userService.deleteByUsername(username);
        log.info("User deleted: username={}", username);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully", null));
    }
}
