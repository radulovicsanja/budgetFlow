package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.*;
import com.example.budgetFlow.exception.BudgetOverspendException;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.BudgetCategoryRepository;
import com.example.budgetFlow.repository.BudgetRepository;
import com.example.budgetFlow.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Servis za mjesečni budžet, alokacije i 50/30/20. */
@RequiredArgsConstructor
@Service
public class BudgetServiceImpl implements BudgetService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;
    private final BudgetCategoryService budgetCategoryService;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    /** Kreira mjesečni budžet (jedan po mjesecu). */
    @Override
    public Budget createBudget(Budget budget) {
        if (budgetRepository.existsByUserIdAndMonth(
                budget.getUser().getId(),
                budget.getMonth())) {
            throw new CustomException("Budžet za ovaj mjesec već postoji.");
        }
        return budgetRepository.save(budget);
    }

    @Override
    public Budget getBudgetById(Long id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new CustomException("Budžet nije pronađen."));
    }

    @Override
    public Budget getUserBudgetForMonth(Long userId, String month) {
        return budgetRepository.findByUserIdAndMonth(userId, month)
                .orElseThrow(() -> new CustomException("Budžet za mjesec ne postoji."));
    }

    @Override
    public List<Budget> getAllUserBudgets(Long userId) {
        return budgetRepository.findByUserId(userId);
    }

    @Override
    public Budget updateBudget(Long id, Budget updatedBudget) {
        Budget existing = getBudgetById(id);
        existing.setTotalAmount(updatedBudget.getTotalAmount());
        existing.setAdditionalIncome(updatedBudget.getAdditionalIncome());
        existing.setMonth(updatedBudget.getMonth());
        return budgetRepository.save(existing);
    }

    /** Ažurira budžet nakon nove transakcije (prihod ili trošak). */
    @Override
    @Transactional
    public void updateBudgetAfterTransaction(Transaction transaction, boolean confirmFromUnallocated) {
        if (transaction.getDate() == null || transaction.getUser() == null) {
            throw new CustomException("Transakcija mora imati korisnika i datum.");
        }

        String month = transaction.getDate().format(MONTH_FORMAT);

        if (transaction.getType() == null) {
            throw new CustomException("Nepoznat tip transakcije.");
        }

        // prihod: budžet se ažurira samo ako postoji za taj mjesec
        if (transaction.getType() == TransactionType.INCOME) {
            budgetRepository.findByUserIdAndMonth(transaction.getUser().getId(), month)
                    .ifPresent(budget -> applyIncome(budget, transaction.getAmount()));
            return;
        }

        if (transaction.getType() == TransactionType.EXPENSE) {
            Budget budget = budgetRepository
                    .findByUserIdAndMonth(transaction.getUser().getId(), month)
                    .orElseThrow(() -> new CustomException(
                            "Budžet za mjesec " + month + " ne postoji. Prvo kreiraj budžet."
                    ));
            applyExpense(budget, transaction, confirmFromUnallocated);
            return;
        }

        throw new CustomException("Nepoznat tip transakcije.");
    }

    /** Poništi efekat transakcije na budžet (pri brisanju/izmjeni). */
    @Override
    @Transactional
    public void reverseBudgetAfterTransaction(Transaction transaction) {
        if (transaction == null || transaction.getDate() == null || transaction.getUser() == null
                || transaction.getType() == null || transaction.getAmount() == null) {
            return;
        }

        String month = transaction.getDate().format(MONTH_FORMAT);

        if (transaction.getType() == TransactionType.INCOME) {
            budgetRepository.findByUserIdAndMonth(transaction.getUser().getId(), month)
                    .ifPresent(budget -> {
                        BigDecimal current = budget.getAdditionalIncome() != null
                                ? budget.getAdditionalIncome()
                                : BigDecimal.ZERO;
                        BigDecimal next = current.subtract(transaction.getAmount());
                        if (next.compareTo(BigDecimal.ZERO) < 0) {
                            next = BigDecimal.ZERO;
                        }
                        budget.setAdditionalIncome(next);
                        budgetRepository.save(budget);
                    });
        }
        // EXPENSE: potrošnja se računa iz preostalih transakcija
    }

    private void applyIncome(Budget budget, BigDecimal amount) {
        BigDecimal current = budget.getAdditionalIncome() != null
                ? budget.getAdditionalIncome()
                : BigDecimal.ZERO;
        budget.setAdditionalIncome(current.add(amount));
        budgetRepository.save(budget);
    }

    /** Primjenjuje trošak na alokaciju kategorije (uz neraspoređeno po potrebi). */
    private void applyExpense(Budget budget, Transaction transaction, boolean confirmFromUnallocated) {
        if (transaction.getCategory() == null || transaction.getCategory().getId() == null) {
            throw new CustomException("Trošak mora imati kategoriju.");
        }

        BudgetCategory budgetCategory = budgetCategoryRepository
                .findByBudgetIdAndCategoryId(budget.getId(), transaction.getCategory().getId());

        if (budgetCategory == null) {
            throw new CustomException(
                    "Kategorija nije raspoređena u budžetu. Prvo dodijelite procenat/iznos kategoriji."
            );
        }

        BigDecimal spentSoFar = calculateSpentInCategory(
                transaction.getUser().getId(),
                transaction.getCategory().getId(),
                budget.getMonth()
        );

        BigDecimal allocated = budgetCategory.getAllocatedAmount();
        BigDecimal remaining = allocated.subtract(spentSoFar);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        BigDecimal amount = transaction.getAmount();
        BigDecimal spentAfter = spentSoFar.add(amount);

        // stane u raspoloživo kategorije
        if (amount.compareTo(remaining) <= 0) {
            notifyBudgetWarning(budget, transaction, spentAfter, allocated);
            return;
        }

        BigDecimal shortage = amount.subtract(remaining);
        BigDecimal unallocated = calculateUnallocated(budget);

        if (shortage.compareTo(unallocated) > 0) {
            throw new BudgetOverspendException(
                    "Nema dovoljno sredstava. Potrebno " + shortage +
                            "€ iz neraspoređenog, a dostupno je " + unallocated + "€.",
                    shortage,
                    unallocated,
                    remaining,
                    false
            );
        }

        if (!confirmFromUnallocated) {
            throw new BudgetOverspendException(
                    "Trošak premašuje raspoloživo u kategoriji za " + shortage +
                            "€. Potvrdite uzimanje iz neraspoređenog (confirmFromUnallocated=true) ili odbijte transakciju.",
                    shortage,
                    unallocated,
                    remaining,
                    true
            );
        }

        // potvrđeno: uvećaj allocated za shortage
        BigDecimal newAllocated = allocated.add(shortage);
        budgetCategory.setAllocatedAmount(newAllocated);

        BigDecimal totalAvailable = getTotalAvailable(budget);
        if (totalAvailable.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal newPercentage = newAllocated
                    .divide(totalAvailable, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            budgetCategory.setPercentage(newPercentage);
        }

        budgetCategoryRepository.save(budgetCategory);
        notifyBudgetWarning(budget, transaction, spentAfter, newAllocated);
    }

    private void notifyBudgetWarning(
            Budget budget,
            Transaction transaction,
            BigDecimal spentAfter,
            BigDecimal allocated
    ) {
        try {
            if (transaction.getCategory() == null) return;
            String categoryName = transaction.getCategory().getName();
            notificationService.notifyIfBudgetThresholdReached(
                    transaction.getUser(),
                    categoryName,
                    budget.getMonth(),
                    spentAfter,
                    allocated
            );
        } catch (Exception ex) {
            System.err.println("Budget warning notification failed: " + ex.getMessage());
        }
    }

    private BigDecimal calculateSpentInCategory(Long userId, Long categoryId, String month) {
        return transactionRepository.findByUserIdAndCategoryId(userId, categoryId).stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .filter(t -> t.getDate() != null && month.equals(t.getDate().format(MONTH_FORMAT)))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateUnallocated(Budget budget) {
        BigDecimal totalAvailable = getTotalAvailable(budget);
        BigDecimal allocatedSum = budgetCategoryRepository.findByBudgetId(budget.getId()).stream()
                .map(BudgetCategory::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unallocated = totalAvailable.subtract(allocatedSum);
        return unallocated.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : unallocated;
    }

    /** Ukupan prihod budžeta (totalAmount + additionalIncome). */
    private BigDecimal getTotalAvailable(Budget budget) {
        BigDecimal total = budget.getTotalAmount() != null ? budget.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal additional = budget.getAdditionalIncome() != null
                ? budget.getAdditionalIncome()
                : BigDecimal.ZERO;
        return total.add(additional);
    }

    @Override
    public Budget getById(Long id) {
        Optional<Budget> budget = budgetRepository.findById(id);
        return budget.orElse(null);
    }

    @Override
    public BudgetCategory addOrUpdateCategoryManual(Long budgetId, Long categoryId, Double percentage, Double allocatedAmount) {
        Budget budget = getById(budgetId);
        if (budget == null) throw new CustomException("Budžet nije pronađen");

        Category category = categoryService.getById(categoryId);
        if (category == null) throw new CustomException("Kategorija nije pronađena");

        BigDecimal totalBudget = getTotalAvailable(budget);
        if (totalBudget.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("Budžet mora imati iznos veći od nule.");
        }
        BigDecimal perc = percentage != null ? BigDecimal.valueOf(percentage) : BigDecimal.ZERO;
        BigDecimal amount = allocatedAmount != null ? BigDecimal.valueOf(allocatedAmount) : BigDecimal.ZERO;

        BudgetCategory bc = budgetCategoryRepository.findByBudgetIdAndCategoryId(budgetId, categoryId);
        if (bc == null) {
            bc = new BudgetCategory();
            bc.setBudget(budget);
            bc.setCategory(category);
        }

        if (perc.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal calcAmount = totalBudget.multiply(perc).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            bc.setPercentage(perc);
            bc.setAllocatedAmount(calcAmount);
        } else if (amount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal calcPerc = amount.divide(totalBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            bc.setAllocatedAmount(amount);
            bc.setPercentage(calcPerc);
        } else {
            throw new CustomException("Morate unijeti ili procenat ili iznos");
        }

        return budgetCategoryService.save(bc);
    }

    /** Primjenjuje predloženu raspodjelu 50/30/20. */
    @Override
    public List<BudgetCategory> applySuggestedBudget(Long budgetId) {
        Budget budget = getById(budgetId);
        if (budget == null) {
            throw new CustomException("Budžet nije pronađen");
        }

        BigDecimal total = getTotalAvailable(budget);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("Budžet mora imati iznos veći od nule.");
        }

        Map<String, BigDecimal> distribution = Map.of(
                "ESSENTIAL", BigDecimal.valueOf(50),
                "OPTIONAL", BigDecimal.valueOf(30),
                "SAVINGS", BigDecimal.valueOf(20)
        );

        List<Category> categories = categoryService.getUserCategories(budget.getUser().getId()).stream()
                .filter(c -> c.getName() != null
                        && !c.getName().equalsIgnoreCase("Prihod")
                        && !c.getName().equalsIgnoreCase("Neraspoređeno"))
                .toList();
        Map<String, List<Category>> grouped = categories.stream()
                .collect(Collectors.groupingBy(c -> c.getType().getName()));

        List<BudgetCategory> saved = new ArrayList<>();

        for (Map.Entry<String, List<Category>> entry : grouped.entrySet()) {
            String type = entry.getKey();
            List<Category> cats = entry.getValue();

            BigDecimal totalPerc = distribution.getOrDefault(type, BigDecimal.ZERO);
            if (cats.isEmpty() || totalPerc.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal perCategoryPerc = totalPerc.divide(BigDecimal.valueOf(cats.size()), 2, RoundingMode.HALF_UP);

            for (Category c : cats) {
                BigDecimal amount = total.multiply(perCategoryPerc).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                BudgetCategory bc = budgetCategoryRepository.findByBudgetIdAndCategoryId(budget.getId(), c.getId());
                if (bc == null) {
                    bc = new BudgetCategory();
                    bc.setBudget(budget);
                    bc.setCategory(c);
                }
                bc.setPercentage(perCategoryPerc);
                bc.setAllocatedAmount(amount);

                saved.add(budgetCategoryService.save(bc));
            }
        }

        return saved;
    }
}
