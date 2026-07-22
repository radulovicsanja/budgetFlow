package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.CategoryTypeDTO;
import com.example.budgetFlow.entity.CategoryType;
import com.example.budgetFlow.service.CategoryTypeServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Category Types", description = "Manage category types (ESSENTIAL, OPTIONAL, SAVINGS)")
@RestController
@RequestMapping("/api/category-types")
@RequiredArgsConstructor
public class CategoryTypeController {

    private final CategoryTypeServiceImpl categoryTypeService;

    @Operation(summary = "Create category type")
    @ApiResponse(responseCode = "200", description = "Category type created")
    @PostMapping
    public ResponseEntity<CategoryType> createCategoryType(@RequestBody @Valid CategoryTypeDTO dto) {

        CategoryType categoryType = CategoryType.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();

        return ResponseEntity.ok(categoryTypeService.save(categoryType));
    }

    @Operation(summary = "Get all category types")
    @ApiResponse(responseCode = "200", description = "List of category types")
    @GetMapping
    public ResponseEntity<List<CategoryType>> getAllCategoryTypes() {
        return ResponseEntity.ok(categoryTypeService.findAll());
    }

    @Operation(summary = "Get category type by ID")
    @ApiResponse(responseCode = "200", description = "Category type found")
    @GetMapping("/{id}")
    public ResponseEntity<CategoryType> getCategoryTypeById(@Parameter(description = "Category type ID") @PathVariable Long id) {
        return ResponseEntity.ok(categoryTypeService.findById(id));
    }

    @Operation(summary = "Update category type")
    @ApiResponse(responseCode = "200", description = "Category type updated")
    @PutMapping("/{id}")
    public ResponseEntity<CategoryType> updateCategoryType(
            @Parameter(description = "Category type ID") @PathVariable Long id,
            @RequestBody @Valid CategoryTypeDTO dto) {

        CategoryType existing = categoryTypeService.findById(id);
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());

        return ResponseEntity.ok(categoryTypeService.save(existing));
    }

    @Operation(summary = "Delete category type")
    @ApiResponse(responseCode = "200", description = "Category type deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategoryType(@Parameter(description = "Category type ID") @PathVariable Long id) {
        categoryTypeService.deleteById(id);
        return ResponseEntity.ok("CategoryType deleted successfully.");
    }
}
