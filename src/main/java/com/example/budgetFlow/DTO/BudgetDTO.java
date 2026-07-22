package com.example.budgetFlow.DTO;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BudgetDTO {

    /** Ignoriše se — vlasnik se uzima iz JWT-a. */
    private Long userId;

    @NotBlank(message = "Mjesec je obavezan")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Mjesec mora biti u formatu YYYY-MM, npr. 2026-03")
    private String month;

    @NotNull(message = "Ukupan iznos je obavezan")
    @Positive(message = "Ukupan iznos mora biti veći od nule")
    @DecimalMin(value = "0.0", inclusive = false, message = "Ukupan iznos mora biti pozitivan")
    private BigDecimal totalAmount;

    @NotNull(message = "Dodatni prihod mora biti unesen (može 0)")
    @DecimalMin(value = "0.0", inclusive = true, message = "Dodatni prihod ne može biti negativan")
    private BigDecimal additionalIncome = BigDecimal.ZERO;
}