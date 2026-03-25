package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.CategoryDTO;
import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.entity.CategoryType;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.service.CategoryService;
import com.example.budgetFlow.service.CategoryTypeServiceImpl;
import com.example.budgetFlow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;
    private final CategoryTypeServiceImpl categoryTypeService;

    // CREATE
    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody @Valid CategoryDTO dto) {

        User user = userService.getById(dto.getUserId());
        CategoryType type = categoryTypeService.findById(dto.getTypeId());

        Category category = Category.builder()
                .name(dto.getName())
                .user(user)
                .type(type)
                .isDefault(dto.getIsDefault())
                .build();

        return ResponseEntity.ok(categoryService.createCategory(category));
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getUserCategories(null)); // opcionalno filtrirati po userId
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @PathVariable Long id,
            @RequestBody @Valid CategoryDTO dto) {

        Category existing = categoryService.getById(id);

        existing.setName(dto.getName());
        existing.setUser(userService.getById(dto.getUserId()));
        existing.setType(categoryTypeService.findById(dto.getTypeId()));
        existing.setIsDefault(dto.getIsDefault());

        return ResponseEntity.ok(categoryService.updateCategory(id, existing));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("Category deleted successfully.");
    }
}