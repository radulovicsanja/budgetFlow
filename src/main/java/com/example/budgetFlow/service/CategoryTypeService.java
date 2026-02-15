package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.CategoryType;

import java.util.List;

public interface CategoryTypeService {

    CategoryType save(CategoryType categoryType);

    List<CategoryType> findAll();

    CategoryType findById(Long id);

    void deleteById(Long id);
}
