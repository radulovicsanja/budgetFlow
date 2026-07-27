package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.entity.CategoryType;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private User user;
    private CategoryType categoryType;
    private Category category;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("sanja")
                .email("sanja@gmail.com")
                .password("password123")
                .build();

        categoryType = CategoryType.builder()
                .id(2L)
                .name("ESSENTIAL")
                .description("Osnovni troškovi")
                .build();

        category = Category.builder()
                .id(5L)
                .name("Hrana")
                .user(user)
                .type(categoryType)
                .isDefault(false)
                .build();
    }

    @Test
    void createCategory_validCategory_savesCategory() {

        when(categoryRepository.existsByUserIdAndName(1L, "Hrana"))
                .thenReturn(false);

        when(categoryRepository.save(category))
                .thenReturn(category);

        Category result = categoryService.createCategory(category);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("Hrana", result.getName());
        assertEquals(user, result.getUser());
        assertEquals(categoryType, result.getType());

        verify(categoryRepository)
                .existsByUserIdAndName(1L, "Hrana");

        verify(categoryRepository)
                .save(category);
    }

    @Test
    void createCategory_existingCategory_throwsCustomException() {

        when(categoryRepository.existsByUserIdAndName(1L, "Hrana"))
                .thenReturn(true);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> categoryService.createCategory(category)
        );

        assertEquals(
                "Kategorija već postoji.",
                exception.getMessage()
        );

        verify(categoryRepository)
                .existsByUserIdAndName(1L, "Hrana");

        verify(categoryRepository, never())
                .save(any(Category.class));
    }

    @Test
    void getById_existingCategory_returnsCategory() {

        when(categoryRepository.findById(5L))
                .thenReturn(Optional.of(category));

        Category result = categoryService.getById(5L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("Hrana", result.getName());

        verify(categoryRepository)
                .findById(5L);
    }

    @Test
    void getById_nonExistingCategory_throwsCustomException() {

        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> categoryService.getById(99L)
        );

        assertEquals(
                "Kategorija nije pronađena.",
                exception.getMessage()
        );

        verify(categoryRepository)
                .findById(99L);
    }

    @Test
    void getUserCategories_returnsCategoriesForUser() {

        Category secondCategory = Category.builder()
                .id(6L)
                .name("Stanovanje")
                .user(user)
                .type(categoryType)
                .isDefault(false)
                .build();

        when(categoryRepository.findByUserId(1L))
                .thenReturn(List.of(category, secondCategory));

        List<Category> result =
                categoryService.getUserCategories(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Hrana", result.get(0).getName());
        assertEquals("Stanovanje", result.get(1).getName());

        verify(categoryRepository)
                .findByUserId(1L);
    }

    @Test
    void getUserCategoriesByType_returnsCategoriesOfSelectedType() {

        when(categoryRepository.findByUserIdAndTypeId(1L, 2L))
                .thenReturn(List.of(category));

        List<Category> result =
                categoryService.getUserCategoriesByType(1L, 2L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(categoryType, result.get(0).getType());

        verify(categoryRepository)
                .findByUserIdAndTypeId(1L, 2L);
    }

    @Test
    void updateCategory_customCategory_updatesNameAndType() {

        CategoryType newType = CategoryType.builder()
                .id(3L)
                .name("OPTIONAL")
                .description("Opcioni troškovi")
                .build();

        Category updatedCategory = Category.builder()
                .name("Restorani")
                .type(newType)
                .build();

        when(categoryRepository.findById(5L))
                .thenReturn(Optional.of(category));

        when(categoryRepository.save(category))
                .thenReturn(category);

        Category result =
                categoryService.updateCategory(5L, updatedCategory);

        assertNotNull(result);
        assertEquals("Restorani", result.getName());
        assertEquals(newType, result.getType());

        verify(categoryRepository)
                .findById(5L);

        verify(categoryRepository)
                .save(category);
    }

    @Test
    void updateCategory_defaultCategory_throwsCustomException() {

        category.setIsDefault(true);

        Category updatedCategory = Category.builder()
                .name("Novi naziv")
                .type(categoryType)
                .build();

        when(categoryRepository.findById(5L))
                .thenReturn(Optional.of(category));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> categoryService.updateCategory(
                        5L,
                        updatedCategory
                )
        );

        assertEquals(
                "Predefinisane kategorije se ne mogu mijenjati.",
                exception.getMessage()
        );

        verify(categoryRepository, never())
                .save(any(Category.class));
    }

    @Test
    void deleteCategory_customCategory_deletesCategory() {

        when(categoryRepository.findById(5L))
                .thenReturn(Optional.of(category));

        categoryService.deleteCategory(5L);

        verify(categoryRepository)
                .findById(5L);

        verify(categoryRepository)
                .deleteById(5L);
    }

    @Test
    void deleteCategory_defaultCategory_throwsCustomException() {

        category.setIsDefault(true);

        when(categoryRepository.findById(5L))
                .thenReturn(Optional.of(category));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> categoryService.deleteCategory(5L)
        );

        assertEquals(
                "Predefinisane kategorije se ne mogu obrisati.",
                exception.getMessage()
        );

        verify(categoryRepository, never())
                .deleteById(anyLong());
    }
}