package com.example.budgetFlow.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetCategoryDTO {
    @NotNull
    private Long budgetId;

    @NotNull
    private Long categoryId;

    @NotNull
    @Positive
    private BigDecimal percentage;

    @NotNull
    @Positive
    private BigDecimal allocatedAmount;
}