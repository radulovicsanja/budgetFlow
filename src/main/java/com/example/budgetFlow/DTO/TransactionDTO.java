package com.example.budgetFlow.DTO;

import com.example.budgetFlow.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionDTO {

    @NotNull(message = "Iznos je obavezan")
    @Positive(message = "Iznos mora biti veći od nule")
    private BigDecimal amount;

    @NotNull(message = "Tip transakcije je obavezan")
    private TransactionType type;

    private String description;

    @NotNull(message = "Datum je obavezan")
    private LocalDate date;

    /** Ignoriše se — vlasnik se uzima iz JWT-a. */
    private Long userId;

    /** Obavezno samo za EXPENSE. Za INCOME može biti null. */
    private Long categoryId;

    /** true = uzmi shortage iz neraspoređenog. */
    private Boolean confirmFromUnallocated = false;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Boolean getConfirmFromUnallocated() {
        return confirmFromUnallocated;
    }

    public void setConfirmFromUnallocated(Boolean confirmFromUnallocated) {
        this.confirmFromUnallocated = confirmFromUnallocated;
    }
}
