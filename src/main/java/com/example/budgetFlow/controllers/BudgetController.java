package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.BudgetCategoryDTO;
import com.example.budgetFlow.DTO.BudgetDTO;
import com.example.budgetFlow.entity.Budget;
import com.example.budgetFlow.entity.BudgetCategory;
import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.service.BudgetService;
import com.example.budgetFlow.service.CategoryService;
import com.example.budgetFlow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Budgets", description = "Budget creation and management")
@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final UserService userService;
    private final CategoryService categoryService;

    @Operation(summary = "Create budget", description = "Create a new monthly budget for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Budget created")
    @PostMapping
    public ResponseEntity<Budget> createBudget(@RequestBody @Valid BudgetDTO dto) {
        User user = userService.getCurrentUser();

        BigDecimal additional = dto.getAdditionalIncome() != null
                ? dto.getAdditionalIncome()
                : BigDecimal.ZERO;

        Budget budget = Budget.builder()
                .user(user)
                .month(dto.getMonth())
                .totalAmount(dto.getTotalAmount())
                .additionalIncome(additional)
                .build();

        return ResponseEntity.ok(budgetService.createBudget(budget));
    }

    @Operation(summary = "Get my budgets", description = "Retrieve all budgets for the authenticated user")
    @ApiResponse(responseCode = "200", description = "List of budgets")
    @GetMapping("/me")
    public ResponseEntity<List<Budget>> getMyBudgets() {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(budgetService.getAllUserBudgets(user.getId()));
    }

    @Operation(summary = "Get budgets by user", description = "Only allowed for the authenticated user's own ID")
    @ApiResponse(responseCode = "200", description = "List of budgets")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Budget>> getBudgetsByUser(@Parameter(description = "User ID") @PathVariable Long userId) {
        userService.assertOwnership(userId);
        return ResponseEntity.ok(budgetService.getAllUserBudgets(userId));
    }

    @Operation(summary = "Get budget by ID")
    @ApiResponse(responseCode = "200", description = "Budget found")
    @ApiResponse(responseCode = "404", description = "Budget not found")
    @GetMapping("/{id}")
    public ResponseEntity<Budget> getBudgetById(@Parameter(description = "Budget ID") @PathVariable Long id) {
        Budget budget = budgetService.getBudgetById(id);
        userService.assertOwnership(budget.getUser().getId());
        return ResponseEntity.ok(budget);
    }

    @Operation(summary = "Update budget")
    @ApiResponse(responseCode = "200", description = "Budget updated")
    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(
            @Parameter(description = "Budget ID") @PathVariable Long id,
            @RequestBody @Valid BudgetDTO dto) {

        Budget existing = budgetService.getBudgetById(id);
        userService.assertOwnership(existing.getUser().getId());

        User user = userService.getCurrentUser();

        // ako additionalIncome nije poslat, zadrži postojeći
        BigDecimal additional = dto.getAdditionalIncome() != null
                ? dto.getAdditionalIncome()
                : existing.getAdditionalIncome();

        Budget updatedBudget = Budget.builder()
                .user(user)
                .month(dto.getMonth())
                .totalAmount(dto.getTotalAmount())
                .additionalIncome(additional != null ? additional : BigDecimal.ZERO)
                .build();

        return ResponseEntity.ok(budgetService.updateBudget(id, updatedBudget));
    }

    @Operation(summary = "Add/update category allocation", description = "Assign a category to a budget with a custom percentage or amount")
    @ApiResponse(responseCode = "200", description = "Category allocation saved")
    @PutMapping("/{id}/categories")
    public ResponseEntity<BudgetCategory> addOrUpdateCategory(
            @Parameter(description = "Budget ID") @PathVariable Long id,
            @RequestBody @Valid BudgetCategoryDTO dto) {

        Budget budget = budgetService.getBudgetById(id);
        userService.assertOwnership(budget.getUser().getId());

        Category category = categoryService.getById(dto.getCategoryId());
        userService.assertOwnership(category.getUser().getId());

        Double percentage = dto.getPercentage() != null ? dto.getPercentage().doubleValue() : null;
        Double amount = dto.getAllocatedAmount() != null ? dto.getAllocatedAmount().doubleValue() : null;

        return ResponseEntity.ok(
                budgetService.addOrUpdateCategoryManual(id, dto.getCategoryId(), percentage, amount)
        );
    }

    @Operation(summary = "Apply 50/30/20 distribution", description = "Automatically allocate budget using the 50% essential / 30% optional / 20% savings rule")
    @ApiResponse(responseCode = "200", description = "Budget categories allocated")
    @PostMapping("/{id}/suggested")
    public ResponseEntity<List<BudgetCategory>> applySuggested(@Parameter(description = "Budget ID") @PathVariable Long id) {
        Budget budget = budgetService.getBudgetById(id);
        userService.assertOwnership(budget.getUser().getId());
        return ResponseEntity.ok(budgetService.applySuggestedBudget(id));
    }
}
