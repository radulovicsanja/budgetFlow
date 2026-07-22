package com.example.budgetFlow.seeders;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Start-up migracije (kolone/tabele) + admin nalog. */
@Component
@Order(0)
@RequiredArgsConstructor
public class DatabaseBootstrap implements CommandLineRunner {

    public static final String ADMIN_EMAIL = "admin@budgetflow.com";
    public static final String ADMIN_PASSWORD = "admin123";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            ensureRoleColumn();
            ensureTransactionCategoryNullable();
            ensureNotificationDueProcessed();
            ensureSavingsGoalTable();
            ensureRecurringTransactionTable();
            ensureAdminUser();
            System.out.println("Admin login: " + ADMIN_EMAIL + " / " + ADMIN_PASSWORD);
        } catch (Exception e) {
            System.err.println("DatabaseBootstrap greška: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Kolona due_processed za dnevne podsjetnike. */
    private void ensureNotificationDueProcessed() {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE notification ADD COLUMN IF NOT EXISTS due_processed BOOLEAN DEFAULT FALSE"
            );
            jdbcTemplate.update("UPDATE notification SET due_processed = FALSE WHERE due_processed IS NULL");
            System.out.println("notification.due_processed OK");
        } catch (Exception e) {
            System.out.println("notification.due_processed: " + e.getMessage());
        }
    }

    private void ensureSavingsGoalTable() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS savings_goal (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL REFERENCES users(id),
                        title VARCHAR(120) NOT NULL,
                        target_amount NUMERIC(12,2) NOT NULL,
                        current_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                        deadline DATE NOT NULL,
                        note VARCHAR(255),
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            System.out.println("savings_goal table OK");
        } catch (Exception e) {
            System.out.println("savings_goal: " + e.getMessage());
        }
    }

    private void ensureRecurringTransactionTable() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS recurring_transaction (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL REFERENCES users(id),
                        amount NUMERIC(10,2) NOT NULL,
                        type VARCHAR(20) NOT NULL,
                        description VARCHAR(255),
                        category_id BIGINT REFERENCES category(id),
                        day_of_month INT NOT NULL,
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        next_run_date DATE NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            System.out.println("recurring_transaction table OK");
        } catch (Exception e) {
            System.out.println("recurring_transaction: " + e.getMessage());
        }
    }

    /** Prihodi: category_id može biti NULL. */
    private void ensureTransactionCategoryNullable() {
        try {
            // "transaction" je rezervisana riječ u PostgreSQL-u
            jdbcTemplate.execute("ALTER TABLE \"transaction\" ALTER COLUMN category_id DROP NOT NULL");
            System.out.println("transaction.category_id sada dozvoljava NULL (za prihode).");
        } catch (Exception e) {
            try {
                jdbcTemplate.execute("ALTER TABLE transaction ALTER COLUMN category_id DROP NOT NULL");
                System.out.println("transaction.category_id sada dozvoljava NULL (za prihode).");
            } catch (Exception e2) {
                System.out.println("transaction.category_id: " + e2.getMessage());
            }
        }
    }

    private void ensureRoleColumn() {
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20)");
        } catch (Exception e) {
            // fallback bez IF NOT EXISTS
            try {
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN role VARCHAR(20)");
            } catch (Exception ignored) {
                // kolona već postoji
            }
        }
        try {
            jdbcTemplate.update("UPDATE users SET role = 'USER' WHERE role IS NULL");
        } catch (Exception ignored) {
            // ignore
        }
    }

    private void ensureAdminUser() {
        String hash = passwordEncoder.encode(ADMIN_PASSWORD);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE lower(email) = lower(?)",
                Integer.class,
                ADMIN_EMAIL
        );

        if (count != null && count > 0) {
            try {
                jdbcTemplate.update(
                        "UPDATE users SET role = 'ADMIN', password = ? WHERE lower(email) = lower(?)",
                        hash,
                        ADMIN_EMAIL
                );
            } catch (Exception e) {
                jdbcTemplate.update(
                        "UPDATE users SET password = ? WHERE lower(email) = lower(?)",
                        hash,
                        ADMIN_EMAIL
                );
            }
            System.out.println("Admin ažuriran.");
            return;
        }

        Integer usernameTaken = 0;
        try {
            usernameTaken = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE lower(username) = lower(?)",
                    Integer.class,
                    "admin"
            );
        } catch (Exception ignored) {
            // ignore
        }
        String username = (usernameTaken != null && usernameTaken > 0) ? "admin_bf" : "admin";

        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO users (username, email, password, role, created_at)
                    VALUES (?, ?, ?, 'ADMIN', CURRENT_TIMESTAMP)
                    """,
                    username,
                    ADMIN_EMAIL,
                    hash
            );
        } catch (Exception e) {
            jdbcTemplate.update(
                    """
                    INSERT INTO users (username, email, password, role)
                    VALUES (?, ?, ?, 'ADMIN')
                    """,
                    username,
                    ADMIN_EMAIL,
                    hash
            );
        }
        System.out.println("Admin kreiran: " + username);
    }
}
