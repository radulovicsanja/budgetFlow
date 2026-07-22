package com.example.budgetFlow.repository;

import com.example.budgetFlow.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserId(Long userId);

    List<Category> findByUserIdAndTypeId(Long userId, Long typeId);

    boolean existsByUserIdAndName(Long userId, String name);

    Optional<Category> findByUserIdAndNameIgnoreCase(Long userId, String name);

    void deleteByUserId(Long userId);
}
