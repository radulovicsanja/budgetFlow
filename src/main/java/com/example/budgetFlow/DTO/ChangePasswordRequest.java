package com.example.budgetFlow.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "Stara lozinka je obavezna")
    private String oldPassword;

    @NotBlank(message = "Nova lozinka je obavezna")
    @Size(min = 6, max = 255, message = "Nova lozinka mora imati najmanje 6 karaktera")
    private String newPassword;
}
