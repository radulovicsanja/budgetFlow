package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Budget;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.BudgetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;

    public BudgetServiceImpl(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Override
    public Budget createBudget(Budget budget) {

        // jedan budžet po mjesecu
        if (budgetRepository.existsByUserIdAndMonth(
                budget.getUser().getId(),
                budget.getMonth())) {

            throw new CustomException(
                    "Budžet za ovaj mjesec već postoji."
            );
        }

        return budgetRepository.save(budget);
    }

    @Override
    public Budget getBudgetById(Long id) {
        return budgetRepository.findById(id)
                .orElseThrow(() ->
                        new CustomException("Budžet nije pronađen."));
    }

    @Override
    public Budget getUserBudgetForMonth(Long userId, String month) {
        return budgetRepository.findByUserIdAndMonth(userId, month)
                .orElseThrow(() ->
                        new CustomException("Budžet za mjesec ne postoji."));
    }

    @Override
    public List<Budget> getAllUserBudgets(Long userId) {
        return budgetRepository.findByUserId(userId);
    }

    @Override
    public Budget updateBudget(Long id, Budget updatedBudget) {

        Budget existing = getBudgetById(id);

        existing.setTotalAmount(updatedBudget.getTotalAmount());
        existing.setAdditionalIncome(updatedBudget.getAdditionalIncome());
        existing.setMonth(updatedBudget.getMonth());

        return budgetRepository.save(existing);
    }

    @Override
    public void deleteBudget(Long id) {
        budgetRepository.deleteById(id);
    }
}