package com.example.budgetFlow.repository;

import com.example.budgetFlow.entity.Transaction;
import com.example.budgetFlow.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserId(Long userId);

    List<Transaction> findByUserIdAndCategoryId(Long userId, Long categoryId);

    List<Transaction> findByUserIdAndType(Long userId, TransactionType type);

    void deleteByUserId(Long userId);
}
