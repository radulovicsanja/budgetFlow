
package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.TransactionDTO;
import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.entity.Transaction;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.service.CategoryService;
import com.example.budgetFlow.service.TransactionService;
import com.example.budgetFlow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Transactions", description = "Manage income and expense transactions")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;
    private final CategoryService categoryService;

    @Operation(summary = "Create transaction")
    @ApiResponse(responseCode = "200", description = "Transaction created")
    @PostMapping
    public ResponseEntity<Transaction> create(@RequestBody @Valid TransactionDTO dto) {

        User user = userService.getById(dto.getUserId());
        Category category = categoryService.getById(dto.getCategoryId());

        Transaction transaction = new Transaction(
                dto.getAmount(),
                dto.getType(),
                dto.getDescription(),
                dto.getDate(),
                user,
                category
        );

        return ResponseEntity.ok(transactionService.save(transaction));
    }
    @Operation(summary = "Update transaction")
    @ApiResponse(responseCode = "200", description = "Transaction updated")
    @PutMapping("/{id}")
    public Transaction update(@Parameter(description = "Transaction ID") @PathVariable Long id, @RequestBody @Valid TransactionDTO dto) {

        // Dohvati postojeću transakciju
        Transaction existing = transactionService.getById(id);

        // Dohvati User i Category
        User user = userService.getById(dto.getUserId());
        Category category = categoryService.getById(dto.getCategoryId());

        // Postavi nova polja
        existing.setAmount(dto.getAmount());
        existing.setType(dto.getType());
        existing.setDescription(dto.getDescription());
        existing.setDate(dto.getDate());
        existing.setUser(user);
        existing.setCategory(category);

        // Sačuvaj i vrati
        return transactionService.update(existing);
    }

    @Operation(summary = "Delete transaction")
    @ApiResponse(responseCode = "200", description = "Transaction deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@Parameter(description = "Transaction ID") @PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.ok("Deleted");
    }

    @Operation(summary = "Get transaction by ID")
    @ApiResponse(responseCode = "200", description = "Transaction found")
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@Parameter(description = "Transaction ID") @PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getById(id));
    }

    @Operation(summary = "Get transactions", description = "Get transactions for a user, optionally filtered by category or type (INCOME/EXPENSE)")
    @ApiResponse(responseCode = "200", description = "List of transactions")
    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(
            @Parameter(description = "User ID", required = true) @RequestParam Long userId,
            @Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by type (INCOME or EXPENSE)") @RequestParam(required = false) String type
    ) {

        if (categoryId != null) {
            return ResponseEntity.ok(
                    transactionService.getByUserIdAndCategoryId(userId, categoryId)
            );
        }

        if (type != null) {
            return ResponseEntity.ok(
                    transactionService.getByUserIdAndType(userId, type)
            );
        }

        return ResponseEntity.ok(transactionService.getByUserId(userId));
    }
}