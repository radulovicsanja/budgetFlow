package com.example.budgetFlow.controllers;

import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.example.budgetFlow.DTO.LoginRequest;
import org.springframework.security.core.userdetails.UserDetails;
import com.example.budgetFlow.security.JwtUtils;
import java.util.Map;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    // Crud
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
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    // crUd
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
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteAccount() {

        User currentUser = userService.getCurrentUser();
        userService.delete(currentUser.getId());

        return ResponseEntity.ok("Account deleted successfully.");
    }
}