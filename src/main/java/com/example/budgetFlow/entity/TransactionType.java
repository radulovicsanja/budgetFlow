package com.example.budgetFlow.entity;

import com.example.budgetFlow.exception.CustomException;

public enum TransactionType {
    INCOME,
    EXPENSE;

    public static TransactionType from(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException("Tip transakcije je obavezan (Prihod ili Trošak).");
        }
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT)
                .replace('Š', 'S')
                .replace('Ć', 'C')
                .replace('Č', 'C')
                .replace('Ž', 'Z')
                .replace('Đ', 'D');

        if (normalized.equals("INCOME") || normalized.equals("PRIHOD") || normalized.contains("PRIHOD")) {
            return INCOME;
        }
        if (normalized.equals("EXPENSE") || normalized.equals("TROSAK") || normalized.contains("TROS")) {
            return EXPENSE;
        }
        throw new CustomException("Nepoznat tip transakcije. Koristi Prihod ili Trošak.");
    }
}
