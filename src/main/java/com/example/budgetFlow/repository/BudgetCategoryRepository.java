package com.example.budgetFlow.repository;

import com.example.budgetFlow.entity.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, Long> {

    // Pronalaženje svih kategorija po budžetu
    List<BudgetCategory> findByBudgetId(Long budgetId);

    // Opcionalno: pronalaženje po budžetu i kategoriji
    BudgetCategory findByBudgetIdAndCategoryId(Long budgetId, Long categoryId);
}
