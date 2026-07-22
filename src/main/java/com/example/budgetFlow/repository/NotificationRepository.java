package com.example.budgetFlow.repository;

import com.example.budgetFlow.entity.Notification;
import com.example.budgetFlow.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    List<Notification> findByTypeAndDueDateAndDueProcessedFalse(
            NotificationType type,
            LocalDate dueDate
    );

    boolean existsByUserIdAndTypeAndTitleAndReadFalse(
            Long userId,
            NotificationType type,
            String title
    );

    void deleteByUserId(Long userId);
}
