package com.example.budgetFlow.repository;

import com.example.budgetFlow.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // sve kategorije jednog korisnika
    List<Category> findByUserId(Long userId);

    // filtriranje po tipu (ESSENTIAL / OPTIONAL / SAVINGS)
    List<Category> findByUserIdAndTypeId(Long userId, Long typeId);

    // provjera duplikata naziva kategorije za korisnika
    boolean existsByUserIdAndName(Long userId, String name);
}
