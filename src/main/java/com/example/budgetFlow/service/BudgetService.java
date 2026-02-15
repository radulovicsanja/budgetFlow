package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Budget;

import java.util.List;

public interface BudgetService {

    Budget createBudget(Budget budget);

    Budget getBudgetById(Long id);

    Budget getUserBudgetForMonth(Long userId, String month);

    List<Budget> getAllUserBudgets(Long userId);

    Budget updateBudget(Long id, Budget updatedBudget);

    void deleteBudget(Long id);
}
