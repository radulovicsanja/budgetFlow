package com.example.budgetFlow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;


    @Value("${budgetflow.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${budgetflow.mail.from:budgetflow@localhost}")
    private String from;

    @Value("${budgetflow.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public EmailService(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    public void sendReminder(
            String toEmail,
            String title,
            String message
    ) {
        if (!canSendEmail(toEmail)) {
            return;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(toEmail);
            mail.setSubject("BudgetFlow podsjetnik: " + title);
            mail.setText(message + "\n\n— BudgetFlow");

            mailSender.getObject().send(mail);

            log.info("Email podsjetnik poslan na {}", toEmail);
        } catch (Exception ex) {
            log.error(
                    "Slanje podsjetnika na {} nije uspjelo.",
                    toEmail,
                    ex
            );
        }
    }

    public void sendPasswordResetEmail(
            String toEmail,
            String token
    ) {

        if (!canSendEmail(toEmail)) {
            return;
        }

        String resetLink =
                frontendUrl + "/reset-password?token=" + token;

        String messageText =
                "Poštovani,\n\n" +
                        "primili smo zahtjev za resetovanje lozinke " +
                        "za vaš BudgetFlow nalog.\n\n" +
                        "Lozinku možete resetovati otvaranjem sljedećeg linka:\n" +
                        resetLink + "\n\n" +
                        "Link važi jedan sat.\n\n" +
                        "Ako nijeste poslali ovaj zahtjev, " +
                        "slobodno zanemarite ovu poruku.\n\n" +
                        "— BudgetFlow";

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(toEmail);
            mail.setSubject("BudgetFlow – oporavak lozinke");
            mail.setText(messageText);

            mailSender.getObject().send(mail);

            log.info(
                    "Email za resetovanje lozinke poslan na {}",
                    toEmail
            );
        } catch (Exception ex) {
            log.error(
                    "Slanje emaila za reset lozinke na {} nije uspjelo.",
                    toEmail,
                    ex
            );

            throw new IllegalStateException(
                    "Email za resetovanje lozinke nije mogao biti poslan.",
                    ex
            );
        }
    }

    private boolean canSendEmail(String toEmail) {
        if (!mailEnabled) {
            log.info(
                    "Email je isključen: budgetflow.mail.enabled=false"
            );
            return false;
        }

        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Email primaoca nije unesen.");
            return false;
        }

        JavaMailSender sender = mailSender.getIfAvailable();

        if (sender == null) {
            log.warn(
                    "JavaMailSender nije dostupan. Email nije poslan."
            );
            return false;
        }

        return true;
    }
}