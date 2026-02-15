package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.BudgetCategory;
import com.example.budgetFlow.repository.BudgetCategoryRepository;
import com.example.budgetFlow.service.BudgetCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BudgetCategoryServiceImpl implements BudgetCategoryService {

    private final BudgetCategoryRepository repository;

    public BudgetCategoryServiceImpl(BudgetCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public BudgetCategory save(BudgetCategory budgetCategory) {
        return repository.save(budgetCategory);
    }

    @Override
    public BudgetCategory update(BudgetCategory budgetCategory) {
        if (budgetCategory.getId() == null) {
            throw new IllegalArgumentException("BudgetCategory ID must not be null for update");
        }
        return repository.save(budgetCategory);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public BudgetCategory getById(Long id) {
        Optional<BudgetCategory> bc = repository.findById(id);
        return bc.orElse(null);
    }

    @Override
    public List<BudgetCategory> getByBudgetId(Long budgetId) {
        return repository.findByBudgetId(budgetId);
    }
}
