
package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.TransactionDTO;
import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.entity.Transaction;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.service.CategoryService;
import com.example.budgetFlow.service.TransactionService;
import com.example.budgetFlow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;
    private final CategoryService categoryService;

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
    @PutMapping("/{id}")
    public Transaction update(@PathVariable Long id, @RequestBody @Valid TransactionDTO dto) {

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

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.ok("Deleted");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(
            @RequestParam Long userId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String type
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