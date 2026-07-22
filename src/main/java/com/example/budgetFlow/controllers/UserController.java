package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.ChangePasswordRequest;
import com.example.budgetFlow.DTO.ForgotPasswordRequest;
import com.example.budgetFlow.DTO.RegisterRequest;
import com.example.budgetFlow.DTO.ResetPasswordRequest;
import com.example.budgetFlow.entity.Role;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.security.JwtUtils;
import com.example.budgetFlow.service.PasswordService;
import com.example.budgetFlow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Users", description = "User registration, profile and password management")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordService passwordService;
    private final JwtUtils jwtUtils;

    @Operation(summary = "Register")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Error")
    })
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody @Valid RegisterRequest request) {
        Map<String, String> body = new LinkedHashMap<>();
        try {
            User user = User.builder()
                    .username(request.getUsername().trim())
                    .email(request.getEmail().trim().toLowerCase())
                    .password(request.getPassword())
                    .role(Role.USER)
                    .build();

            User savedUser = userService.register(user);

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(savedUser.getEmail())
                    .password(savedUser.getPassword())
                    .roles(savedUser.getRole() != null ? savedUser.getRole().name() : "USER")
                    .build();

            String token = jwtUtils.generateToken(userDetails);

            body.put("token", token);
            body.put("email", savedUser.getEmail());
            body.put("username", savedUser.getUsername());
            body.put("role", savedUser.getRole() != null ? savedUser.getRole().name() : "USER");
            return ResponseEntity.ok(body);

        } catch (CustomException ex) {
            body.put("message", "Greška pri registraciji");
            return ResponseEntity.badRequest().body(body);
        } catch (Exception ex) {
            body.put("message", "Greška pri registraciji");
            return ResponseEntity.badRequest().body(body);
        }
    }

    @Operation(summary = "Get current user")
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @Operation(summary = "Update current user")
    @PutMapping("/me")
    public ResponseEntity<User> updateUser(@RequestBody User updatedUser) {
        return ResponseEntity.ok(
                userService.updateProfile(updatedUser.getUsername(), updatedUser.getEmail())
        );
    }

    @Operation(summary = "Change password")
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        passwordService.changePassword(request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Lozinka je uspješno promijenjena."));
    }

    @SecurityRequirements
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordService.forgotPassword(request.getEmail()));
    }

    @SecurityRequirements
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        passwordService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Lozinka je uspješno resetovana. Možete se prijaviti."));
    }

    @DeleteMapping("/me")
    public ResponseEntity<String> deleteAccount() {
        User currentUser = userService.getCurrentUser();
        userService.delete(currentUser.getId());
        return ResponseEntity.ok("Account deleted successfully.");
    }
}
