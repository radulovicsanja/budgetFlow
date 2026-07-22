package com.example.budgetFlow.entity;

public enum NotificationType {
    /** Ručni podsjetnik (npr. neplaćeni račun). */
    BILL_REMINDER,
    /** Automatsko upozorenje o potrošnji kategorije. */
    BUDGET_WARNING,
    /** Opšta informacija sistema. */
    INFO
}
