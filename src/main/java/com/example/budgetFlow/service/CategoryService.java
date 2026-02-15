package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Category;

import java.util.List;

public interface CategoryService {

    Category createCategory(Category category);

    Category getById(Long id);

    List<Category> getUserCategories(Long userId);

    List<Category> getUserCategoriesByType(Long userId, Long typeId);

    Category updateCategory(Long id, Category category);

    void deleteCategory(Long id);
}
