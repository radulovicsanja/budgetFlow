package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.entity.Transaction;
import com.example.budgetFlow.entity.TransactionType;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User user;
    private Category category;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("sanja")
                .email("sanja@gmail.com")
                .password("password123")
                .build();

        category = Category.builder()
                .id(5L)
                .name("Hrana")
                .user(user)
                .build();

        transaction = new Transaction(
                new BigDecimal("50.00"),
                TransactionType.EXPENSE,
                "Kupovina hrane",
                LocalDate.of(2026, 7, 20),
                user,
                category
        );

        transaction.setId(10L);
    }

    @Test
    void save_validTransaction_updatesBudgetAndSavesTransaction() {

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        Transaction result = transactionService.save(transaction, false);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(new BigDecimal("50.00"), result.getAmount());
        assertEquals(TransactionType.EXPENSE, result.getType());

        verify(budgetService)
                .updateBudgetAfterTransaction(transaction, false);

        verify(transactionRepository)
                .save(transaction);
    }

    @Test
    void update_validTransaction_reversesPreviousAndAppliesUpdatedTransaction() {

        Transaction updatedTransaction = new Transaction(
                new BigDecimal("80.00"),
                TransactionType.EXPENSE,
                "Veća kupovina hrane",
                LocalDate.of(2026, 7, 21),
                user,
                category
        );

        updatedTransaction.setId(10L);

        when(transactionRepository.save(updatedTransaction))
                .thenReturn(updatedTransaction);

        Transaction result = transactionService.update(
                transaction,
                updatedTransaction,
                true
        );

        assertNotNull(result);
        assertEquals(new BigDecimal("80.00"), result.getAmount());
        assertEquals("Veća kupovina hrane", result.getDescription());

        verify(budgetService)
                .reverseBudgetAfterTransaction(transaction);

        verify(budgetService)
                .updateBudgetAfterTransaction(updatedTransaction, true);

        verify(transactionRepository)
                .save(updatedTransaction);
    }

    @Test
    void delete_existingTransaction_reversesBudgetAndDeletesTransaction() {

        transactionService.delete(transaction);

        verify(budgetService)
                .reverseBudgetAfterTransaction(transaction);

        verify(transactionRepository)
                .delete(transaction);
    }

    @Test
    void getById_existingTransaction_returnsTransaction() {

        when(transactionRepository.findById(10L))
                .thenReturn(Optional.of(transaction));

        Transaction result = transactionService.getById(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Kupovina hrane", result.getDescription());

        verify(transactionRepository)
                .findById(10L);
    }

    @Test
    void getById_nonExistingTransaction_throwsCustomException() {

        when(transactionRepository.findById(99L))
                .thenReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> transactionService.getById(99L)
        );

        assertEquals(
                "Transakcija nije pronađena.",
                exception.getMessage()
        );

        verify(transactionRepository)
                .findById(99L);
    }

    @Test
    void getByUserId_returnsUserTransactions() {

        List<Transaction> transactions = List.of(transaction);

        when(transactionRepository.findByUserId(1L))
                .thenReturn(transactions);

        List<Transaction> result = transactionService.getByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(transaction, result.getFirst());

        verify(transactionRepository)
                .findByUserId(1L);
    }

    @Test
    void getByUserIdAndCategoryId_returnsCategoryTransactions() {

        List<Transaction> transactions = List.of(transaction);

        when(transactionRepository.findByUserIdAndCategoryId(1L, 5L))
                .thenReturn(transactions);

        List<Transaction> result =
                transactionService.getByUserIdAndCategoryId(1L, 5L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(category, result.getFirst().getCategory());

        verify(transactionRepository)
                .findByUserIdAndCategoryId(1L, 5L);
    }

    @Test
    void getByUserIdAndType_returnsTransactionsOfSelectedType() {

        List<Transaction> transactions = List.of(transaction);

        when(transactionRepository.findByUserIdAndType(
                1L,
                TransactionType.EXPENSE
        )).thenReturn(transactions);

        List<Transaction> result = transactionService.getByUserIdAndType(
                1L,
                TransactionType.EXPENSE
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TransactionType.EXPENSE, result.getFirst().getType());

        verify(transactionRepository)
                .findByUserIdAndType(1L, TransactionType.EXPENSE);
    }
}