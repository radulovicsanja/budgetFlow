package com.example.budgetFlow.scheduler;

import com.example.budgetFlow.service.NotificationService;
import com.example.budgetFlow.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notificationService;
    private final RecurringTransactionService recurringTransactionService;

    /** Svaki dan u 08:00 — bilješke + email + ponavljajuće transakcije. */
    @Scheduled(cron = "0 0 8 * * *")
    public void processDueReminders() {
        log.info("Pokrećem dnevnu obradu bilješki...");
        notificationService.processDueReminders();
        log.info("Pokrećem ponavljajuće transakcije...");
        recurringTransactionService.processDue();
    }
}
