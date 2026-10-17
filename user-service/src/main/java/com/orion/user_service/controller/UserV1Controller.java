package com.orion.user_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
     * Create a new user account.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@RequestBody UserRequest request) {
        log.info("Registering new user: username={}, email={}", 
                request != null ? request.getUsername() : null, 
                request != null ? request.getEmail() : null);
        UserResponse response = userService.create(request);
        log.info("User registered successfully: username={}", response.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered successfully", response));
    }

    /**
     * GET /api/users/v1/user
     * List all users.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        log.info("Fetching all users");
        List<UserResponse> list = userService.getAll();
        return ResponseEntity.ok(ApiResponse.ok("Users fetched successfully", list));
    }

    /**
     * GET /api/users/v1/user/{identifier}
     * Fetch a user by either username or email.
     */
    @GetMapping("/{identifier}")
    public ResponseEntity<ApiResponse<UserResponse>> getByIdentifier(@PathVariable String identifier) {
        log.info("Fetching user by identifier: {}", identifier);
        UserResponse response = userService.getByIdentifier(identifier);
        return ResponseEntity.ok(ApiResponse.ok("User fetched successfully", response));
    }

    /**
     * PATCH /api/users/v1/user
     * Partially update an existing user.
     */
    @PatchMapping
    public ResponseEntity<ApiResponse<UserResponse>> updatePatch(@RequestBody UserRequest request) {
        return update(request);
    }

    /**
     * PUT /api/users/v1/user
     * Update an existing user.
     */
    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> update(@RequestBody UserRequest request) {
        log.info("Updating user: identifier={}", request != null ? request.getUsername() : null);
        UserResponse response = userService.update(request);
        log.info("User updated successfully: username={}", response.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", response));
    }

    /**
     * DELETE /api/users/v1/user/{identifier}
     * Delete a user by either username or email.
     */
    @DeleteMapping("/{identifier}")
    public ResponseEntity<ApiResponse<Void>> deleteByIdentifier(@PathVariable String identifier) {
        log.info("Deleting user by identifier: {}", identifier);
        userService.deleteByIdentifier(identifier);
        log.info("User deleted successfully: identifier={}", identifier);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully", null));
    }
}
