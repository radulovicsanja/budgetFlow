package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Transaction;
import com.example.budgetFlow.repository.TransactionRepository;
import com.example.budgetFlow.service.TransactionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;

    public TransactionServiceImpl(TransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        return repository.save(transaction);
    }

    @Override
    public Transaction update(Transaction transaction) {
        if (transaction.getId() == null) {
            throw new IllegalArgumentException("Transaction ID must not be null for update");
        }
        return repository.save(transaction);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Transaction getById(Long id) {
        Optional<Transaction> transaction = repository.findById(id);
        return transaction.orElse(null);
    }

    @Override
    public List<Transaction> getByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public List<Transaction> getByUserIdAndCategoryId(Long userId, Long categoryId) {
        return repository.findByUserIdAndCategoryId(userId, categoryId);
    }

    @Override
    public List<Transaction> getByUserIdAndType(Long userId, String type) {
        return repository.findByUserIdAndType(userId, type);
    }
}