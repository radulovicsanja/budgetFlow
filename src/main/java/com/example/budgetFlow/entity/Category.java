package com.example.budgetFlow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Naziv kategorije je obavezan")
    @Size(max = 100, message = "Naziv može imati maksimalno 100 karaktera")
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * VEZA NA USERA
     * Jedan user -> više kategorija
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * VEZA NA CATEGORY TYPE
     * ESSENTIAL / OPTIONAL / SAVINGS
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private CategoryType type;

    /**
     * Sistemskie kategorije (Hrana, Stanarina...)
     * ne mogu se obrisati
     */
    @Column(name = "is_default")
    private Boolean isDefault = false;
}
