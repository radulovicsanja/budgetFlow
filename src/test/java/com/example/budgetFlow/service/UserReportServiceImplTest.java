package com.example.budgetFlow.service;

import com.example.budgetFlow.DTO.UserReportDTO;
import com.example.budgetFlow.entity.*;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.BudgetRepository;
import com.example.budgetFlow.repository.UserReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserReportServiceImplTest {

    @Mock
    private UserReportRepository userReportRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private UserReportServiceImpl userReportService;

    private User user;
    private Category foodCategory;
    private Category transportCategory;
    private User_report report;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .id(1L)
                .username("sanja")
                .email("sanja@mail.com")
                .password("password")
                .build();

        foodCategory = Category.builder()
                .id(10L)
                .name("Hrana")
                .user(user)
                .isDefault(false)
                .build();

        transportCategory = Category.builder()
                .id(11L)
                .name("Prevoz")
                .user(user)
                .isDefault(false)
                .build();

        report = new User_report(
                1L,
                LocalDate.of(2026, 7, 1),
                new BigDecimal("1200.00"),
                new BigDecimal("400.00"),
                new BigDecimal("800.00"),
                "{Hrana=400.00}"
        );

        report.setId(5L);
    }

    @Test
    void getReportById_existingReport_returnsReport() {

        when(userReportRepository.findById(5L))
                .thenReturn(Optional.of(report));

        User_report result = userReportService.getReportById(5L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals(
                0,
                new BigDecimal("1200.00")
                        .compareTo(result.getTotalIncome())
        );

        verify(userReportRepository).findById(5L);
    }

    @Test
    void getReportById_nonExistingReport_throwsCustomException() {

        when(userReportRepository.findById(99L))
                .thenReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> userReportService.getReportById(99L)
        );

        assertEquals(
                "Izvještaj nije pronađen.",
                exception.getMessage()
        );

        verify(userReportRepository).findById(99L);
    }

    @Test
    void getReportsByUser_returnsUserReports() {

        User_report secondReport = new User_report(
                1L,
                LocalDate.of(2026, 6, 1),
                new BigDecimal("1000.00"),
                new BigDecimal("300.00"),
                new BigDecimal("700.00"),
                "{Prevoz=300.00}"
        );

        when(userReportRepository.findByUserId(1L))
                .thenReturn(List.of(report, secondReport));

        List<User_report> result =
                userReportService.getReportsByUser(1L);

        assertEquals(2, result.size());
        assertEquals(report, result.get(0));

        verify(userReportRepository).findByUserId(1L);
    }

    @Test
    void getReportByUserAndMonth_existingReport_returnsReport() {

        when(userReportRepository.findByUserId(1L))
                .thenReturn(List.of(report));

        User_report result =
                userReportService.getReportByUserAndMonth(1L, "2026-07");

        assertNotNull(result);
        assertEquals(LocalDate.of(2026, 7, 1), result.getReportDate());
        assertEquals(5L, result.getId());

        verify(userReportRepository).findByUserId(1L);
    }

    @Test
    void getReportByUserAndMonth_invalidMonth_throwsCustomException() {

        CustomException exception = assertThrows(
                CustomException.class,
                () -> userReportService.getReportByUserAndMonth(
                        1L,
                        "jul-2026"
                )
        );

        assertEquals(
                "Mjesec mora biti u formatu YYYY-MM, npr. 2026-07.",
                exception.getMessage()
        );

        verifyNoInteractions(userReportRepository);
    }

    @Test
    void updateReport_validData_updatesAndSavesReport() {

        UserReportDTO dto = new UserReportDTO();
        dto.setUserId(1L);
        dto.setTotalIncome(new BigDecimal("1500.00"));
        dto.setTotalExpenses(new BigDecimal("500.00"));
        dto.setTotalSavings(new BigDecimal("1000.00"));
        dto.setCategoryBreakdown("{Hrana=300.00, Prevoz=200.00}");

        when(userReportRepository.findById(5L))
                .thenReturn(Optional.of(report));

        when(userReportRepository.save(report))
                .thenReturn(report);

        User_report result =
                userReportService.updateReport(5L, dto);

        assertEquals(
                0,
                new BigDecimal("1500.00")
                        .compareTo(result.getTotalIncome())
        );

        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(result.getTotalExpenses())
        );

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(result.getTotalSavings())
        );

        assertEquals(
                "{Hrana=300.00, Prevoz=200.00}",
                result.getCategoryBreakdown()
        );

        verify(userReportRepository).save(report);
    }

    @Test
    void deleteReport_validId_deletesReport() {

        userReportService.deleteReport(5L);

        verify(userReportRepository).deleteById(5L);
    }

    @Test
    void getSummary_budgetExists_usesBudgetIncomeAndCalculatesExpenses() {

        Budget budget = Budget.builder()
                .id(20L)
                .user(user)
                .month("2026-07")
                .totalAmount(new BigDecimal("1000.00"))
                .additionalIncome(new BigDecimal("200.00"))
                .build();

        Transaction income = transaction(
                new BigDecimal("300.00"),
                TransactionType.INCOME,
                LocalDate.of(2026, 7, 5),
                null
        );

        Transaction foodExpense = transaction(
                new BigDecimal("250.00"),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 7, 10),
                foodCategory
        );

        Transaction transportExpense = transaction(
                new BigDecimal("100.00"),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 7, 15),
                transportCategory
        );

        when(transactionService.getByUserId(1L))
                .thenReturn(List.of(
                        income,
                        foodExpense,
                        transportExpense
                ));

        when(budgetRepository.findByUserIdAndMonth(1L, "2026-07"))
                .thenReturn(Optional.of(budget));

        UserReportDTO result =
                userReportService.getSummary(1L, "2026-07");

        assertEquals(
                0,
                new BigDecimal("1200.00")
                        .compareTo(result.getTotalIncome())
        );

        assertEquals(
                0,
                new BigDecimal("350.00")
                        .compareTo(result.getTotalExpenses())
        );

        assertEquals(
                0,
                new BigDecimal("850.00")
                        .compareTo(result.getTotalSavings())
        );

        assertTrue(result.getCategoryBreakdown().contains("Hrana"));
        assertTrue(result.getCategoryBreakdown().contains("250.00"));
        assertTrue(result.getCategoryBreakdown().contains("Prevoz"));
        assertTrue(result.getCategoryBreakdown().contains("100.00"));
    }

    @Test
    void getSummary_budgetDoesNotExist_usesIncomeTransactions() {

        Transaction incomeOne = transaction(
                new BigDecimal("700.00"),
                TransactionType.INCOME,
                LocalDate.of(2026, 7, 2),
                null
        );

        Transaction incomeTwo = transaction(
                new BigDecimal("300.00"),
                TransactionType.INCOME,
                LocalDate.of(2026, 7, 8),
                null
        );

        Transaction expense = transaction(
                new BigDecimal("250.00"),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 7, 12),
                foodCategory
        );

        when(transactionService.getByUserId(1L))
                .thenReturn(List.of(incomeOne, incomeTwo, expense));

        when(budgetRepository.findByUserIdAndMonth(1L, "2026-07"))
                .thenReturn(Optional.empty());

        UserReportDTO result =
                userReportService.getSummary(1L, "2026-07");

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(result.getTotalIncome())
        );

        assertEquals(
                0,
                new BigDecimal("250.00")
                        .compareTo(result.getTotalExpenses())
        );

        assertEquals(
                0,
                new BigDecimal("750.00")
                        .compareTo(result.getTotalSavings())
        );
    }

    @Test
    void getCategoryStatistics_groupsExpensesByCategory() {

        Transaction foodExpenseOne = transaction(
                new BigDecimal("100.00"),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 7, 5),
                foodCategory
        );

        Transaction foodExpenseTwo = transaction(
                new BigDecimal("150.00"),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 7, 10),
                foodCategory
        );

        Transaction transportExpense = transaction(
                new BigDecimal("80.00"),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 7, 15),
                transportCategory
        );

        Transaction income = transaction(
                new BigDecimal("1000.00"),
                TransactionType.INCOME,
                LocalDate.of(2026, 7, 1),
                null
        );

        when(transactionService.getByUserId(1L))
                .thenReturn(List.of(
                        foodExpenseOne,
                        foodExpenseTwo,
                        transportExpense,
                        income
                ));

        List<UserReportDTO> result =
                userReportService.getCategoryStatistics(1L, "2026-07");

        assertEquals(2, result.size());

        UserReportDTO foodStatistics = result.stream()
                .filter(dto -> "Hrana".equals(dto.getCategoryName()))
                .findFirst()
                .orElseThrow();

        UserReportDTO transportStatistics = result.stream()
                .filter(dto -> "Prevoz".equals(dto.getCategoryName()))
                .findFirst()
                .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("250.00")
                        .compareTo(foodStatistics.getAmount())
        );

        assertEquals(
                0,
                new BigDecimal("80.00")
                        .compareTo(transportStatistics.getAmount())
        );
    }

    @Test
    void generateMonthlyReport_validData_createsAndSavesReport() {

        Budget budget = Budget.builder()
                .id(20L)
                .user(user)
                .month("2026-07")
                .totalAmount(new BigDecimal("1000.00"))
                .additionalIncome(new BigDecimal("200.00"))
                .build();

        Transaction foodExpense = transaction(
                new BigDecimal("300.00"),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 7, 10),
                foodCategory
        );

        when(transactionService.getByUserId(1L))
                .thenReturn(List.of(foodExpense));

        when(budgetRepository.findByUserIdAndMonth(1L, "2026-07"))
                .thenReturn(Optional.of(budget));

        when(userReportRepository.findByUserId(1L))
                .thenReturn(List.of());

        when(userReportRepository.save(any(User_report.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User_report result =
                userReportService.generateMonthlyReport(1L, "2026-07");

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(LocalDate.of(2026, 7, 1), result.getReportDate());

        assertEquals(
                0,
                new BigDecimal("1200.00")
                        .compareTo(result.getTotalIncome())
        );

        assertEquals(
                0,
                new BigDecimal("300.00")
                        .compareTo(result.getTotalExpenses())
        );

        assertEquals(
                0,
                new BigDecimal("900.00")
                        .compareTo(result.getTotalSavings())
        );

        assertTrue(result.getCategoryBreakdown().contains("Hrana"));

        ArgumentCaptor<User_report> captor =
                ArgumentCaptor.forClass(User_report.class);

        verify(userReportRepository).save(captor.capture());

        assertEquals(
                LocalDate.of(2026, 7, 1),
                captor.getValue().getReportDate()
        );
    }

    private Transaction transaction(
            BigDecimal amount,
            TransactionType type,
            LocalDate date,
            Category category
    ) {
        return new Transaction(
                amount,
                type,
                "Test transakcija",
                date,
                user,
                category
        );
    }
}