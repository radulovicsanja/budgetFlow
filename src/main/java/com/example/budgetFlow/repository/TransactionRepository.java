package com.example.budgetFlow.repository;

import com.example.budgetFlow.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Sve transakcije određenog korisnika
    List<Transaction> findByUserId(Long userId);

    // Sve transakcije po korisniku i kategoriji
    List<Transaction> findByUserIdAndCategoryId(Long userId, Long categoryId);

    // Opcionalno: transakcije po tipu (INCOME / EXPENSE)
    List<Transaction> findByUserIdAndType(Long userId, String type);
}
