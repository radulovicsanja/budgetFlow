package com.example.budgetFlow.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UserReportDTO {
@NotNull
    private Long userId;

    @NotNull
    @Positive
    private BigDecimal totalIncome;

    @NotNull
    @Positive
    private BigDecimal totalExpenses;

    @NotNull
    @Positive
    private BigDecimal totalSavings;

    // za kategorije
    private String categoryName;
    @NotNull
    @Positive
    private BigDecimal amount;

    // JSON breakdown
    private String categoryBreakdown;
}