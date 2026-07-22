package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.entity.CategoryType;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.CategoryRepository;
import com.example.budgetFlow.repository.CategoryTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Seed podrazumijevanih kategorija za novog korisnika. */
@Service
@RequiredArgsConstructor
public class DefaultCategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryTypeRepository categoryTypeRepository;

    private static final Map<String, List<String>> DEFAULTS_LOCALIZED = Map.of(
            "ESSENTIAL", List.of("Stanarina", "Računi", "Hrana", "Prevoz"),
            "OPTIONAL", List.of("Izlasci", "Luksuz", "Zabava", "Odjeća"),
            "SAVINGS", List.of("Štednja", "Neraspoređeno")
    );

    public static final String INCOME_CATEGORY_NAME = "Prihod";

    @Transactional
    public void seedForUser(User user) {
        if (user == null || user.getId() == null) {
            throw new CustomException("Korisnik mora biti sačuvan prije seedovanja kategorija.");
        }

        for (Map.Entry<String, List<String>> entry : DEFAULTS_LOCALIZED.entrySet()) {
            CategoryType type = requireType(entry.getKey());

            for (String name : entry.getValue()) {
                if (categoryRepository.existsByUserIdAndName(user.getId(), name)) {
                    continue;
                }

                Category category = Category.builder()
                        .name(name)
                        .user(user)
                        .type(type)
                        .isDefault(true)
                        .build();

                categoryRepository.save(category);
            }
        }

        getOrCreateIncomeCategory(user);
    }

    /** Sistemska kategorija "Prihod" — korisnik je ne bira. */
    @Transactional
    public Category getOrCreateIncomeCategory(User user) {
        return categoryRepository.findByUserIdAndNameIgnoreCase(user.getId(), INCOME_CATEGORY_NAME)
                .orElseGet(() -> {
                    CategoryType type = requireType("SAVINGS");
                    return categoryRepository.save(Category.builder()
                            .name(INCOME_CATEGORY_NAME)
                            .user(user)
                            .type(type)
                            .isDefault(true)
                            .build());
                });
    }

    private CategoryType requireType(String name) {
        return categoryTypeRepository.findByName(name)
                .orElseGet(() -> categoryTypeRepository.save(
                        CategoryType.builder()
                                .name(name)
                                .description(name)
                                .build()
                ));
    }
}
