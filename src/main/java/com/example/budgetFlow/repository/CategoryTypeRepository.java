package com.example.budgetFlow.repository;

import com.example.budgetFlow.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryTypeRepository extends JpaRepository<CategoryType, Long> {
    boolean existsByName(String name);

    Optional<CategoryType> findByName(String name);
}