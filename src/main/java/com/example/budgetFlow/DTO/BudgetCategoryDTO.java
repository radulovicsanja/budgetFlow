package com.example.budgetFlow.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetCategoryDTO {
    private Long budgetId;

    @NotNull(message = "Kategorija je obavezna")
    private Long categoryId;

    /** Procenat ili allocatedAmount (ne mora oba). */
    private BigDecimal percentage;

    private BigDecimal allocatedAmount;
}
