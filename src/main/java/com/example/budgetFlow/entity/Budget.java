package com.example.budgetFlow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ne u JSON (lazy + ciklus)
    @NotNull(message = "Korisnik je obavezan")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @NotBlank(message = "Mjesec je obavezan")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Mjesec mora biti u formatu YYYY-MM, npr. 2026-03")
    @Column(nullable = false, length = 7)
    private String month;

    @NotNull(message = "Ukupan budžet je obavezan")
    @DecimalMin(value = "0.0", inclusive = false, message = "Budžet mora biti pozitivan")
    @Digits(integer = 10, fraction = 2, message = "Budžet mora biti decimalni broj sa maksimalno 2 decimale")
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @NotNull(message = "Dodatna zarada ne može biti null")
    @DecimalMin(value = "0.0", message = "Dodatna zarada ne može biti negativna")
    @Digits(integer = 10, fraction = 2, message = "Dodatna zarada mora biti decimalni broj sa maksimalno 2 decimale")
    @Column(name = "additional_income", nullable = false, precision = 10, scale = 2)
    private BigDecimal additionalIncome = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
