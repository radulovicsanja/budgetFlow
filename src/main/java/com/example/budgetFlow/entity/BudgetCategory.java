package com.example.budgetFlow.entity;

import jakarta.persistence.*;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Entity
@Setter
@Builder
@Table(name = "budget_category",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"budget_id", "category_id"})})
public class BudgetCategory {

    // Getters & Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "allocated_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal allocatedAmount;

    // Constructors
    public BudgetCategory() {}

    public BudgetCategory(Long id, Budget budget, Category category, BigDecimal percentage, BigDecimal allocatedAmount) {
        this.id = id;
        this.budget = budget;
        this.category = category;
        this.percentage = percentage;
        this.allocatedAmount = allocatedAmount;
    }

    public void setId(Long id) { this.id = id; }

    public void setBudget(Budget budget) { this.budget = budget; }

    public void setCategory(Category category) { this.category = category; }

    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }

    public void setAllocatedAmount(BigDecimal allocatedAmount) { this.allocatedAmount = allocatedAmount; }
}
