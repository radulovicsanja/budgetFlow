package com.example.budgetFlow.repository;

import com.example.budgetFlow.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    // jedan budžet po korisniku/mjesecu
    Optional<Budget> findByUserIdAndMonth(Long userId, String month);

    List<Budget> findByUserId(Long userId);

    boolean existsByUserIdAndMonth(Long userId, String month);

    void deleteByUserId(Long userId);
}
