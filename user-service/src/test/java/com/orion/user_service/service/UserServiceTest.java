package com.orion.user_service.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.orion.user_service.dto.UserInternalDto;
import com.orion.user_service.dto.UserRequest;
import com.orion.user_service.dto.UserResponse;
import com.orion.user_service.model.User;
import com.orion.user_service.repository.UserRepository;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(passwordEncoder.encode(any())).thenAnswer(inv -> "encoded_" + inv.getArgument(0));
    }

    @Test
    void testCreateUserSuccess() {
        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(1L);
            u.prePersist();
            return u;
        });

        UserRequest request = UserRequest.builder()
                .username("john_doe")
                .name("John Doe")
                .email("john@example.com")
                .password("secret123")
                .role("ADMIN")
                .expireAt(OffsetDateTime.now().plusYears(1))
                .build();

        UserResponse response = userService.create(request);

        assertNotNull(response);
        assertEquals("john_doe", response.getUsername());
        assertEquals("John Doe", response.getName());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("ADMIN", response.getRole());
        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUserDuplicateUsernameThrows() {
        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        UserRequest request = UserRequest.builder()
                .username("john_doe")
                .name("John Doe")
                .email("john@example.com")
                .password("secret123")
                .build();

        assertThrows(IllegalArgumentException.class, () -> userService.create(request));
    }

    @Test
    void testCreateUserDuplicateEmailThrows() {
        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        UserRequest request = UserRequest.builder()
                .username("john_doe")
                .name("John Doe")
                .email("john@example.com")
                .password("secret123")
                .build();

        assertThrows(IllegalArgumentException.class, () -> userService.create(request));
    }

    @Test
    void testCreateUserMissingRequiredFieldsThrows() {
        UserRequest request1 = UserRequest.builder().name("John").email("j@test.com").password("p").build();
        assertThrows(IllegalArgumentException.class, () -> userService.create(request1));

        UserRequest request2 = UserRequest.builder().username("j").name("John").password("p").build();
        assertThrows(IllegalArgumentException.class, () -> userService.create(request2));
    }

    @Test
    void testUpdateUserSuccess() {
        User existing = User.builder()
                .userId(1L)
                .username("john_doe")
                .name("Old Name")
                .email("old@example.com")
                .password("encoded_old")
                .status("ACTIVE")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRequest request = UserRequest.builder()
                .username("john_doe")
                .name("New Name")
                .email("new@example.com")
                .status("SUSPENDED")
                .build();

        UserResponse response = userService.update(request);

        assertNotNull(response);
        assertEquals("New Name", response.getName());
        assertEquals("new@example.com", response.getEmail());
        assertEquals("SUSPENDED", response.getStatus());
    }

    @Test
    void testGetByUsernameSuccess() {
        User existing = User.builder()
                .userId(1L)
                .username("john_doe")
                .name("John Doe")
                .email("john@example.com")
                .status("ACTIVE")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(existing));

        UserResponse response = userService.getByUsername("john_doe");

        assertNotNull(response);
        assertEquals("john_doe", response.getUsername());
        assertEquals("john@example.com", response.getEmail());
    }

    @Test
    void testGetByUsernameForAuthSuccess() {
        OffsetDateTime now = OffsetDateTime.now();
        User existing = User.builder()
                .userId(1L)
                .username("john_doe")
                .name("John Doe")
                .email("john@example.com")
                .password("encoded_pass")
                .status("ACTIVE")
                .unlockAttempts((short) 0)
                .createdAt(now)
                .role("USER")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(existing));

        UserInternalDto dto = userService.getByUsernameForAuth("john_doe");

        assertNotNull(dto);
        assertEquals("john_doe", dto.getUsername());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals("encoded_pass", dto.getPassword());
        assertEquals("ACTIVE", dto.getStatus());
    }

    @Test
    void testDeleteByUsernameSuccess() {
        User existing = User.builder()
                .userId(1L)
                .username("john_doe")
                .email("john@example.com")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(existing));

        userService.deleteByUsername("john_doe");

        verify(userRepository).delete(existing);
    }

    @Test
    void testGetByIdentifierForAuthWithEmailSuccess() {
        OffsetDateTime now = OffsetDateTime.now();
        User existing = User.builder()
                .userId(2L)
                .username("jane_doe")
                .name("Jane Doe")
                .email("jane@example.com")
                .password("encoded_pass")
                .status("ACTIVE")
                .unlockAttempts((short) 0)
                .createdAt(now)
                .role("USER")
                .build();

        when(userRepository.findByUsername("jane@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("jane@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(existing));

        UserInternalDto dto = userService.getByIdentifierForAuth("jane@example.com");

        assertNotNull(dto);
        assertEquals("jane_doe", dto.getUsername());
        assertEquals("jane@example.com", dto.getEmail());
    }

    @Test
    void testGetByIdentifierForAuthNotFoundThrows() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.getByIdentifierForAuth("unknown"));
    }

    @Test
    void testCreateUserInvalidEmailFormatThrows() {
        UserRequest request = UserRequest.builder()
                .username("validuser")
                .name("Valid User")
                .email("not-an-email")
                .password("pass123")
                .build();

        assertThrows(IllegalArgumentException.class, () -> userService.create(request));
    }
}
