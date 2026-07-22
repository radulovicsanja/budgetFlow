package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.CsvImportResultDTO;
import com.example.budgetFlow.DTO.MonthCompareDTO;
import com.example.budgetFlow.DTO.MonthTrendDTO;
import com.example.budgetFlow.DTO.UserReportDTO;
import com.example.budgetFlow.entity.User_report;
import com.example.budgetFlow.service.CsvImportService;
import com.example.budgetFlow.service.UserReportService;
import com.example.budgetFlow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Reports", description = "Financial summaries, statistics, and CSV import/export")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class UserReportController {

    private final UserReportService userReportService;
    private final UserService userService;
    private final CsvImportService csvImportService;

    @Operation(summary = "Get financial summary", description = "Returns income/expense summary for the authenticated user in a given month (YYYY-MM)")
    @ApiResponse(responseCode = "200", description = "Summary returned")
    @GetMapping("/summary")
    public ResponseEntity<UserReportDTO> getSummary(
            @Parameter(description = "Month (YYYY-MM)", required = true) @RequestParam String month
    ) {
        Long userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(userReportService.getSummary(userId, month));
    }

    @Operation(summary = "Get category statistics", description = "Returns spending statistics per category for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Category statistics returned")
    @GetMapping("/category")
    public ResponseEntity<List<UserReportDTO>> getCategoryStats(
            @Parameter(description = "Month (YYYY-MM)", required = true) @RequestParam String month
    ) {
        Long userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(userReportService.getCategoryStatistics(userId, month));
    }

    @Operation(summary = "Trend for last N months (income from budget total+additional, expenses from transactions)")
    @GetMapping("/trend")
    public ResponseEntity<List<MonthTrendDTO>> getTrend(
            @RequestParam(defaultValue = "6") int months,
            @Parameter(description = "End month YYYY-MM (default: current)") @RequestParam(required = false) String month
    ) {
        Long userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(userReportService.getTrend(userId, months, month));
    }

    @Operation(summary = "Compare selected month with previous month")
    @GetMapping("/compare")
    public ResponseEntity<MonthCompareDTO> compare(
            @Parameter(description = "Month (YYYY-MM)", required = true) @RequestParam String month
    ) {
        Long userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(userReportService.compareWithPreviousMonth(userId, month));
    }

    @Operation(
            summary = "Generate monthly report",
            description = "Generates and persists a monthly report (JSON for UI preview)"
    )
    @ApiResponse(responseCode = "200", description = "Monthly report generated")
    @GetMapping("/monthly")
    public ResponseEntity<User_report> generateMonthly(
            @Parameter(description = "Month (YYYY-MM)", required = true) @RequestParam String month
    ) {
        Long userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(userReportService.generateMonthlyReport(userId, month));
    }

    @Operation(summary = "Download monthly report CSV")
    @ApiResponse(responseCode = "200", description = "CSV monthly report returned")
    @GetMapping(value = "/monthly/download", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> downloadMonthly(
            @Parameter(description = "Month (YYYY-MM)", required = true) @RequestParam String month
    ) {
        Long userId = userService.getCurrentUser().getId();
        byte[] csv = userReportService.generateAndExportMonthlyReport(userId, month);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"mjesecni-izvjestaj-" + month + ".csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csv);
    }

    @Operation(summary = "Export report to CSV", description = "Downloads a CSV file of the monthly report")
    @ApiResponse(responseCode = "200", description = "CSV file returned")
    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> export(
            @Parameter(description = "Month (YYYY-MM)", required = true) @RequestParam String month
    ) {
        Long userId = userService.getCurrentUser().getId();
        byte[] csv = userReportService.exportReportToCSV(userId, month);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"transakcije-" + month + ".csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csv);
    }

    @Operation(
            summary = "Download CSV import template",
            description = "Template columns: budget,category,amount,date,type,description"
    )
    @ApiResponse(responseCode = "200", description = "CSV template returned")
    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=budgetflow-import-template.csv")
                .contentType(new MediaType("text", "csv"))
                .body(csvImportService.getTemplateCsv());
    }

    @Operation(
            summary = "Import transactions from CSV",
            description = "Upload CSV with columns: budget,category,amount,date[,type][,description]. " +
                    "budget = YYYY-MM (must match an existing monthly budget). " +
                    "confirmFromUnallocated=true allows taking shortage from unallocated budget."
    )
    @ApiResponse(responseCode = "200", description = "Import finished")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CsvImportResultDTO> importCsv(
            @Parameter(description = "CSV file") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Take shortage from unallocated if category is exceeded")
            @RequestParam(defaultValue = "true") boolean confirmFromUnallocated
    ) {
        return ResponseEntity.ok(csvImportService.importTransactions(file, confirmFromUnallocated));
    }

    @Operation(summary = "Get all my reports")
    @ApiResponse(responseCode = "200", description = "List of reports")
    @GetMapping("/me")
    public ResponseEntity<List<User_report>> getMyReports() {
        Long userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(userReportService.getReportsByUser(userId));
    }

    @Operation(summary = "Get all reports for user", description = "Only allowed for the authenticated user's own ID")
    @ApiResponse(responseCode = "200", description = "List of reports")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<User_report>> getAllReports(@Parameter(description = "User ID") @PathVariable Long userId) {
        userService.assertOwnership(userId);
        return ResponseEntity.ok(userReportService.getReportsByUser(userId));
    }

    @Operation(summary = "Get report by ID")
    @ApiResponse(responseCode = "200", description = "Report found")
    @GetMapping("/{id}")
    public ResponseEntity<User_report> getById(@Parameter(description = "Report ID") @PathVariable Long id) {
        User_report report = userReportService.getReportById(id);
        userService.assertOwnership(report.getUserId());
        return ResponseEntity.ok(report);
    }

    @Operation(summary = "Delete report")
    @ApiResponse(responseCode = "200", description = "Report deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@Parameter(description = "Report ID") @PathVariable Long id) {
        User_report report = userReportService.getReportById(id);
        userService.assertOwnership(report.getUserId());
        userReportService.deleteReport(id);
        return ResponseEntity.ok("Report obrisan");
    }
}
