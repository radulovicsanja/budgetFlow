package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.SavingsGoalRequest;
import com.example.budgetFlow.entity.SavingsGoal;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.SavingsGoalRepository;
import com.example.budgetFlow.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Savings goals")
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class SavingsGoalController {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        Long userId = userService.getCurrentUser().getId();
        List<Map<String, Object>> body = savingsGoalRepository.findByUserIdOrderByDeadlineAsc(userId)
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody @Valid SavingsGoalRequest request) {
        User user = userService.getCurrentUser();
        SavingsGoal goal = SavingsGoal.builder()
                .user(user)
                .title(request.getTitle().trim())
                .targetAmount(request.getTargetAmount())
                .currentAmount(request.getCurrentAmount() != null ? request.getCurrentAmount() : BigDecimal.ZERO)
                .deadline(request.getDeadline())
                .note(request.getNote())
                .build();
        return ResponseEntity.ok(toDto(savingsGoalRepository.save(goal)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody @Valid SavingsGoalRequest request
    ) {
        SavingsGoal goal = getOwned(id);
        goal.setTitle(request.getTitle().trim());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(request.getCurrentAmount() != null ? request.getCurrentAmount() : BigDecimal.ZERO);
        goal.setDeadline(request.getDeadline());
        goal.setNote(request.getNote());
        return ResponseEntity.ok(toDto(savingsGoalRepository.save(goal)));
    }

    @PostMapping("/{id}/add")
    public ResponseEntity<Map<String, Object>> addAmount(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> body
    ) {
        SavingsGoal goal = getOwned(id);
        BigDecimal amount = body.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("Iznos mora biti pozitivan.");
        }
        BigDecimal current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        goal.setCurrentAmount(current.add(amount));
        return ResponseEntity.ok(toDto(savingsGoalRepository.save(goal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        SavingsGoal goal = getOwned(id);
        savingsGoalRepository.delete(goal);
        return ResponseEntity.ok(Map.of("message", "Cilj obrisan."));
    }

    private SavingsGoal getOwned(Long id) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new CustomException("Cilj nije pronađen."));
        userService.assertOwnership(goal.getUser().getId());
        return goal;
    }

    private Map<String, Object> toDto(SavingsGoal goal) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", goal.getId());
        map.put("title", goal.getTitle());
        map.put("targetAmount", goal.getTargetAmount());
        map.put("currentAmount", goal.getCurrentAmount());
        map.put("deadline", goal.getDeadline());
        map.put("note", goal.getNote());
        map.put("progressPercent", goal.getProgressPercent());
        map.put("completed", goal.isCompleted());
        return map;
    }
}
