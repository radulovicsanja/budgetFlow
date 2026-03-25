package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.CategoryTypeDTO;
import com.example.budgetFlow.entity.CategoryType;
import com.example.budgetFlow.service.CategoryTypeServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/category-types")
@RequiredArgsConstructor
public class CategoryTypeController {

    private final CategoryTypeServiceImpl categoryTypeService;

    // CREATE
    @PostMapping
    public ResponseEntity<CategoryType> createCategoryType(@RequestBody @Valid CategoryTypeDTO dto) {

        CategoryType categoryType = CategoryType.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();

        return ResponseEntity.ok(categoryTypeService.save(categoryType));
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<List<CategoryType>> getAllCategoryTypes() {
        return ResponseEntity.ok(categoryTypeService.findAll());
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<CategoryType> getCategoryTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryTypeService.findById(id));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<CategoryType> updateCategoryType(
            @PathVariable Long id,
            @RequestBody @Valid CategoryTypeDTO dto) {

        CategoryType existing = categoryTypeService.findById(id);
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());

        return ResponseEntity.ok(categoryTypeService.save(existing));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategoryType(@PathVariable Long id) {
        categoryTypeService.deleteById(id);
        return ResponseEntity.ok("CategoryType deleted successfully.");
    }
}