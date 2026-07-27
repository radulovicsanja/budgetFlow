package com.example.budgetFlow.service;

import com.example.budgetFlow.DTO.RecurringTransactionRequest;
import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.entity.RecurringTransaction;
import com.example.budgetFlow.entity.TransactionType;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.RecurringTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest {

    @Mock
    private RecurringTransactionRepository repository;

    @Mock
    private UserService userService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private DefaultCategoryService defaultCategoryService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    private User user;
    private Category expenseCategory;
    private Category incomeCategory;
    private RecurringTransaction recurringTransaction;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .id(1L)
                .username("sanja")
                .email("sanja@mail.com")
                .password("password")
                .build();

        expenseCategory = Category.builder()
                .id(10L)
                .name("Hrana")
                .user(user)
                .isDefault(false)
                .build();

        incomeCategory = Category.builder()
                .id(11L)
                .name("Prihod")
                .user(user)
                .isDefault(true)
                .build();

        recurringTransaction = RecurringTransaction.builder()
                .id(20L)
                .user(user)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.EXPENSE)
                .description("Mjesečna kupovina")
                .category(expenseCategory)
                .dayOfMonth(15)
                .active(true)
                .nextRunDate(LocalDate.now().plusMonths(1).withDayOfMonth(15))
                .build();
    }

    @Test
    void listMine_returnsCurrentUsersRecurringTransactions() {

        when(userService.getCurrentUser()).thenReturn(user);

        when(repository.findByUserIdOrderByNextRunDateAsc(1L))
                .thenReturn(List.of(recurringTransaction));

        List<Map<String, Object>> result =
                recurringTransactionService.listMine();

        assertEquals(1, result.size());

        Map<String, Object> item = result.get(0);

        assertEquals(20L, item.get("id"));
        assertEquals(new BigDecimal("100.00"), item.get("amount"));
        assertEquals(TransactionType.EXPENSE, item.get("type"));
        assertEquals("Mjesečna kupovina", item.get("description"));
        assertEquals(10L, item.get("categoryId"));
        assertEquals("Hrana", item.get("categoryName"));
        assertEquals(15, item.get("dayOfMonth"));
        assertEquals(true, item.get("active"));

        verify(repository)
                .findByUserIdOrderByNextRunDateAsc(1L);
    }

    @Test
    void create_expenseWithCategory_savesRecurringTransaction() {

        RecurringTransactionRequest request = expenseRequest();

        when(userService.getCurrentUser()).thenReturn(user);

        when(categoryService.getById(10L))
                .thenReturn(expenseCategory);

        when(repository.save(any(RecurringTransaction.class)))
                .thenAnswer(invocation -> {
                    RecurringTransaction saved =
                            invocation.getArgument(0);
                    saved.setId(20L);
                    return saved;
                });

        Map<String, Object> result =
                recurringTransactionService.create(request);

        assertEquals(20L, result.get("id"));
        assertEquals(new BigDecimal("100.00"), result.get("amount"));
        assertEquals(TransactionType.EXPENSE, result.get("type"));
        assertEquals(10L, result.get("categoryId"));
        assertEquals("Hrana", result.get("categoryName"));
        assertEquals(15, result.get("dayOfMonth"));
        assertEquals(true, result.get("active"));

        verify(categoryService).getById(10L);
        verify(userService).assertOwnership(1L);
        verify(repository).save(any(RecurringTransaction.class));
    }

    @Test
    void create_expenseWithoutCategory_throwsCustomException() {

        RecurringTransactionRequest request = expenseRequest();
        request.setCategoryId(null);

        when(userService.getCurrentUser()).thenReturn(user);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> recurringTransactionService.create(request)
        );

        assertEquals(
                "Za trošak moraš odabrati kategoriju.",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(RecurringTransaction.class));
    }

    @Test
    void create_income_usesDefaultIncomeCategory() {

        RecurringTransactionRequest request =
                new RecurringTransactionRequest();

        request.setAmount(new BigDecimal("1200.00"));
        request.setType(TransactionType.INCOME);
        request.setDescription("Plata");
        request.setCategoryId(null);
        request.setDayOfMonth(1);
        request.setActive(true);

        when(userService.getCurrentUser()).thenReturn(user);

        when(defaultCategoryService.getOrCreateIncomeCategory(user))
                .thenReturn(incomeCategory);

        when(repository.save(any(RecurringTransaction.class)))
                .thenAnswer(invocation -> {
                    RecurringTransaction saved =
                            invocation.getArgument(0);
                    saved.setId(21L);
                    return saved;
                });

        Map<String, Object> result =
                recurringTransactionService.create(request);

        assertEquals(21L, result.get("id"));
        assertEquals(new BigDecimal("1200.00"), result.get("amount"));
        assertEquals(TransactionType.INCOME, result.get("type"));
        assertEquals(11L, result.get("categoryId"));
        assertEquals("Prihod", result.get("categoryName"));

        verify(defaultCategoryService)
                .getOrCreateIncomeCategory(user);

        verifyNoInteractions(categoryService);
    }

    @Test
    void update_existingRecurringTransaction_updatesFields() {

        RecurringTransactionRequest request =
                new RecurringTransactionRequest();

        request.setAmount(new BigDecimal("150.00"));
        request.setType(TransactionType.EXPENSE);
        request.setDescription("Ažurirana kupovina");
        request.setCategoryId(10L);
        request.setDayOfMonth(20);
        request.setActive(false);

        when(repository.findById(20L))
                .thenReturn(Optional.of(recurringTransaction));

        when(userService.getCurrentUser()).thenReturn(user);

        when(categoryService.getById(10L))
                .thenReturn(expenseCategory);

        when(repository.save(recurringTransaction))
                .thenReturn(recurringTransaction);

        Map<String, Object> result =
                recurringTransactionService.update(20L, request);

        assertEquals(new BigDecimal("150.00"), result.get("amount"));
        assertEquals("Ažurirana kupovina", result.get("description"));
        assertEquals(20, result.get("dayOfMonth"));
        assertEquals(false, result.get("active"));

        verify(userService, times(2))
                .assertOwnership(1L);

        verify(repository).save(recurringTransaction);
    }

    @Test
    void update_nonExistingRecurringTransaction_throwsCustomException() {

        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> recurringTransactionService.update(
                        99L,
                        expenseRequest()
                )
        );

        assertEquals(
                "Ponavljajuća transakcija nije pronađena.",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(RecurringTransaction.class));
    }

    @Test
    void delete_existingRecurringTransaction_deletesTransaction() {

        when(repository.findById(20L))
                .thenReturn(Optional.of(recurringTransaction));

        recurringTransactionService.delete(20L);

        verify(userService).assertOwnership(1L);
        verify(repository).delete(recurringTransaction);
    }

    @Test
    void delete_nonExistingRecurringTransaction_throwsCustomException() {

        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> recurringTransactionService.delete(99L)
        );

        assertEquals(
                "Ponavljajuća transakcija nije pronađena.",
                exception.getMessage()
        );

        verify(repository, never())
                .delete(any(RecurringTransaction.class));
    }

    private RecurringTransactionRequest expenseRequest() {

        RecurringTransactionRequest request =
                new RecurringTransactionRequest();

        request.setAmount(new BigDecimal("100.00"));
        request.setType(TransactionType.EXPENSE);
        request.setDescription("Mjesečna kupovina");
        request.setCategoryId(10L);
        request.setDayOfMonth(15);
        request.setActive(true);

        return request;
    }
}