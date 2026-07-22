package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Transaction;
import com.example.budgetFlow.entity.TransactionType;

import java.util.List;

public interface TransactionService {

    Transaction save(Transaction transaction, boolean confirmFromUnallocated);

    /** Update: reverse stare, pa primijeni novu na budžet. */
    Transaction update(Transaction previous, Transaction updated, boolean confirmFromUnallocated);

    void delete(Transaction existing);

    Transaction getById(Long id);

    List<Transaction> getByUserId(Long userId);

    List<Transaction> getByUserIdAndCategoryId(Long userId, Long categoryId);

    List<Transaction> getByUserIdAndType(Long userId, TransactionType type);
}
