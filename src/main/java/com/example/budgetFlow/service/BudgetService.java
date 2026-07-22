package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Budget;
import com.example.budgetFlow.entity.BudgetCategory;
import com.example.budgetFlow.entity.Transaction;


import java.util.List;

public interface BudgetService {

    Budget getById(Long id);

    Budget createBudget(Budget budget);

    Budget getBudgetById(Long id);

    Budget getUserBudgetForMonth(Long userId, String month);

    List<Budget> getAllUserBudgets(Long userId);

    Budget updateBudget(Long id, Budget updatedBudget);

    BudgetCategory addOrUpdateCategoryManual(Long budgetId, Long categoryId, Double percentage, Double allocatedAmount);

    List<BudgetCategory> applySuggestedBudget(Long budgetId);

    /** Ažurira budžet poslije transakcije (confirmFromUnallocated za prekoračenje). */
    void updateBudgetAfterTransaction(Transaction transaction, boolean confirmFromUnallocated);

    /** Poništi efekat transakcije na budžet (prije delete/update). */
    void reverseBudgetAfterTransaction(Transaction transaction);
}
