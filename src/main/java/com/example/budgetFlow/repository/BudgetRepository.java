package com.example.budgetFlow.repository;

import com.example.budgetFlow.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    // Jedan budžet po korisniku za određeni mjesec
    Optional<Budget> findByUserIdAndMonth(Long userId, String month);

    // Svi budžeti korisnika
    List<Budget> findByUserId(Long userId);

    // Provjera da li već postoji budžet za mjesec
    boolean existsByUserIdAndMonth(Long userId, String month);

}
