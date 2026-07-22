package com.example.budgetFlow.repository;

import com.example.budgetFlow.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByUserIdOrderByDeadlineAsc(Long userId);

    void deleteByUserId(Long userId);
}
