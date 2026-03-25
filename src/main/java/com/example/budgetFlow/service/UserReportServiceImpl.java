package com.example.budgetFlow.service;

import com.example.budgetFlow.DTO.UserReportDTO;
import com.example.budgetFlow.entity.Transaction;
import com.example.budgetFlow.entity.User_report;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.UserReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserReportServiceImpl implements UserReportService {

    private final UserReportRepository userReportRepository;
    private final TransactionService transactionService;

    @Override
    public User_report getReportById(Long id) {
        return userReportRepository.findById(id)
                .orElseThrow(() -> new CustomException("Izvjestaj nije pronađen"));
    }

    @Override
    public List<User_report> getReportsByUser(Long userId) {
        return userReportRepository.findByUserId(userId);
    }

    @Override
    public User_report getReportByUserAndMonth(Long userId, String month) {
        return userReportRepository.findByUserId(userId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new CustomException("Izvjestaj nije pronađen"));
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

    // ✅ SUMMARY
    @Override
    public UserReportDTO getSummary(Long userId, String month) {

        List<Transaction> transactions = transactionService.getByUserId(userId);

        BigDecimal income = calculateIncome(transactions);
        BigDecimal expenses = calculateExpenses(transactions);

        UserReportDTO dto = new UserReportDTO();
        dto.setUserId(userId);
        dto.setTotalIncome(income);
        dto.setTotalExpenses(expenses);
        dto.setTotalSavings(income.subtract(expenses));

        return dto;
    }

    // CATEGORY STATS
    @Override
    public List<UserReportDTO> getCategoryStatistics(Long userId, String month) {

        List<Transaction> transactions = transactionService.getByUserId(userId);

        return transactions.stream()
                .filter(t -> t.getType().equalsIgnoreCase("EXPENSE"))
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    UserReportDTO dto = new UserReportDTO();
                    dto.setCategoryName(entry.getKey());
                    dto.setAmount(entry.getValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // GENERATE REPORT
    @Override
    public User_report generateMonthlyReport(Long userId, String month) {

        List<Transaction> transactions = transactionService.getByUserId(userId);

        BigDecimal income = calculateIncome(transactions);
        BigDecimal expenses = calculateExpenses(transactions);

        User_report report = new User_report();
        report.setUserId(userId);
        report.setReportDate(LocalDate.now());
        report.setTotalIncome(income);
        report.setTotalExpenses(expenses);
        report.setTotalSavings(income.subtract(expenses));
        report.setCategoryBreakdown(getCategoryBreakdown(userId));

        return userReportRepository.save(report);
    }

    // EXPORT CSV
    @Override
    public byte[] exportReportToCSV(Long userId, String month) {

        List<Transaction> transactions = transactionService.getByUserId(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("Type,Category,Amount,Date\n");

        for (Transaction t : transactions) {
            sb.append(t.getType()).append(",")
                    .append(t.getCategory().getName()).append(",")
                    .append(t.getAmount()).append(",")
                    .append(t.getDate()).append("\n");
        }

        return sb.toString().getBytes();
    }

    // INCOME
    private BigDecimal calculateIncome(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getType().equalsIgnoreCase("INCOME"))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateExpenses(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getType().equalsIgnoreCase("EXPENSE"))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String getCategoryBreakdown(Long userId) {
        return transactionService.getByUserId(userId)
                .stream()
                .filter(t -> t.getType().equalsIgnoreCase("EXPENSE"))
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ))
                .toString();
    }
}