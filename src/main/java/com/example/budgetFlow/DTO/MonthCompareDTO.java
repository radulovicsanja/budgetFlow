package com.example.budgetFlow.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthCompareDTO {
    private String currentMonth;
    private String previousMonth;
    private BigDecimal currentIncome;
    private BigDecimal previousIncome;
    private BigDecimal incomeChangePercent;
    private BigDecimal currentExpenses;
    private BigDecimal previousExpenses;
    private BigDecimal expensesChangePercent;
    private BigDecimal currentSavings;
    private BigDecimal previousSavings;
    private BigDecimal savingsChangePercent;
}
