package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.BudgetCategory;

import java.util.List;

public interface BudgetCategoryService {

    BudgetCategory save(BudgetCategory budgetCategory);

    BudgetCategory update(BudgetCategory budgetCategory);

    void delete(Long id);

    BudgetCategory getById(Long id);

    List<BudgetCategory> getByBudgetId(Long budgetId);
}
