package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Budget;
import com.example.budgetFlow.entity.BudgetCategory;
import com.example.budgetFlow.entity.Transaction;


import java.util.List;

public interface BudgetService {


        Budget getById(Long id); // za BudgetCategoryContgroller

    Budget createBudget(Budget budget);

    Budget getBudgetById(Long id);

    Budget getUserBudgetForMonth(Long userId, String month);

    List<Budget> getAllUserBudgets(Long userId);


    Budget updateBudget(Long id, Budget updatedBudget);

    // BudgetCategory specifične metode
    BudgetCategory addOrUpdateCategoryManual(Long budgetId, Long categoryId, Double percentage, Double allocatedAmount);
    List<BudgetCategory> applySuggestedBudget(Long budgetId);


    void updateBudgetAfterTransaction(Transaction savedTransaction);
}
