package com.example.budgetFlow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Email podsjetnici (opcionalno, kad je mail uključen). */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${budgetflow.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${budgetflow.mail.from:budgetflow@localhost}")
    private String from;

    public EmailService(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    public void sendReminder(String toEmail, String title, String message) {
        if (!mailEnabled) {
            log.info("Email isključen (budgetflow.mail.enabled=false). Podsjetnik za {}: {}", toEmail, title);
            return;
        }
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("JavaMailSender nije dostupan — email nije poslan.");
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(toEmail);
            mail.setSubject("BudgetFlow podsjetnik: " + title);
            mail.setText(message + "\n\n— BudgetFlow");
            sender.send(mail);
            log.info("Email podsjetnik poslan na {}", toEmail);
        } catch (Exception ex) {
            log.warn("Slanje emaila nije uspjelo na {}: {}", toEmail, ex.getMessage());
        }
    }
}
