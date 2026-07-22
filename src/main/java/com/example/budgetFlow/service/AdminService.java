package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.Role;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.exception.ForbiddenException;
import com.example.budgetFlow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Admin operacije nad korisnicima. */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final NotificationRepository notificationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserReportRepository userReportRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;

    public List<User> listUsers() {
        requireAdmin();
        return userRepository.findAll();
    }

    /** Briše korisnika i sve povezane podatke. */
    @Transactional
    public void deleteUser(Long userId) {
        requireAdmin();
        User current = userService.getCurrentUser();
        if (current.getId().equals(userId)) {
            throw new CustomException("Ne možeš obrisati vlastiti admin nalog.");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Korisnik nije pronađen."));

        passwordResetTokenRepository.deleteByUser(target);
        notificationRepository.deleteByUserId(userId);
        recurringTransactionRepository.deleteByUserId(userId);
        savingsGoalRepository.deleteByUserId(userId);
        transactionRepository.deleteByUserId(userId);
        budgetCategoryRepository.deleteByBudget_User_Id(userId);
        budgetCategoryRepository.deleteByCategory_User_Id(userId);
        budgetRepository.deleteByUserId(userId);
        categoryRepository.deleteByUserId(userId);
        userReportRepository.deleteByUserId(userId);
        userRepository.delete(target);
    }

    @Transactional
    public User updateRole(Long userId, Role role) {
        requireAdmin();
        User current = userService.getCurrentUser();
        if (current.getId().equals(userId) && role != Role.ADMIN) {
            throw new CustomException("Ne možeš sam sebi ukloniti ADMIN ulogu.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Korisnik nije pronađen."));
        user.setRole(role);
        return userRepository.save(user);
    }

    private void requireAdmin() {
        User current = userService.getCurrentUser();
        if (current.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Samo admin ima pristup.");
        }
    }
}
