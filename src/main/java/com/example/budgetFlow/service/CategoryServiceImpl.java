package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Category createCategory(Category category) {

        if (categoryRepository.existsByUserIdAndName(
                category.getUser().getId(),
                category.getName())) {

            throw new CustomException("Kategorija već postoji.");
        }

        return categoryRepository.save(category);
    }

    @Override
    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new CustomException("Kategorija nije pronađena."));
    }

    @Override
    public List<Category> getUserCategories(Long userId) {
        return categoryRepository.findByUserId(userId);
    }

    @Override
    public List<Category> getUserCategoriesByType(Long userId, Long typeId) {
        return categoryRepository.findByUserIdAndTypeId(userId, typeId);
    }

    @Override
    public Category updateCategory(Long id, Category updatedCategory) {

        Category existing = getById(id);

        existing.setName(updatedCategory.getName());
        existing.setType(updatedCategory.getType());

        return categoryRepository.save(existing);
    }

    @Override
    public void deleteCategory(Long id) {

        Category category = getById(id);

        // ne dozvoljavamo brisanje sistemskih kategorija
        if (category.getIsDefault()) {
            throw new CustomException(
                    "Default kategorije se ne mogu obrisati."
            );
        }

        categoryRepository.deleteById(id);
    }
}
