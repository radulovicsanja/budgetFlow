package com.example.budgetFlow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "user_report")
public class User_report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate reportDate;

    @Column(nullable = false)
    private BigDecimal totalIncome;

    @Column(nullable = false)
    private BigDecimal totalExpenses;

    @Column(nullable = false)
    private BigDecimal totalSavings;

    // Opcionalno: možemo sačuvati kategorije i njihove iznose kao JSON ili drugu tabelu
    @Lob
    private String categoryBreakdown; // npr JSON string {"Essentials": 500, "Optional": 200, "Savings": 300}

    // Constructors
    public User_report() {}

    public User_report(Long userId, LocalDate reportDate, BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal totalSavings, String categoryBreakdown) {
        this.userId = userId;
        this.reportDate = reportDate;
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.totalSavings = totalSavings;
        this.categoryBreakdown = categoryBreakdown;
    }

    // Getteri i Setteri
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public BigDecimal getTotalIncome() { return totalIncome; }
    public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }

    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }

    public BigDecimal getTotalSavings() { return totalSavings; }
    public void setTotalSavings(BigDecimal totalSavings) { this.totalSavings = totalSavings; }

    public String getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(String categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }

}