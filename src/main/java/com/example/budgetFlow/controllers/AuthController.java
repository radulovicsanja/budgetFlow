package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.LoginRequest;
import com.example.budgetFlow.security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Auth", description = "Authentication endpoints")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Operation(summary = "Ping — provjera da li backend radi")
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Backend radi",
                "admin", "admin@budgetflow.com / admin123"
        ));
    }

    @Operation(summary = "Login")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody(required = false) LoginRequest request) {
        Map<String, String> body = new LinkedHashMap<>();

        try {
            if (request == null
                    || request.getEmail() == null || request.getEmail().isBlank()
                    || request.getPassword() == null || request.getPassword().isBlank()) {
                body.put("message", "Pogrešan email ili lozinka.");
                return ResponseEntity.status(401).body(body);
            }

            String email = request.getEmail().trim().toLowerCase();

            List<Map<String, Object>> rows;
            try {
                rows = jdbcTemplate.queryForList(
                        """
                        SELECT id, username, email, password, role
                        FROM users
                        WHERE lower(email) = ?
                        """,
                        email
                );
            } catch (Exception sqlEx) {
                // fallback ako kolona role još ne postoji
                rows = jdbcTemplate.queryForList(
                        """
                        SELECT id, username, email, password
                        FROM users
                        WHERE lower(email) = ?
                        """,
                        email
                );
            }

            if (rows.isEmpty()) {
                body.put("message", "Pogrešan email ili lozinka.");
                return ResponseEntity.status(401).body(body);
            }

            Map<String, Object> row = rows.get(0);
            String hash = String.valueOf(row.get("password"));
            if (!passwordEncoder.matches(request.getPassword(), hash)) {
                body.put("message", "Pogrešan email ili lozinka.");
                return ResponseEntity.status(401).body(body);
            }

            String username = row.get("username") != null
                    ? String.valueOf(row.get("username"))
                    : email;
            Object roleObj = row.get("role");
            String role = roleObj != null && !String.valueOf(roleObj).isBlank()
                    ? String.valueOf(roleObj).replace("ROLE_", "")
                    : "USER";

            UserDetails details = User.builder()
                    .username(email)
                    .password(hash)
                    .roles(role)
                    .build();

            String token = jwtUtils.generateToken(details);

            body.put("token", token);
            body.put("email", email);
            body.put("username", username);
            body.put("role", role);
            return ResponseEntity.ok(body);

        } catch (Exception ex) {
            System.err.println("LOGIN ERROR: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            ex.printStackTrace();
            body.put("message", "Greška na serveru: " + ex.getMessage());
            return ResponseEntity.status(500).body(body);
        }
    }
}
