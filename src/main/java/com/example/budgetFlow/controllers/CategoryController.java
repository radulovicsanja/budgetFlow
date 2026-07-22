package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.CategoryDTO;
import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.entity.CategoryType;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.service.CategoryService;
import com.example.budgetFlow.service.CategoryTypeServiceImpl;
import com.example.budgetFlow.service.DefaultCategoryService;
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
    private final DefaultCategoryService defaultCategoryService;

    @Operation(summary = "Create category")
    @ApiResponse(responseCode = "200", description = "Category created")
    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody @Valid CategoryDTO dto) {

        User user = userService.getCurrentUser();
        CategoryType type = categoryTypeService.findById(dto.getTypeId());

        Category category = Category.builder()
                .name(dto.getName())
                .user(user)
                .type(type)
                .isDefault(false) // korisničke kategorije nikad nisu predefinisane
                .build();

        return ResponseEntity.ok(categoryService.createCategory(category));
    }

    @Operation(summary = "Get my categories", description = "Returns predefined and custom categories for the authenticated user")
    @ApiResponse(responseCode = "200", description = "List of categories")
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        User user = userService.getCurrentUser();
        List<Category> list = categoryService.getUserCategories(user.getId());
        if (list.isEmpty()) {
            defaultCategoryService.seedForUser(user);
            list = categoryService.getUserCategories(user.getId());
        }
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Seed default categories", description = "Creates predefined categories for the authenticated user if missing (useful for existing accounts)")
    @ApiResponse(responseCode = "200", description = "Defaults seeded")
    @PostMapping("/seed-defaults")
    public ResponseEntity<List<Category>> seedDefaults() {
        User user = userService.getCurrentUser();
        defaultCategoryService.seedForUser(user);
        return ResponseEntity.ok(categoryService.getUserCategories(user.getId()));
    }

    @Operation(summary = "Get category by ID")
    @ApiResponse(responseCode = "200", description = "Category found")
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@Parameter(description = "Category ID") @PathVariable Long id) {
        Category category = categoryService.getById(id);
        userService.assertOwnership(category.getUser().getId());
        return ResponseEntity.ok(category);
    }

    @Operation(summary = "Update category")
    @ApiResponse(responseCode = "200", description = "Category updated")
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @Parameter(description = "Category ID") @PathVariable Long id,
            @RequestBody @Valid CategoryDTO dto) {

        Category existing = categoryService.getById(id);
        userService.assertOwnership(existing.getUser().getId());

        existing.setName(dto.getName());
        existing.setType(categoryTypeService.findById(dto.getTypeId()));
        // vlasnika ne mijenjamo — ostaje trenutni korisnik

        return ResponseEntity.ok(categoryService.updateCategory(id, existing));
    }

    @Operation(summary = "Delete category")
    @ApiResponse(responseCode = "200", description = "Category deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(@Parameter(description = "Category ID") @PathVariable Long id) {
        Category existing = categoryService.getById(id);
        userService.assertOwnership(existing.getUser().getId());
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("Category deleted successfully.");
    }
}
