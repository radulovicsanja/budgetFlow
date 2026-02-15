package com.example.budgetFlow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "category_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ime tipa kategorije je obavezno")
    @Size(max = 50, message = "Ime tipa kategorije ne može biti duže od 50 karaktera")
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Size(max = 255, message = "Opis tipa kategorije može biti do 255 karaktera")
    private String description;
}
