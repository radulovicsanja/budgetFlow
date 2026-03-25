package com.example.budgetFlow.DTO;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BudgetDTO {

    @NotNull(message = "Korisnik je obavezan")
    private Long userId;

    @NotBlank(message = "Mjesec je obavezan")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Mjesec mora biti u formatu YYYY-MM, npr. 2026-03")
    private String month;

    @NotNull(message = "Ukupan budžet je obavezan")
    @Positive
    @DecimalMin(value = "0.0", inclusive = false, message = "Budžet mora biti pozitivan")
    private BigDecimal totalAmount;

    @NotNull(message = "Dodatna zarada ne može biti null")
    @Positive
    @DecimalMin(value = "0.0", message = "Dodatna zarada ne može biti negativna")
    private BigDecimal additionalIncome = BigDecimal.ZERO;
}