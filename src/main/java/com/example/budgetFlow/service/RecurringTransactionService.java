package com.example.budgetFlow.service;

import com.example.budgetFlow.DTO.RecurringTransactionRequest;
import com.example.budgetFlow.entity.*;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.RecurringTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ponavljajuće transakcije i dnevni job. */
@Service
public class RecurringTransactionService {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionService.class);
    private static final int MAX_CATCH_UP = 3;

    private final RecurringTransactionRepository repository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final DefaultCategoryService defaultCategoryService;
    private final TransactionService transactionService;
    private final TransactionTemplate requiresNewTx;

    public RecurringTransactionService(
            RecurringTransactionRepository repository,
            UserService userService,
            CategoryService categoryService,
            DefaultCategoryService defaultCategoryService,
            TransactionService transactionService,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.userService = userService;
        this.categoryService = categoryService;
        this.defaultCategoryService = defaultCategoryService;
        this.transactionService = transactionService;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public List<Map<String, Object>> listMine() {
        Long userId = userService.getCurrentUser().getId();
        return repository.findByUserIdOrderByNextRunDateAsc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public Map<String, Object> create(RecurringTransactionRequest request) {
        User user = userService.getCurrentUser();
        Category category = resolveCategory(request, user);
        int day = request.getDayOfMonth();

        RecurringTransaction rt = RecurringTransaction.builder()
                .user(user)
                .amount(request.getAmount())
                .type(request.getType())
                .description(request.getDescription())
                .category(category)
                .dayOfMonth(day)
                .active(request.getActive() == null || request.getActive())
                .nextRunDate(computeNextRun(LocalDate.now(), day))
                .build();

        return toDto(repository.save(rt));
    }

    @Transactional
    public Map<String, Object> update(Long id, RecurringTransactionRequest request) {
        RecurringTransaction rt = getOwned(id);
        User user = userService.getCurrentUser();
        rt.setAmount(request.getAmount());
        rt.setType(request.getType());
        rt.setDescription(request.getDescription());
        rt.setCategory(resolveCategory(request, user));
        rt.setDayOfMonth(request.getDayOfMonth());
        if (request.getActive() != null) {
            rt.setActive(request.getActive());
        }
        rt.setNextRunDate(computeNextRun(LocalDate.now(), request.getDayOfMonth()));
        return toDto(repository.save(rt));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOwned(id));
    }

    /** Svaka stavka u zasebnoj transakciji — greška ne ruši ostale. */
    public void processDue() {
        LocalDate today = LocalDate.now();
        List<Long> dueIds = repository.findByActiveTrueAndNextRunDateLessThanEqual(today).stream()
                .map(RecurringTransaction::getId)
                .toList();

        for (Long id : dueIds) {
            try {
                requiresNewTx.executeWithoutResult(status -> processOne(id, today));
            } catch (Exception ex) {
                log.warn("Ponavljajuća transakcija {} nije kreirana: {}", id, ex.getMessage());
            }
        }
    }

    private void processOne(Long id, LocalDate today) {
        RecurringTransaction rt = repository.findById(id)
                .orElseThrow(() -> new CustomException("Ponavljajuća transakcija nije pronađena."));
        if (!rt.isActive()) {
            return;
        }

        int created = 0;
        while (rt.getNextRunDate() != null
                && !rt.getNextRunDate().isAfter(today)
                && created < MAX_CATCH_UP) {

            LocalDate runDate = rt.getNextRunDate();
            Transaction tx = new Transaction(
                    rt.getAmount(),
                    rt.getType(),
                    rt.getDescription() != null ? rt.getDescription() : "Ponavljajuća transakcija",
                    runDate,
                    rt.getUser(),
                    rt.getCategory()
            );
            // EXPENSE: confirmFromUnallocated da job ne blokira
            transactionService.save(tx, true);

            rt.setNextRunDate(computeNextRun(runDate.plusDays(1), rt.getDayOfMonth()));
            rt = repository.save(rt);
            created++;
        }
    }

    private Category resolveCategory(RecurringTransactionRequest request, User user) {
        if (request.getType() == TransactionType.INCOME) {
            return defaultCategoryService.getOrCreateIncomeCategory(user);
        }
        if (request.getCategoryId() == null) {
            throw new CustomException("Za trošak moraš odabrati kategoriju.");
        }
        Category category = categoryService.getById(request.getCategoryId());
        userService.assertOwnership(category.getUser().getId());
        return category;
    }

    private LocalDate computeNextRun(LocalDate from, int dayOfMonth) {
        int day = Math.min(Math.max(dayOfMonth, 1), 28);
        LocalDate thisMonth = from.withDayOfMonth(Math.min(day, from.lengthOfMonth()));
        if (!thisMonth.isBefore(from)) {
            return thisMonth;
        }
        LocalDate next = from.plusMonths(1);
        return next.withDayOfMonth(Math.min(day, next.lengthOfMonth()));
    }

    private RecurringTransaction getOwned(Long id) {
        RecurringTransaction rt = repository.findById(id)
                .orElseThrow(() -> new CustomException("Ponavljajuća transakcija nije pronađena."));
        userService.assertOwnership(rt.getUser().getId());
        return rt;
    }

    private Map<String, Object> toDto(RecurringTransaction rt) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rt.getId());
        map.put("amount", rt.getAmount());
        map.put("type", rt.getType());
        map.put("description", rt.getDescription());
        map.put("categoryId", rt.getCategory() != null ? rt.getCategory().getId() : null);
        map.put("categoryName", rt.getCategory() != null ? rt.getCategory().getName() : null);
        map.put("dayOfMonth", rt.getDayOfMonth());
        map.put("active", rt.isActive());
        map.put("nextRunDate", rt.getNextRunDate());
        return map;
    }
}
