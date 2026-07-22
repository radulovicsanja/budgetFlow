package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.RecurringTransactionRequest;
import com.example.budgetFlow.service.RecurringTransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Recurring transactions")
@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(recurringTransactionService.listMine());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody @Valid RecurringTransactionRequest request) {
        return ResponseEntity.ok(recurringTransactionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody @Valid RecurringTransactionRequest request
    ) {
        return ResponseEntity.ok(recurringTransactionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        recurringTransactionService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Obrisano."));
    }
}
