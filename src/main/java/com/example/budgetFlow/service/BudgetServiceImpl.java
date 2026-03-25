package com.example.budgetFlow.service;
import com.example.budgetFlow.entity.*;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;
    private final BudgetCategoryService budgetCategoryService;



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
    public void updateBudgetAfterTransaction(Transaction savedTransaction) {

    }


    @Override
    public Budget getById(Long id) {
        Optional<Budget> budget = budgetRepository.findById(id);
        return budget.orElse(null);
    }

    // ✨ RUČNO UNOSIMO KATEGORIJU
    @Override
    public BudgetCategory addOrUpdateCategoryManual(Long budgetId, Long categoryId, Double percentage, Double allocatedAmount) {

        Budget budget = getById(budgetId);
        if (budget == null) throw new CustomException("Budžet nije pronađen");

        Category category = categoryService.getById(categoryId);
        if (category == null) throw new CustomException("Kategorija nije pronađena");

        BigDecimal totalBudget = budget.getTotalAmount();
        BigDecimal perc = percentage != null ? BigDecimal.valueOf(percentage) : BigDecimal.ZERO;
        BigDecimal amount = allocatedAmount != null ? BigDecimal.valueOf(allocatedAmount) : BigDecimal.ZERO;

        BudgetCategory bc = new BudgetCategory();
        bc.setBudget(budget);
        bc.setCategory(category);

        if (perc.compareTo(BigDecimal.ZERO) > 0) {
            // izračunaj amount iz procenta
            BigDecimal calcAmount = totalBudget.multiply(perc).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            bc.setPercentage(perc);
            bc.setAllocatedAmount(calcAmount);
        } else if (amount.compareTo(BigDecimal.ZERO) > 0) {
            // izračunaj procenat iz amount
            BigDecimal calcPerc = amount.divide(totalBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            bc.setAllocatedAmount(amount);
            bc.setPercentage(calcPerc);
        } else {
            throw new CustomException("Morate unijeti ili procenat ili iznos");
        }

        return budgetCategoryService.save(bc);
    }

    //AUTOMATSKA SUGGESTED BUDGET 50/30/20
    @Override
    public List<BudgetCategory> applySuggestedBudget(Long budgetId) {

        Budget budget = getById(budgetId);
        BigDecimal total = budget.getTotalAmount();

        Map<String, BigDecimal> distribution = Map.of(
                "ESSENTIAL", BigDecimal.valueOf(50),
                "OPTIONAL", BigDecimal.valueOf(30),
                "SAVINGS", BigDecimal.valueOf(20)
        );

        List<Category> categories = categoryService.getUserCategories(budget.getUser().getId());
        Map<String, List<Category>> grouped = categories.stream()
                .collect(Collectors.groupingBy(c -> c.getType().getName()));

        List<BudgetCategory> saved = new ArrayList<>();

        for (Map.Entry<String, List<Category>> entry : grouped.entrySet()) {
            String type = entry.getKey();
            List<Category> cats = entry.getValue();

            BigDecimal totalPerc = distribution.getOrDefault(type, BigDecimal.ZERO);
            if (cats.isEmpty() || totalPerc.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal perCategoryPerc = totalPerc.divide(BigDecimal.valueOf(cats.size()), 2, RoundingMode.HALF_UP);

            for (Category c : cats) {
                BigDecimal amount = total.multiply(perCategoryPerc).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                BudgetCategory bc = new BudgetCategory();
                bc.setBudget(budget);
                bc.setCategory(c);
                bc.setPercentage(perCategoryPerc);
                bc.setAllocatedAmount(amount);

                saved.add(budgetCategoryService.save(bc));
            }
        }

        return saved;
    }
}