package com.example.budgetFlow.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class SavingsGoalRequest {

    @NotBlank(message = "Naslov je obavezan")
    private String title;

    @NotNull(message = "Ciljni iznos je obavezan")
    @DecimalMin(value = "0.01", message = "Ciljni iznos mora biti veći od nule")
    private BigDecimal targetAmount;

    @DecimalMin(value = "0.0", message = "Trenutni iznos ne može biti negativan")
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @NotNull(message = "Rok je obavezan")
    private LocalDate deadline;

    private String note;
}
