package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Transaction;
import com.example.budgetFlow.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    @Override
    public Transaction update(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    @Override
    public void delete(Long id) {
        transactionRepository.deleteById(id);
    }

    @Override
    public Transaction getById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    @Override
    public List<Transaction> getByUserId(Long userId) {
        return transactionRepository.findByUserId(userId);
    }

    @Override
    public List<Transaction> getByUserIdAndCategoryId(Long userId, Long categoryId) {
        return transactionRepository.findByUserIdAndCategoryId(userId, categoryId);
    }

    @Override
    public List<Transaction> getByUserIdAndType(Long userId, String type) {
        return transactionRepository.findByUserIdAndType(userId, type);
    }
}