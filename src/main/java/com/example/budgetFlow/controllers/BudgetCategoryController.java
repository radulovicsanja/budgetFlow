package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.BudgetCategoryDTO;
import com.example.budgetFlow.entity.Budget;
import com.example.budgetFlow.entity.BudgetCategory;
import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.service.BudgetCategoryService;
import com.example.budgetFlow.service.BudgetService;
import com.example.budgetFlow.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/budget-categories")
@RequiredArgsConstructor
public class BudgetCategoryController {

    private final BudgetCategoryService budgetCategoryService;
    private final BudgetService budgetService;
    private final CategoryService categoryService;

    // CREATE
    @PostMapping
    public ResponseEntity<BudgetCategory> create(@RequestBody @Valid BudgetCategoryDTO dto) {

        Budget budget = budgetService.getById(dto.getBudgetId());
        Category category = categoryService.getById(dto.getCategoryId());

        BudgetCategory bc = new BudgetCategory();
        bc.setBudget(budget);
        bc.setCategory(category);
        bc.setPercentage(dto.getPercentage());
        bc.setAllocatedAmount(dto.getAllocatedAmount());

        return ResponseEntity.ok(budgetCategoryService.save(bc));
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<BudgetCategory> getById(@PathVariable Long id) {
        BudgetCategory bc = budgetCategoryService.getById(id);
        if (bc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(bc);
    }

    // GET BY BUDGET
    @GetMapping("/budget/{budgetId}")
    public ResponseEntity<List<BudgetCategory>> getByBudget(@PathVariable Long budgetId) {
        List<BudgetCategory> list = budgetCategoryService.getByBudgetId(budgetId);
        return ResponseEntity.ok(list);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<BudgetCategory> update(@PathVariable Long id,
                                                 @RequestBody @Valid BudgetCategoryDTO dto) {
        BudgetCategory existing = budgetCategoryService.getById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        existing.setBudget(budgetService.getById(dto.getBudgetId()));
        existing.setCategory(categoryService.getById(dto.getCategoryId()));
        existing.setPercentage(dto.getPercentage());
        existing.setAllocatedAmount(dto.getAllocatedAmount());

        return ResponseEntity.ok(budgetCategoryService.update(existing));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        budgetCategoryService.delete(id);
        return ResponseEntity.ok("BudgetCategory deleted successfully");
    }
}