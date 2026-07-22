package com.example.budgetFlow.seeders;

import com.example.budgetFlow.entity.CategoryType;
import com.example.budgetFlow.repository.CategoryTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
public class CategoryTypeSeeder implements CommandLineRunner {

    private final CategoryTypeRepository categoryTypeRepository;

    @Override
    public void run(String... args) {
        // name = kod u logici, description = prikaz u UI
        upsert("ESSENTIAL", "Osnovne potrebe");
        upsert("OPTIONAL", "Želje / dodatni troškovi");
        upsert("SAVINGS", "Štednja");
    }

    private void upsert(String name, String description) {
        categoryTypeRepository.findByName(name).ifPresentOrElse(
                existing -> {
                    if (!description.equals(existing.getDescription())) {
                        existing.setDescription(description);
                        categoryTypeRepository.save(existing);
                    }
                },
                () -> categoryTypeRepository.save(
                        CategoryType.builder()
                                .name(name)
                                .description(description)
                                .build()
                )
        );
    }
}
