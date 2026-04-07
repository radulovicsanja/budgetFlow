package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.CategoryDTO;
import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.entity.CategoryType;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.service.CategoryService;
import com.example.budgetFlow.service.CategoryTypeServiceImpl;
import com.example.budgetFlow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Categories", description = "Manage spending/income categories")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;
    private final CategoryTypeServiceImpl categoryTypeService;

    // CREATE
    @Operation(summary = "Create category")
    @ApiResponse(responseCode = "200", description = "Category created")
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
    @Operation(summary = "Get all categories")
    @ApiResponse(responseCode = "200", description = "List of categories")
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getUserCategories(null)); // opcionalno filtrirati po userId
    }

    // GET BY ID
    @Operation(summary = "Get category by ID")
    @ApiResponse(responseCode = "200", description = "Category found")
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@Parameter(description = "Category ID") @PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    // UPDATE
    @Operation(summary = "Update category")
    @ApiResponse(responseCode = "200", description = "Category updated")
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @Parameter(description = "Category ID") @PathVariable Long id,
            @RequestBody @Valid CategoryDTO dto) {

        Category existing = categoryService.getById(id);

        existing.setName(dto.getName());
        existing.setUser(userService.getById(dto.getUserId()));
        existing.setType(categoryTypeService.findById(dto.getTypeId()));
        existing.setIsDefault(dto.getIsDefault());

        return ResponseEntity.ok(categoryService.updateCategory(id, existing));
    }

    // DELETE
    @Operation(summary = "Delete category")
    @ApiResponse(responseCode = "200", description = "Category deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(@Parameter(description = "Category ID") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("Category deleted successfully.");
    }
}