package com.orion.api_gateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.orion.api_gateway.dto.request.IndividualLoginRequest;
import com.orion.api_gateway.dto.request.OrganizationLoginRequest;
import com.orion.api_gateway.dto.response.AuthResponse;
import com.orion.api_gateway.exception.GlobalExceptionHandler;
import com.orion.api_gateway.exception.InvalidCredentialsException;
import com.orion.api_gateway.service.AuthService;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/auth/login - Method not allowed (405) for individual login")
    void getLogin_individual_methodNotAllowed() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request method 'GET' is not supported for individual login. Please use POST."));
    }

    @Test
    @DisplayName("PUT /api/auth/login - Method not allowed (405) for individual login")
    void putLogin_individual_methodNotAllowed() throws Exception {
        mockMvc.perform(put("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"user1\",\"password\":\"pass\"}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request method 'PUT' is not supported for individual login. Please use POST."));
    }

    @Test
    @DisplayName("GET /api/auth/o/login - Method not allowed (405) for organization login")
    void getLogin_organization_methodNotAllowed() throws Exception {
        mockMvc.perform(get("/api/auth/o/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request method 'GET' is not supported for organization login. Please use POST."));
    }

    @Test
    @DisplayName("DELETE /api/auth/o/login - Method not allowed (405) for organization login")
    void deleteLogin_organization_methodNotAllowed() throws Exception {
        mockMvc.perform(delete("/api/auth/o/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request method 'DELETE' is not supported for organization login. Please use POST."));
    }

    @Test
    @DisplayName("POST /api/auth/login with empty body - Bad request (400) for individual login")
    void postLogin_individual_missingBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request body is required for individual login. Please provide valid JSON with 'username' and 'password'."));
    }

    @Test
    @DisplayName("POST /api/auth/o/login with empty body - Bad request (400) for organization login")
    void postLogin_organization_missingBody() throws Exception {
        mockMvc.perform(post("/api/auth/o/login")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request body is required for organization login. Please provide valid JSON with 'code' and 'password'."));
    }

    @Test
    @DisplayName("POST /api/auth/login with invalid credentials - Unauthorized (401)")
    void postLogin_individual_invalidCredentials() throws Exception {
        when(authService.loginIndividual(any(IndividualLoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"user1\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("POST /api/auth/o/login with invalid credentials - Unauthorized (401)")
    void postLogin_organization_invalidCredentials() throws Exception {
        when(authService.loginOrganization(any(OrganizationLoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid organization code or password"));

        mockMvc.perform(post("/api/auth/o/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"ORG1\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid organization code or password"));
    }

    @Test
    @DisplayName("POST /api/auth/login with valid credentials - OK (200)")
    void postLogin_individual_success() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .build();

        when(authService.loginIndividual(any(IndividualLoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"user1\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("POST /api/auth/o/login with valid credentials - OK (200)")
    void postLogin_organization_success() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("org-access-token")
                .refreshToken("org-refresh-token")
                .tokenType("Bearer")
                .build();

        when(authService.loginOrganization(any(OrganizationLoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/o/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"ORG1\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Organization login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("org-access-token"));
    }
}
