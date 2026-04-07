package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.BudgetCategoryDTO;
import com.example.budgetFlow.entity.Budget;
import com.example.budgetFlow.entity.BudgetCategory;
import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.service.BudgetCategoryService;
import com.example.budgetFlow.service.BudgetService;
import com.example.budgetFlow.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Budget Categories", description = "Manage category allocations within a budget")
@RestController
@RequestMapping("/api/budget-categories")
@RequiredArgsConstructor
public class BudgetCategoryController {

    private final BudgetCategoryService budgetCategoryService;
    private final BudgetService budgetService;
    private final CategoryService categoryService;

    // CREATE
    @Operation(summary = "Create budget-category allocation")
    @ApiResponse(responseCode = "200", description = "Allocation created")
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
    @Operation(summary = "Get budget-category by ID")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "Not found")
    @GetMapping("/{id}")
    public ResponseEntity<BudgetCategory> getById(@Parameter(description = "BudgetCategory ID") @PathVariable Long id) {
        BudgetCategory bc = budgetCategoryService.getById(id);
        if (bc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(bc);
    }

    // GET BY BUDGET
    @Operation(summary = "Get all category allocations for a budget")
    @ApiResponse(responseCode = "200", description = "List of allocations")
    @GetMapping("/budget/{budgetId}")
    public ResponseEntity<List<BudgetCategory>> getByBudget(@Parameter(description = "Budget ID") @PathVariable Long budgetId) {
        List<BudgetCategory> list = budgetCategoryService.getByBudgetId(budgetId);
        return ResponseEntity.ok(list);
    }

    // UPDATE
    @Operation(summary = "Update budget-category allocation")
    @ApiResponse(responseCode = "200", description = "Allocation updated")
    @ApiResponse(responseCode = "404", description = "Not found")
    @PutMapping("/{id}")
    public ResponseEntity<BudgetCategory> update(@Parameter(description = "BudgetCategory ID") @PathVariable Long id,
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
    @Operation(summary = "Delete budget-category allocation")
    @ApiResponse(responseCode = "200", description = "Deleted successfully")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@Parameter(description = "BudgetCategory ID") @PathVariable Long id) {
        budgetCategoryService.delete(id);
        return ResponseEntity.ok("BudgetCategory deleted successfully");
    }
}