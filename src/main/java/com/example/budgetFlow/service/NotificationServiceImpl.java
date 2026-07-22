package com.example.budgetFlow.service;

import com.example.budgetFlow.DTO.NotificationRequest;
import com.example.budgetFlow.entity.Notification;
import com.example.budgetFlow.entity.NotificationType;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Podsjetnici, budget warningi i dnevni job. */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("0.80");

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final EmailService emailService;

    @Override
    @Transactional
    public Notification create(NotificationRequest request) {
        User user = userService.getCurrentUser();

        NotificationType type = request.getType() != null
                ? request.getType()
                : NotificationType.BILL_REMINDER;

        // korisnik ne smije ručno kreirati BUDGET_WARNING (to radi sistem)
        if (type == NotificationType.BUDGET_WARNING) {
            type = NotificationType.BILL_REMINDER;
        }

        Notification notification = Notification.builder()
                .user(user)
                .title(request.getTitle().trim())
                .message(request.getMessage().trim())
                .type(type)
                .dueDate(request.getDueDate())
                .read(false)
                .dueProcessed(false)
                .build();

        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getMyNotifications(boolean unreadOnly) {
        Long userId = userService.getCurrentUser().getId();
        if (unreadOnly) {
            return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Map<String, Long> getUnreadCount() {
        Long userId = userService.getCurrentUser().getId();
        return Map.of("unreadCount", notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @Override
    @Transactional
    public Notification markAsRead(Long id) {
        Notification notification = getOwned(id);
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead() {
        Long userId = userService.getCurrentUser().getId();
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Notification notification = getOwned(id);
        notificationRepository.delete(notification);
    }

    /** Upozorenje kad potrošnja pređe 80% limita kategorije. */
    @Override
    @Transactional
    public void notifyIfBudgetThresholdReached(
            User user,
            String categoryName,
            String month,
            BigDecimal spentIncludingNew,
            BigDecimal allocated
    ) {
        if (user == null || allocated == null || allocated.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal ratio = spentIncludingNew.divide(allocated, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(WARNING_THRESHOLD) < 0) {
            return;
        }

        String title = "Upozorenje: " + categoryName + " (" + month + ")";
        if (notificationRepository.existsByUserIdAndTypeAndTitleAndReadFalse(
                user.getId(), NotificationType.BUDGET_WARNING, title)) {
            return;
        }

        int percent = ratio.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        Notification warning = Notification.builder()
                .user(user)
                .title(title)
                .message("Potrošnja u kategoriji \"" + categoryName + "\" je na " + percent
                        + "% budžeta (" + spentIncludingNew + " / " + allocated + "€).")
                .type(NotificationType.BUDGET_WARNING)
                .dueDate(LocalDate.now())
                .read(false)
                .dueProcessed(true)
                .build();

        notificationRepository.save(warning);
    }

    /** Dnevni job: dospjeli podsjetnici + email. */
    @Override
    @Transactional
    public void processDueReminders() {
        LocalDate today = LocalDate.now();
        List<Notification> due = notificationRepository.findByTypeAndDueDateAndDueProcessedFalse(
                NotificationType.BILL_REMINDER, today
        );

        for (Notification reminder : due) {
            String title = "Podsjetnik: " + reminder.getTitle();
            String body = "Danas dospijeva: " + reminder.getMessage();

            Notification info = Notification.builder()
                    .user(reminder.getUser())
                    .title(title)
                    .message(body)
                    .type(NotificationType.INFO)
                    .dueDate(today)
                    .read(false)
                    .dueProcessed(true)
                    .build();
            notificationRepository.save(info);

            if (reminder.getUser() != null) {
                emailService.sendReminder(reminder.getUser().getEmail(), title, body);
            }

            reminder.setDueProcessed(true);
            notificationRepository.save(reminder);
        }
    }

    private Notification getOwned(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new CustomException("Notifikacija nije pronađena."));
        userService.assertOwnership(notification.getUser().getId());
        return notification;
    }
}
