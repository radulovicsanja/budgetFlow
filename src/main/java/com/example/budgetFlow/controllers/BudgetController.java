package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.BudgetCategoryDTO;
import com.example.budgetFlow.DTO.BudgetDTO;
import com.example.budgetFlow.entity.Budget;
import com.example.budgetFlow.entity.BudgetCategory;
import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.service.BudgetCategoryService;
import com.example.budgetFlow.service.BudgetService;
import com.example.budgetFlow.service.CategoryService;
import com.example.budgetFlow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final BudgetCategoryService budgetCategoryService;

    // CREATE - kreiraj novi budžet
    @PostMapping
    public ResponseEntity<Budget> createBudget(@RequestBody @Valid BudgetDTO dto) {
        User user = userService.getById(dto.getUserId());

        Budget budget = Budget.builder()
                .user(user)
                .month(dto.getMonth())
                .totalAmount(dto.getTotalAmount())
                .additionalIncome(dto.getAdditionalIncome())
                .build();

        return ResponseEntity.ok(budgetService.createBudget(budget));
    }

    // GET ALL BY USER
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Budget>> getBudgetsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(budgetService.getAllUserBudgets(userId));
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Budget> getBudgetById(@PathVariable Long id) {
        Budget budget = budgetService.getBudgetById(id);
        if (budget == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(budget);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(
            @PathVariable Long id,
            @RequestBody @Valid BudgetDTO dto) {

        User user = userService.getById(dto.getUserId());

        Budget updatedBudget = Budget.builder()
                .user(user)
                .month(dto.getMonth())
                .totalAmount(dto.getTotalAmount())
                .additionalIncome(dto.getAdditionalIncome())
                .build();

        return ResponseEntity.ok(budgetService.updateBudget(id, updatedBudget));
    }



    // CUSTOM (USER DEFINES)
    @PutMapping("/{id}/categories")
    public ResponseEntity<BudgetCategory> addOrUpdateCategory(
            @PathVariable Long id,
            @RequestBody BudgetCategoryDTO dto) {

        Budget budget = budgetService.getById(id);
        if (budget == null) return ResponseEntity.badRequest().build();

        Category category = categoryService.getById(dto.getCategoryId());
        if (category == null) return ResponseEntity.badRequest().build();

        BigDecimal total = budget.getTotalAmount();
        BigDecimal percentage = dto.getPercentage();
        BigDecimal amount = dto.getAllocatedAmount();

        BudgetCategory bc = new BudgetCategory();
        bc.setBudget(budget);
        bc.setCategory(category);
//ako korisnik unese samo procenat
        if (percentage != null && percentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal calculatedAmount = total
                    .multiply(percentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            bc.setPercentage(percentage);
            bc.setAllocatedAmount(calculatedAmount);
//ako korisnik unese samo amount
        } else if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal calculatedPercentage = amount
                    .divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            bc.setAllocatedAmount(amount);
            bc.setPercentage(calculatedPercentage);

        } else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(budgetCategoryService.save(bc));
    }

    // SUGGESTED (50/30/20 AUTOMATSKI)
    @PostMapping("/{id}/suggested")
    public ResponseEntity<List<BudgetCategory>> applySuggested(@PathVariable Long id) {

        Budget budget = budgetService.getById(id);
        if (budget == null) return ResponseEntity.badRequest().build();

        BigDecimal total = budget.getTotalAmount();

        Map<String, BigDecimal> typeDistribution = Map.of(
                "ESSENTIAL", new BigDecimal("50"),
                "OPTIONAL", new BigDecimal("30"),
                "SAVINGS", new BigDecimal("20")
        );

        List<Category> categories = categoryService.getUserCategories(budget.getUser().getId());
        Map<String, List<Category>> grouped = categories.stream()
                .collect(Collectors.groupingBy(c -> c.getType().getName()));

        List<BudgetCategory> saved = new ArrayList<>();

        for (Map.Entry<String, List<Category>> entry : grouped.entrySet()) {

            String type = entry.getKey();
            List<Category> cats = entry.getValue();

            BigDecimal totalPercentage = typeDistribution.getOrDefault(type, BigDecimal.ZERO);
            if (cats.isEmpty() || totalPercentage.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal perCategoryPercentage = totalPercentage
                    .divide(BigDecimal.valueOf(cats.size()), 2, RoundingMode.HALF_UP);

            for (Category c : cats) {
                BigDecimal amount = total
                        .multiply(perCategoryPercentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                BudgetCategory bc = new BudgetCategory();
                bc.setBudget(budget);
                bc.setCategory(c);
                bc.setPercentage(perCategoryPercentage);
                bc.setAllocatedAmount(amount);

                saved.add(budgetCategoryService.save(bc));
            }
        }

        return ResponseEntity.ok(saved);
    }
}