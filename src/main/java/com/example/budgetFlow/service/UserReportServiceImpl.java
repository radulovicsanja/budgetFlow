package com.example.budgetFlow.service;

import com.example.budgetFlow.DTO.MonthCompareDTO;
import com.example.budgetFlow.DTO.MonthTrendDTO;
import com.example.budgetFlow.DTO.UserReportDTO;
import com.example.budgetFlow.entity.Budget;
import com.example.budgetFlow.entity.Transaction;
import com.example.budgetFlow.entity.TransactionType;
import com.example.budgetFlow.entity.User_report;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.BudgetRepository;
import com.example.budgetFlow.repository.UserReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Izvještaji, trend i CSV export. */
@Service
@RequiredArgsConstructor
public class UserReportServiceImpl implements UserReportService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final UserReportRepository userReportRepository;
    private final TransactionService transactionService;
    private final BudgetRepository budgetRepository;

    @Override
    public User_report getReportById(Long id) {
        return userReportRepository.findById(id)
                .orElseThrow(() -> new CustomException("Izvještaj nije pronađen."));
    }

    @Override
    public List<User_report> getReportsByUser(Long userId) {
        return userReportRepository.findByUserId(userId);
    }

    @Override
    public User_report getReportByUserAndMonth(Long userId, String month) {
        String m = normalizeMonth(month);

        return userReportRepository.findByUserId(userId).stream()
                .filter(r -> belongsToMonth(r.getReportDate(), m))
                .findFirst()
                .orElseThrow(() -> new CustomException(
                        "Izvještaj za mjesec " + m + " nije pronađen."
                ));
    }

    @Override
    public User_report updateReport(Long id, UserReportDTO dto) {
        User_report report = getReportById(id);

        report.setTotalIncome(dto.getTotalIncome());
        report.setTotalExpenses(dto.getTotalExpenses());
        report.setTotalSavings(dto.getTotalSavings());
        report.setCategoryBreakdown(dto.getCategoryBreakdown());

        return userReportRepository.save(report);
    }

    @Override
    public void deleteReport(Long id) {
        userReportRepository.deleteById(id);
    }

    @Override
    public UserReportDTO getSummary(Long userId, String month) {
        String m = normalizeMonth(month);
        List<Transaction> transactions = getTransactionsForMonth(userId, m);

        BigDecimal income = calculateTotalIncome(userId, m, transactions);
        BigDecimal expenses = calculateExpenses(transactions);

        UserReportDTO dto = new UserReportDTO();
        dto.setUserId(userId);
        dto.setTotalIncome(income);
        dto.setTotalExpenses(expenses);
        dto.setTotalSavings(income.subtract(expenses));
        dto.setCategoryBreakdown(buildCategoryBreakdown(transactions));

        return dto;
    }

    @Override
    public List<UserReportDTO> getCategoryStatistics(Long userId, String month) {
        String m = normalizeMonth(month);
        List<Transaction> transactions = getTransactionsForMonth(userId, m);

        return transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        this::categoryName,
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    UserReportDTO dto = new UserReportDTO();
                    dto.setUserId(userId);
                    dto.setCategoryName(entry.getKey());
                    dto.setAmount(entry.getValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MonthTrendDTO> getTrend(Long userId, int months) {
        return getTrend(userId, months, null);
    }

    @Override
    public List<MonthTrendDTO> getTrend(Long userId, int months, String endMonth) {
        int count = Math.min(Math.max(months, 1), 24);
        YearMonth end = endMonth != null && !endMonth.isBlank()
                ? YearMonth.parse(normalizeMonth(endMonth), MONTH_FORMAT)
                : YearMonth.now();
        List<MonthTrendDTO> result = new ArrayList<>();

        for (int i = count - 1; i >= 0; i--) {
            String m = end.minusMonths(i).format(MONTH_FORMAT);
            List<Transaction> txs = getTransactionsForMonth(userId, m);
            // Prihod strogo iz budžeta: totalAmount + additionalIncome (bez budžeta = 0)
            BigDecimal income = calculateBudgetIncome(userId, m);
            BigDecimal expenses = calculateExpenses(txs);
            result.add(MonthTrendDTO.builder()
                    .month(m)
                    .income(income)
                    .expenses(expenses)
                    .savings(income.subtract(expenses))
                    .build());
        }
        return result;
    }

    @Override
    public MonthCompareDTO compareWithPreviousMonth(Long userId, String month) {
        String current = normalizeMonth(month);
        String previous = YearMonth.parse(current, MONTH_FORMAT).minusMonths(1).format(MONTH_FORMAT);

        UserReportDTO cur = getSummary(userId, current);
        UserReportDTO prev = getSummary(userId, previous);

        return MonthCompareDTO.builder()
                .currentMonth(current)
                .previousMonth(previous)
                .currentIncome(cur.getTotalIncome())
                .previousIncome(prev.getTotalIncome())
                .incomeChangePercent(percentChange(prev.getTotalIncome(), cur.getTotalIncome()))
                .currentExpenses(cur.getTotalExpenses())
                .previousExpenses(prev.getTotalExpenses())
                .expensesChangePercent(percentChange(prev.getTotalExpenses(), cur.getTotalExpenses()))
                .currentSavings(cur.getTotalSavings())
                .previousSavings(prev.getTotalSavings())
                .savingsChangePercent(percentChange(prev.getTotalSavings(), cur.getTotalSavings()))
                .build();
    }

    private BigDecimal percentChange(BigDecimal previous, BigDecimal current) {
        BigDecimal prev = previous != null ? previous : BigDecimal.ZERO;
        BigDecimal cur = current != null ? current : BigDecimal.ZERO;
        if (prev.compareTo(BigDecimal.ZERO) == 0) {
            if (cur.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
            return BigDecimal.valueOf(100);
        }
        return cur.subtract(prev)
                .multiply(BigDecimal.valueOf(100))
                .divide(prev.abs(), 1, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public User_report generateMonthlyReport(Long userId, String month) {
        String m = normalizeMonth(month);
        List<Transaction> transactions = getTransactionsForMonth(userId, m);

        BigDecimal income = calculateTotalIncome(userId, m, transactions);
        BigDecimal expenses = calculateExpenses(transactions);
        String breakdown = buildCategoryBreakdown(transactions);
        LocalDate reportDate = YearMonth.parse(m, MONTH_FORMAT).atDay(1);

        Optional<User_report> existing = userReportRepository.findByUserId(userId).stream()
                .filter(r -> belongsToMonth(r.getReportDate(), m))
                .findFirst();

        User_report report = existing.orElseGet(User_report::new);
        report.setUserId(userId);
        report.setReportDate(reportDate);
        report.setTotalIncome(income);
        report.setTotalExpenses(expenses);
        report.setTotalSavings(income.subtract(expenses));
        report.setCategoryBreakdown(breakdown);

        return userReportRepository.save(report);
    }

    @Override
    @Transactional
    public byte[] generateAndExportMonthlyReport(Long userId, String month) {
        String m = normalizeMonth(month);
        User_report report = generateMonthlyReport(userId, m);
        List<Transaction> transactions = getTransactionsForMonth(userId, m);
        return toMonthlyCsv(m, report, transactions, userId);
    }

    @Override
    public byte[] exportReportToCSV(Long userId, String month) {
        String m = normalizeMonth(month);
        List<Transaction> transactions = getTransactionsForMonth(userId, m);

        StringBuilder sb = new StringBuilder();
        sb.append("tip;kategorija;iznos;datum\n");
        for (Transaction t : transactions) {
            appendTransactionRow(sb, t);
        }
        if (transactions.isEmpty()) {
            sb.append("Nema transakcija za mjesec ").append(m).append(";;;\n");
        }
        return withBom(sb.toString());
    }

    private byte[] toMonthlyCsv(String month, User_report report, List<Transaction> transactions, Long userId) {
        Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndMonth(userId, month);
        BigDecimal budgetTotal = budgetOpt.map(Budget::getTotalAmount).orElse(BigDecimal.ZERO);
        BigDecimal budgetAdditional = budgetOpt
                .map(b -> b.getAdditionalIncome() != null ? b.getAdditionalIncome() : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);

        StringBuilder sb = new StringBuilder();
        sb.append("Polje;Vrijednost\n");
        sb.append("Mjesec;").append(month).append("\n");
        sb.append("Budžet (osnovni iznos);").append(budgetTotal).append("\n");
        sb.append("Dodatni prihodi;").append(budgetAdditional).append("\n");
        sb.append("Ukupni prihodi;").append(report.getTotalIncome()).append("\n");
        sb.append("Ukupni rashodi;").append(report.getTotalExpenses()).append("\n");
        sb.append("Ušteda;").append(report.getTotalSavings()).append("\n");
        sb.append("Broj transakcija;").append(transactions.size()).append("\n");
        sb.append("\n");
        sb.append("tip;kategorija;iznos;datum\n");

        if (transactions.isEmpty()) {
            sb.append("Nema transakcija za ovaj mjesec;;;\n");
            Set<String> otherMonths = availableMonths(userId);
            if (!otherMonths.isEmpty()) {
                sb.append("Dostupni mjeseci sa transakcijama;")
                        .append(String.join(", ", otherMonths))
                        .append(";;\n");
            }
        } else {
            for (Transaction t : transactions) {
                appendTransactionRow(sb, t);
            }
        }

        return withBom(sb.toString());
    }

    private void appendTransactionRow(StringBuilder sb, Transaction t) {
        String typeLabel = t.getType() == TransactionType.INCOME ? "Prihod" : "Trošak";
        sb.append(typeLabel).append(';')
                .append(escapeCsv(categoryName(t))).append(';')
                .append(t.getAmount()).append(';')
                .append(t.getDate()).append('\n');
    }

    private Set<String> availableMonths(Long userId) {
        return transactionService.getByUserId(userId).stream()
                .map(Transaction::getDate)
                .filter(d -> d != null)
                .map(d -> d.format(MONTH_FORMAT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String categoryName(Transaction t) {
        if (t.getCategory() == null || t.getCategory().getName() == null) {
            return t.getType() == TransactionType.INCOME ? "Prihod" : "Bez kategorije";
        }
        return t.getCategory().getName();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains(",")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private byte[] withBom(String content) {
        byte[] body = content.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, out, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, out, UTF8_BOM.length, body.length);
        return out;
    }

    private List<Transaction> getTransactionsForMonth(Long userId, String month) {
        String m = normalizeMonth(month);
        return transactionService.getByUserId(userId).stream()
                .filter(t -> belongsToMonth(t.getDate(), m))
                .collect(Collectors.toList());
    }

    private String normalizeMonth(String month) {
        if (month == null || month.isBlank()) {
            throw new CustomException("Mjesec je obavezan (format YYYY-MM).");
        }
        String raw = month.trim();
        try {
            return YearMonth.parse(raw, MONTH_FORMAT).format(MONTH_FORMAT);
        } catch (DateTimeParseException ex) {
            // podrška za 2026-7
            try {
                String[] parts = raw.split("-");
                if (parts.length == 2) {
                    int y = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    return YearMonth.of(y, m).format(MONTH_FORMAT);
                }
            } catch (Exception ignored) {
                // fall through
            }
            throw new CustomException("Mjesec mora biti u formatu YYYY-MM, npr. 2026-07.");
        }
    }

    private boolean belongsToMonth(LocalDate date, String month) {
        return date != null && month.equals(date.format(MONTH_FORMAT));
    }

    /** Prihod: budžet (total + additional), inače zbir INCOME transakcija. */
    private BigDecimal calculateTotalIncome(Long userId, String month, List<Transaction> transactions) {
        return budgetRepository.findByUserIdAndMonth(userId, month)
                .map(this::sumBudgetIncome)
                .orElseGet(() -> calculateIncomeFromTransactions(transactions));
    }

    /** Samo budžet: totalAmount + additionalIncome. Bez budžeta → 0. */
    private BigDecimal calculateBudgetIncome(Long userId, String month) {
        return budgetRepository.findByUserIdAndMonth(userId, month)
                .map(this::sumBudgetIncome)
                .orElse(BigDecimal.ZERO);
    }

    /** Sabira osnovni i dodatni prihod iz budžeta. */
    private BigDecimal sumBudgetIncome(Budget budget) {
        BigDecimal total = budget.getTotalAmount() != null
                ? budget.getTotalAmount()
                : BigDecimal.ZERO;
        BigDecimal additional = budget.getAdditionalIncome() != null
                ? budget.getAdditionalIncome()
                : BigDecimal.ZERO;
        return total.add(additional);
    }

    private BigDecimal calculateIncomeFromTransactions(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateExpenses(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String buildCategoryBreakdown(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        this::categoryName,
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ))
                .toString();
    }
}
