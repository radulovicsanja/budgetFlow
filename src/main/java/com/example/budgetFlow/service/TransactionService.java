package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Transaction;

import java.util.List;

public interface TransactionService {

    Transaction save(Transaction transaction);

    Transaction update(Transaction transaction);

    void delete(Long id);

    Transaction getById(Long id);

    List<Transaction> getByUserId(Long userId);

    List<Transaction> getByUserIdAndCategoryId(Long userId, Long categoryId);

    List<Transaction> getByUserIdAndType(Long userId, String type);


}