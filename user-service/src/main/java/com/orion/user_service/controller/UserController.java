package com.orion.user_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.orion.user_service.dto.UserInternalDto;
import com.orion.user_service.service.UserService;

import lombok.extern.slf4j.Slf4j;

/**
 * Main controller for the user-service, mounted at /api/users.
 *
 * Responsibilities:
 *   - Internal endpoints consumed by the API Gateway (user auth lookups).
 *
 * Auth notes:
 *   Internal endpoints are NOT meant for external clients — protect at the
 *   network / gateway level.
 */
@Slf4j
@RestController
@RequestMapping("/api/users/internal/v1/")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * GET /api/users/internal/v1/user/{username}
     * Used by the API Gateway to load user credentials during login/token refresh.
     */
    @GetMapping("/user/{username}")
    @ResponseStatus(HttpStatus.OK)
    public UserInternalDto getByUsernameForAuth(@PathVariable String username) {
        log.debug("Internal auth lookup for user: username={}", username);
        return userService.getByUsernameForAuth(username);
    }
}
