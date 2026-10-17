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
 * Main controller for the user-service internal endpoints consumed by the API Gateway.
 * Supports authentication lookup via either username or email address.
 */
@Slf4j
@RestController
@RequestMapping("/api/users/internal/v1")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * GET /api/users/internal/v1/user/{identifier}
     * Used by the API Gateway to load user credentials by username OR email during login.
     */
    @GetMapping({"/user/{identifier}", "/user/identifier/{identifier}"})
    @ResponseStatus(HttpStatus.OK)
    public UserInternalDto getByIdentifierForAuth(@PathVariable String identifier) {
        log.debug("Internal auth lookup for user identifier: {}", identifier);
        return userService.getByIdentifierForAuth(identifier);
    }
}
