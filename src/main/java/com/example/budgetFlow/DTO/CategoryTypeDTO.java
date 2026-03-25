package com.example.budgetFlow.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.FileStore;

@Getter
@Setter
public class CategoryTypeDTO {

    @NotBlank(message = "Ime tipa kategorije je obavezno")
    @Size(max = 50, message = "Ime tipa kategorije ne može biti duže od 50 karaktera")
    private String name;

    @Size(max = 255, message = "Opis tipa kategorije može biti do 255 karaktera")
    private String description;

}