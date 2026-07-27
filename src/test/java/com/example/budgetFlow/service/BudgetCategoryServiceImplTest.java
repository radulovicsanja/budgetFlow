package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Budget;
import com.example.budgetFlow.entity.BudgetCategory;
import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.repository.BudgetCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetCategoryServiceImplTest {

    @Mock
    private BudgetCategoryRepository budgetCategoryRepository;

    @InjectMocks
    private BudgetCategoryServiceImpl budgetCategoryService;

    private User user;
    private Budget budget;
    private Category category;
    private BudgetCategory budgetCategory;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .id(1L)
                .username("sanja")
                .email("sanja@mail.com")
                .password("password")
                .build();

        budget = Budget.builder()
                .id(10L)
                .user(user)
                .month("2026-07")
                .totalAmount(new BigDecimal("1000.00"))
                .additionalIncome(new BigDecimal("200.00"))
                .build();

        category = Category.builder()
                .id(5L)
                .name("Hrana")
                .user(user)
                .isDefault(false)
                .build();

        budgetCategory = BudgetCategory.builder()
                .id(20L)
                .budget(budget)
                .category(category)
                .percentage(new BigDecimal("30.00"))
                .allocatedAmount(new BigDecimal("360.00"))
                .build();
    }

    @Test
    void save_validBudgetCategory_savesAndReturnsBudgetCategory() {

        when(budgetCategoryRepository.save(budgetCategory))
                .thenReturn(budgetCategory);

        BudgetCategory result =
                budgetCategoryService.save(budgetCategory);

        assertNotNull(result);
        assertEquals(20L, result.getId());
        assertEquals(budget, result.getBudget());
        assertEquals(category, result.getCategory());

        assertEquals(
                0,
                new BigDecimal("30.00")
                        .compareTo(result.getPercentage())
        );

        assertEquals(
                0,
                new BigDecimal("360.00")
                        .compareTo(result.getAllocatedAmount())
        );

        verify(budgetCategoryRepository).save(budgetCategory);
    }

    @Test
    void update_existingBudgetCategory_savesUpdatedBudgetCategory() {

        budgetCategory.setPercentage(new BigDecimal("40.00"));
        budgetCategory.setAllocatedAmount(new BigDecimal("480.00"));

        when(budgetCategoryRepository.save(budgetCategory))
                .thenReturn(budgetCategory);

        BudgetCategory result =
                budgetCategoryService.update(budgetCategory);

        assertNotNull(result);

        assertEquals(
                0,
                new BigDecimal("40.00")
                        .compareTo(result.getPercentage())
        );

        assertEquals(
                0,
                new BigDecimal("480.00")
                        .compareTo(result.getAllocatedAmount())
        );

        verify(budgetCategoryRepository).save(budgetCategory);
    }

    @Test
    void update_budgetCategoryWithoutId_throwsIllegalArgumentException() {

        budgetCategory.setId(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> budgetCategoryService.update(budgetCategory)
        );

        assertEquals(
                "BudgetCategory ID must not be null for update",
                exception.getMessage()
        );

        verify(budgetCategoryRepository, never()).save(any());
    }

    @Test
    void delete_validId_deletesBudgetCategory() {

        budgetCategoryService.delete(20L);

        verify(budgetCategoryRepository).deleteById(20L);
    }

    @Test
    void getById_existingBudgetCategory_returnsBudgetCategory() {

        when(budgetCategoryRepository.findById(20L))
                .thenReturn(Optional.of(budgetCategory));

        BudgetCategory result =
                budgetCategoryService.getById(20L);

        assertNotNull(result);
        assertEquals(20L, result.getId());
        assertEquals("Hrana", result.getCategory().getName());

        verify(budgetCategoryRepository).findById(20L);
    }

    @Test
    void getById_nonExistingBudgetCategory_returnsNull() {

        when(budgetCategoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        BudgetCategory result =
                budgetCategoryService.getById(99L);

        assertNull(result);

        verify(budgetCategoryRepository).findById(99L);
    }

    @Test
    void getByBudgetId_returnsBudgetCategories() {

        Category transportCategory = Category.builder()
                .id(6L)
                .name("Prevoz")
                .user(user)
                .isDefault(false)
                .build();

        BudgetCategory transportBudgetCategory =
                BudgetCategory.builder()
                        .id(21L)
                        .budget(budget)
                        .category(transportCategory)
                        .percentage(new BigDecimal("20.00"))
                        .allocatedAmount(new BigDecimal("240.00"))
                        .build();

        when(budgetCategoryRepository.findByBudgetId(10L))
                .thenReturn(List.of(
                        budgetCategory,
                        transportBudgetCategory
                ));

        List<BudgetCategory> result =
                budgetCategoryService.getByBudgetId(10L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Hrana", result.get(0).getCategory().getName());
        assertEquals("Prevoz", result.get(1).getCategory().getName());

        verify(budgetCategoryRepository).findByBudgetId(10L);
    }
}