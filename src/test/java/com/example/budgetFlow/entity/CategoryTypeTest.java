package com.example.budgetFlow.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTypeTest {

    @Test
    void builder_createsCategoryTypeCorrectly() {

        CategoryType type = CategoryType.builder()
                .id(1L)
                .name("ESSENTIAL")
                .description("Osnovni troškovi")
                .build();

        assertEquals(1L, type.getId());
        assertEquals("ESSENTIAL", type.getName());
        assertEquals("Osnovni troškovi", type.getDescription());
    }

    @Test
    void setters_updateFieldsCorrectly() {

        CategoryType type = new CategoryType();

        type.setId(2L);
        type.setName("OPTIONAL");
        type.setDescription("Neobavezni troškovi");

        assertEquals(2L, type.getId());
        assertEquals("OPTIONAL", type.getName());
        assertEquals("Neobavezni troškovi", type.getDescription());
    }

    @Test
    void allArgsConstructor_setsAllFields() {

        CategoryType type = new CategoryType(
                3L,
                "SAVINGS",
                "Štednja"
        );

        assertEquals(3L, type.getId());
        assertEquals("SAVINGS", type.getName());
        assertEquals("Štednja", type.getDescription());
    }

    @Test
    void noArgsConstructor_createsEmptyObject() {

        CategoryType type = new CategoryType();

        assertNull(type.getId());
        assertNull(type.getName());
        assertNull(type.getDescription());
    }

}