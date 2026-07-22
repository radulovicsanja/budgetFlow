package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.TransactionDTO;
import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.entity.Transaction;
import com.example.budgetFlow.entity.TransactionType;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.service.CategoryService;
import com.example.budgetFlow.service.DefaultCategoryService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** API za prihode i troškove. */
@Tag(name = "Transactions", description = "Manage income and expense transactions")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final DefaultCategoryService defaultCategoryService;

    @Operation(
            summary = "Create transaction",
            description = "Creates INCOME/EXPENSE for the authenticated user and updates the monthly budget. " +
                    "INCOME does not require a category. " +
                    "If EXPENSE exceeds category remaining, returns 409 unless confirmFromUnallocated=true."
    )
    @ApiResponse(responseCode = "200", description = "Transaction created")
    @ApiResponse(responseCode = "409", description = "Confirmation required to take from unallocated")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody @Valid TransactionDTO dto) {
        User user = userService.getCurrentUser();
        Category category = resolveCategory(dto, user);

        Transaction transaction = new Transaction(
                dto.getAmount(),
                dto.getType(),
                dto.getDescription(),
                dto.getDate(),
                user,
                category
        );

        boolean confirm = Boolean.TRUE.equals(dto.getConfirmFromUnallocated());
        Transaction saved = transactionService.save(transaction, confirm);
        return ResponseEntity.ok(toResponse(saved));
    }

    @Operation(summary = "Update transaction")
    @ApiResponse(responseCode = "200", description = "Transaction updated")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @Parameter(description = "Transaction ID") @PathVariable Long id,
            @RequestBody @Valid TransactionDTO dto
    ) {
        Transaction existing = transactionService.getById(id);
        userService.assertOwnership(existing.getUser().getId());

        // stara vrijednost za reverse budžeta
        Transaction previous = new Transaction(
                existing.getAmount(),
                existing.getType(),
                existing.getDescription(),
                existing.getDate(),
                existing.getUser(),
                existing.getCategory()
        );
        previous.setId(existing.getId());

        User user = userService.getCurrentUser();
        Category category = resolveCategory(dto, user);

        existing.setAmount(dto.getAmount());
        existing.setType(dto.getType());
        existing.setDescription(dto.getDescription());
        existing.setDate(dto.getDate());
        existing.setUser(user);
        existing.setCategory(category);

        boolean confirm = Boolean.TRUE.equals(dto.getConfirmFromUnallocated());
        Transaction saved = transactionService.update(previous, existing, confirm);
        return ResponseEntity.ok(toResponse(saved));
    }

    @Operation(summary = "Delete transaction")
    @ApiResponse(responseCode = "200", description = "Transaction deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@Parameter(description = "Transaction ID") @PathVariable Long id) {
        Transaction existing = transactionService.getById(id);
        userService.assertOwnership(existing.getUser().getId());
        transactionService.delete(existing);
        return ResponseEntity.ok("Deleted");
    }

    @Operation(summary = "Get transaction by ID")
    @ApiResponse(responseCode = "200", description = "Transaction found")
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@Parameter(description = "Transaction ID") @PathVariable Long id) {
        Transaction transaction = transactionService.getById(id);
        userService.assertOwnership(transaction.getUser().getId());
        return ResponseEntity.ok(transaction);
    }

    @Operation(summary = "Get my transactions", description = "Filter by type, category, date range, amount, search text")
    @ApiResponse(responseCode = "200", description = "List of transactions")
    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String q
    ) {
        Long userId = userService.getCurrentUser().getId();
        List<Transaction> list = transactionService.getByUserId(userId);

        return ResponseEntity.ok(list.stream()
                .filter(t -> type == null || t.getType() == type)
                .filter(t -> categoryId == null
                        || (t.getCategory() != null && categoryId.equals(t.getCategory().getId())))
                .filter(t -> from == null || (t.getDate() != null && !t.getDate().isBefore(from)))
                .filter(t -> to == null || (t.getDate() != null && !t.getDate().isAfter(to)))
                .filter(t -> minAmount == null
                        || (t.getAmount() != null && t.getAmount().compareTo(minAmount) >= 0))
                .filter(t -> maxAmount == null
                        || (t.getAmount() != null && t.getAmount().compareTo(maxAmount) <= 0))
                .filter(t -> {
                    if (q == null || q.isBlank()) return true;
                    String needle = q.trim().toLowerCase();
                    String desc = t.getDescription() != null ? t.getDescription().toLowerCase() : "";
                    String cat = t.getCategory() != null && t.getCategory().getName() != null
                            ? t.getCategory().getName().toLowerCase() : "";
                    return desc.contains(needle) || cat.contains(needle);
                })
                .toList());
    }

    // prihod: sistemska kategorija; trošak: odabrana
    private Category resolveCategory(TransactionDTO dto, User user) {
        if (dto.getType() == TransactionType.INCOME) {
            return defaultCategoryService.getOrCreateIncomeCategory(user);
        }
        if (dto.getCategoryId() == null) {
            throw new CustomException("Za trošak moraš odabrati kategoriju.");
        }
        Category category = categoryService.getById(dto.getCategoryId());
        if (category == null) {
            throw new CustomException("Kategorija nije pronađena.");
        }
        if (category.getUser() == null || category.getUser().getId() == null) {
            throw new CustomException("Kategorija nema vlasnika.");
        }
        userService.assertOwnership(category.getUser().getId());
        return category;
    }

    private Map<String, Object> toResponse(Transaction t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getId());
        map.put("amount", t.getAmount());
        map.put("type", t.getType());
        map.put("description", t.getDescription());
        map.put("date", t.getDate());
        map.put("categoryId", t.getCategory() != null ? t.getCategory().getId() : null);
        map.put("categoryName", t.getCategory() != null ? t.getCategory().getName() : null);
        return map;
    }
}
