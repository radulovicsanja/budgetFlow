package com.example.budgetFlow.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Username je obavezan")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Email je obavezan")
    @Email
    private String email;

    @NotBlank(message = "Lozinka je obavezna")
    @Size(min = 6, max = 100)
    private String password;
}
