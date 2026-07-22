package com.example.budgetFlow.exception;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class BudgetOverspendException extends RuntimeException {

    private final BigDecimal shortage;
    private final BigDecimal unallocated;
    private final BigDecimal categoryRemaining;
    private final boolean confirmationRequired;

    public BudgetOverspendException(
            String message,
            BigDecimal shortage,
            BigDecimal unallocated,
            BigDecimal categoryRemaining,
            boolean confirmationRequired
    ) {
        super(message);
        this.shortage = shortage;
        this.unallocated = unallocated;
        this.categoryRemaining = categoryRemaining;
        this.confirmationRequired = confirmationRequired;
    }
}
