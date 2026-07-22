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
public class MonthTrendDTO {
    private String month;
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal savings;
}
