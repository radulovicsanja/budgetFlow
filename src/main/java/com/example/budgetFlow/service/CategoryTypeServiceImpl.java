package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.CategoryType;
import com.example.budgetFlow.repository.CategoryTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryTypeServiceImpl implements  CategoryTypeService{

    private final CategoryTypeRepository categoryTypeRepository;

    public CategoryTypeServiceImpl(CategoryTypeRepository categoryTypeRepository){
        this.categoryTypeRepository = categoryTypeRepository;
    }

    @Override
    public CategoryType save (CategoryType categoryType){
        return categoryTypeRepository.save(categoryType);
    }

    @Override
    public List<CategoryType> findAll(){
        return categoryTypeRepository.findAll();
    }

    @Override
    public CategoryType findById(Long id) {
        return categoryTypeRepository.findById(id).orElseThrow(() ->
        new RuntimeException("Tip kategorije nije pronadjen."));
    }

    @Override
    public void deleteById(Long id){
        categoryTypeRepository.deleteById(id);
    }

}
