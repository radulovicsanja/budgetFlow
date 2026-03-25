package com.example.budgetFlow.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryDTO {

    @NotBlank(message = "Naziv kategorije je obavezan")
    @Size(max = 100, message = "Naziv može imati maksimalno 100 karaktera")
    private String name;

    @NotNull(message = "Korisnik je obavezan")
    private Long userId;

    @NotNull(message = "Tip kategorije je obavezan")
    private Long typeId;

    private Boolean isDefault = false;
}