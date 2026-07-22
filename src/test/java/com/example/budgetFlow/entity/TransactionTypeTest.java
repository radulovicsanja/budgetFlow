package com.example.budgetFlow.entity;

import com.example.budgetFlow.exception.CustomException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTypeTest {

    @Test
    void from_parsesCaseInsensitive() {
        assertEquals(TransactionType.INCOME, TransactionType.from("income"));
        assertEquals(TransactionType.EXPENSE, TransactionType.from("Expense"));
    }

    @Test
    void from_invalid_throws() {
        assertThrows(CustomException.class, () -> TransactionType.from("TRANSFER"));
        assertThrows(CustomException.class, () -> TransactionType.from(" "));
    }
}
