package com.example.budgetFlow.DTO;

import com.example.budgetFlow.entity.TransactionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RecurringTransactionRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private TransactionType type;

    private String description;

    private Long categoryId;

    @NotNull
    @Min(1)
    @Max(28)
    private Integer dayOfMonth;

    private Boolean active = true;
}
