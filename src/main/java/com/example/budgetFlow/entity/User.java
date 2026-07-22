package com.example.budgetFlow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username je obavezan")
    @Size(min = 3, max = 50, message = "Username mora biti između 3 i 50 karaktera")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Email je obavezan")
    @Email(message = "Email mora biti validan")
    @Size(max = 100, message = "Email ne može biti duži od 100 karaktera")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Lozinka je obavezna")
    @Size(min = 6, max = 255, message = "Lozinka mora biti između 6 i 255 karaktera")
    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.role == null) {
            this.role = Role.USER;
        }
    }
}
