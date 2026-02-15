package com.example.budgetFlow.seeders;
import com.example.budgetFlow.entity.CategoryType;
import com.example.budgetFlow.repository.CategoryTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@Component
@SpringBootApplication
public class CategoryTypeSeeder implements CommandLineRunner {

    private final CategoryTypeRepository categoryTypeRepository;

    public CategoryTypeSeeder(CategoryTypeRepository categoryTypeRepository) {
        this.categoryTypeRepository = categoryTypeRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if(categoryTypeRepository.count() == 0) {
            categoryTypeRepository.save(CategoryType.builder().name("ESSENTIAL").description("Osnovne potrebe").build());
            categoryTypeRepository.save(CategoryType.builder().name("OPTIONAL").description("Dodatni troškovi").build());
            categoryTypeRepository.save(CategoryType.builder().name("SAVINGS").description("Neraspoređeni budžet / štednja").build());
            System.out.println("Seedovani CategoryType podaci!");
        }
    }
}
