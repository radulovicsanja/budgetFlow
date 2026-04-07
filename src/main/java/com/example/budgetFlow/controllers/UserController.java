package com.example.budgetFlow.controllers;

import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.example.budgetFlow.DTO.LoginRequest;
import org.springframework.security.core.userdetails.UserDetails;
import com.example.budgetFlow.security.JwtUtils;
import java.util.Map;


@Tag(name = "Users", description = "User registration and profile management")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    // Crud
    @Operation(summary = "Register", description = "Register a new user account, returns JWT token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or email already taken")
    })
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody @Valid LoginRequest request) {

        // Ne hashuj ovdje; UserService.register već radi hash.
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

        User savedUser = userService.register(user);

        // Generiši JWT za upravo registrovanog korisnika
        UserDetails userDetails = userService.loadUserByUsername(savedUser.getEmail());
        String token = jwtUtils.generateToken(userDetails);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "email", savedUser.getEmail(),
                "username", savedUser.getUsername()
        ));
    }


    // cRud
    @Operation(summary = "Get current user", description = "Returns the currently authenticated user's profile")
    @ApiResponse(responseCode = "200", description = "User profile returned")
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    // crUd
    @Operation(summary = "Update current user", description = "Update the authenticated user's username, email, or password")
    @ApiResponse(responseCode = "200", description = "User updated successfully")
    @PutMapping("/me")
    public ResponseEntity<User> updateUser(@RequestBody User updatedUser) {

        User currentUser = userService.getCurrentUser();

        currentUser.setUsername(updatedUser.getUsername());
        currentUser.setEmail(updatedUser.getEmail());

        // ako korisnik želi novu lozinku
        if (updatedUser.getPassword() != null &&
                !updatedUser.getPassword().isBlank()) {

            currentUser.setPassword(
                    passwordEncoder.encode(updatedUser.getPassword())
            );
        }

        return ResponseEntity.ok(userService.save(currentUser));
    }

    // cruD
    @Operation(summary = "Delete account", description = "Permanently delete the authenticated user's account")
    @ApiResponse(responseCode = "200", description = "Account deleted successfully")
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteAccount() {

        User currentUser = userService.getCurrentUser();
        userService.delete(currentUser.getId());

        return ResponseEntity.ok("Account deleted successfully.");
    }
}