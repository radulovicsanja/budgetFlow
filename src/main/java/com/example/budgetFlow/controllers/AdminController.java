package com.example.budgetFlow.controllers;

import com.example.budgetFlow.entity.Role;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Admin", description = "Admin panel — upravljanje korisnicima")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "List all users")
    @ApiResponse(responseCode = "200", description = "Users returned")
    @GetMapping("/users")
    public ResponseEntity<List<User>> listUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @Operation(summary = "Delete user and related data")
    @ApiResponse(responseCode = "200", description = "User deleted")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@Parameter(description = "User ID") @PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "Korisnik obrisan."));
    }

    @Operation(summary = "Change user role")
    @ApiResponse(responseCode = "200", description = "Role updated")
    @PutMapping("/users/{id}/role")
    public ResponseEntity<User> updateRole(
            @Parameter(description = "User ID") @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        Role role = Role.valueOf(body.get("role").trim().toUpperCase());
        return ResponseEntity.ok(adminService.updateRole(id, role));
    }
}
