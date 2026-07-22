package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Transaction;
import com.example.budgetFlow.entity.TransactionType;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;

    /** Snima transakciju i ažurira mjesečni budžet. */
    @Override
    @Transactional
    public Transaction save(Transaction transaction, boolean confirmFromUnallocated) {
        budgetService.updateBudgetAfterTransaction(transaction, confirmFromUnallocated);
        return transactionRepository.save(transaction);
    }

    /** Prvo poništi stari efekat na budžet, pa primijeni novi. */
    @Override
    @Transactional
    public Transaction update(Transaction previous, Transaction updated, boolean confirmFromUnallocated) {
        budgetService.reverseBudgetAfterTransaction(previous);
        budgetService.updateBudgetAfterTransaction(updated, confirmFromUnallocated);
        return transactionRepository.save(updated);
    }

    /** Briše transakciju i poništi efekat na budžet. */
    @Override
    @Transactional
    public void delete(Transaction existing) {
        budgetService.reverseBudgetAfterTransaction(existing);
        transactionRepository.delete(existing);
    }

    @Override
    public Transaction getById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new CustomException("Transakcija nije pronađena."));
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
    public List<Transaction> getByUserIdAndType(Long userId, TransactionType type) {
        return transactionRepository.findByUserIdAndType(userId, type);
    }
}
