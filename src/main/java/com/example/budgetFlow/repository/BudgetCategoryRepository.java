package com.example.budgetFlow.repository;

import com.example.budgetFlow.entity.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, Long> {

    List<BudgetCategory> findByBudgetId(Long budgetId);

    BudgetCategory findByBudgetIdAndCategoryId(Long budgetId, Long categoryId);

    void deleteByBudget_User_Id(Long userId);

    void deleteByCategory_User_Id(Long userId);
}
