package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Role;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DefaultCategoryService defaultCategoryService;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("sanja")
                .email("sanja@gmail.com")
                .password("password123")
                .build();
    }

    @Test
    void register_validData_createsUser() {

        when(userRepository.existsByEmail(user.getEmail()))
                .thenReturn(false);

        when(userRepository.existsByUsername(user.getUsername()))
                .thenReturn(false);

        when(passwordEncoder.encode(user.getPassword()))
                .thenReturn("encodedPassword");

        User savedUser = User.builder()
                .id(1L)
                .username(user.getUsername())
                .email(user.getEmail())
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        User result = userService.register(user);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("sanja", result.getUsername());
        assertEquals("sanja@gmail.com", result.getEmail());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(Role.USER, result.getRole());

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(defaultCategoryService).seedForUser(savedUser);
    }

    @Test
    void register_existingEmail_throwsCustomException() {

        when(userRepository.existsByEmail(user.getEmail()))
                .thenReturn(true);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> userService.register(user)
        );

        assertEquals("Email already exists.", exception.getMessage());

        verify(userRepository, never()).existsByUsername(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(defaultCategoryService, never()).seedForUser(any(User.class));
    }

    @Test
    void register_existingUsername_throwsCustomException() {

        when(userRepository.existsByEmail(user.getEmail()))
                .thenReturn(false);

        when(userRepository.existsByUsername(user.getUsername()))
                .thenReturn(true);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> userService.register(user)
        );

        assertEquals("Username already exists.", exception.getMessage());

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(defaultCategoryService, never()).seedForUser(any(User.class));
    }

    @Test
    void register_seedCategoriesFails_registrationStillSucceeds() {

        when(userRepository.existsByEmail(user.getEmail()))
                .thenReturn(false);

        when(userRepository.existsByUsername(user.getUsername()))
                .thenReturn(false);

        when(passwordEncoder.encode(user.getPassword()))
                .thenReturn("encodedPassword");

        User savedUser = User.builder()
                .id(1L)
                .username(user.getUsername())
                .email(user.getEmail())
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        doThrow(new RuntimeException("Greška pri kreiranju kategorija"))
                .when(defaultCategoryService)
                .seedForUser(savedUser);

        User result = userService.register(user);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(Role.USER, result.getRole());
        assertEquals("encodedPassword", result.getPassword());

        verify(userRepository).save(any(User.class));
        verify(defaultCategoryService).seedForUser(savedUser);
    }
}