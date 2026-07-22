package com.example.budgetFlow.service;

import com.example.budgetFlow.DTO.NotificationRequest;
import com.example.budgetFlow.entity.Notification;
import com.example.budgetFlow.entity.NotificationType;
import com.example.budgetFlow.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface NotificationService {

    Notification create(NotificationRequest request);

    List<Notification> getMyNotifications(boolean unreadOnly);

    Map<String, Long> getUnreadCount();

    Notification markAsRead(Long id);

    int markAllAsRead();

    void delete(Long id);

    /** BUDGET_WARNING kad potrošnja >= 80% allocated. */
    void notifyIfBudgetThresholdReached(
            User user,
            String categoryName,
            String month,
            BigDecimal spentIncludingNew,
            BigDecimal allocated
    );

    /** Dnevni job: podsjetnici sa dueDate = danas. */
    void processDueReminders();
}
