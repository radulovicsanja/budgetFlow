package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.*;
import com.example.budgetFlow.exception.BudgetOverspendException;
import com.example.budgetFlow.repository.BudgetCategoryRepository;
import com.example.budgetFlow.repository.BudgetRepository;
import com.example.budgetFlow.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private CategoryService categoryService;
    @Mock private BudgetCategoryService budgetCategoryService;
    @Mock private BudgetCategoryRepository budgetCategoryRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private User user;
    private Budget budget;
    private Category category;
    private BudgetCategory budgetCategory;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@mail.com").username("test").password("pass").build();
        budget = Budget.builder()
                .id(10L)
                .user(user)
                .month("2026-07")
                .totalAmount(new BigDecimal("1000.00"))
                .additionalIncome(BigDecimal.ZERO)
                .build();
        category = Category.builder().id(5L).name("Hrana").user(user).build();
        budgetCategory = new BudgetCategory();
        budgetCategory.setId(20L);
        budgetCategory.setBudget(budget);
        budgetCategory.setCategory(category);
        budgetCategory.setAllocatedAmount(new BigDecimal("300.00"));
        budgetCategory.setPercentage(new BigDecimal("30.00"));
    }

    @Test
    void income_increasesAdditionalIncome() {
        when(budgetRepository.findByUserIdAndMonth(1L, "2026-07")).thenReturn(Optional.of(budget));

        Transaction income = new Transaction(
                new BigDecimal("50.00"),
                TransactionType.INCOME,
                "Bonus",
                LocalDate.of(2026, 7, 10),
                user,
                category
        );

        budgetService.updateBudgetAfterTransaction(income, false);

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("50.00").compareTo(captor.getValue().getAdditionalIncome()));
    }

    @Test
    void expense_withinRemaining_doesNotChangeAllocation() {
        when(budgetRepository.findByUserIdAndMonth(1L, "2026-07")).thenReturn(Optional.of(budget));
        when(budgetCategoryRepository.findByBudgetIdAndCategoryId(10L, 5L)).thenReturn(budgetCategory);
        when(transactionRepository.findByUserIdAndCategoryId(1L, 5L)).thenReturn(List.of(
                expense(new BigDecimal("280.00"))
        ));

        Transaction tx = new Transaction(
                new BigDecimal("20.00"),
                TransactionType.EXPENSE,
                "Market",
                LocalDate.of(2026, 7, 15),
                user,
                category
        );

        budgetService.updateBudgetAfterTransaction(tx, false);

        verify(budgetCategoryRepository, never()).save(any());
    }

    @Test
    void expense_overCategory_withoutConfirm_throwsConfirmationRequired() {
        when(budgetRepository.findByUserIdAndMonth(1L, "2026-07")).thenReturn(Optional.of(budget));
        when(budgetCategoryRepository.findByBudgetIdAndCategoryId(10L, 5L)).thenReturn(budgetCategory);
        when(transactionRepository.findByUserIdAndCategoryId(1L, 5L)).thenReturn(List.of(
                expense(new BigDecimal("280.00"))
        ));
        when(budgetCategoryRepository.findByBudgetId(10L)).thenReturn(List.of(budgetCategory));

        Transaction tx = new Transaction(
                new BigDecimal("30.00"),
                TransactionType.EXPENSE,
                "Market",
                LocalDate.of(2026, 7, 15),
                user,
                category
        );

        BudgetOverspendException ex = assertThrows(
                BudgetOverspendException.class,
                () -> budgetService.updateBudgetAfterTransaction(tx, false)
        );

        assertTrue(ex.isConfirmationRequired());
        assertEquals(0, new BigDecimal("10.00").compareTo(ex.getShortage()));
    }

    @Test
    void expense_overCategory_withConfirm_takesFromUnallocated() {
        when(budgetRepository.findByUserIdAndMonth(1L, "2026-07")).thenReturn(Optional.of(budget));
        when(budgetCategoryRepository.findByBudgetIdAndCategoryId(10L, 5L)).thenReturn(budgetCategory);
        when(transactionRepository.findByUserIdAndCategoryId(1L, 5L)).thenReturn(List.of(
                expense(new BigDecimal("280.00"))
        ));
        when(budgetCategoryRepository.findByBudgetId(10L)).thenReturn(List.of(budgetCategory));

        Transaction tx = new Transaction(
                new BigDecimal("30.00"),
                TransactionType.EXPENSE,
                "Market",
                LocalDate.of(2026, 7, 15),
                user,
                category
        );

        budgetService.updateBudgetAfterTransaction(tx, true);

        ArgumentCaptor<BudgetCategory> captor = ArgumentCaptor.forClass(BudgetCategory.class);
        verify(budgetCategoryRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("310.00").compareTo(captor.getValue().getAllocatedAmount()));
    }

    private Transaction expense(BigDecimal amount) {
        return new Transaction(amount, TransactionType.EXPENSE, "old", LocalDate.of(2026, 7, 1), user, category);
    }
}
