package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.UserReportDTO;
import com.example.budgetFlow.entity.User_report;
import com.example.budgetFlow.service.UserReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Reports", description = "Financial summaries, statistics, and CSV exports")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class UserReportController {

    private final UserReportService userReportService;

    // 1️⃣ SUMMARY (finansijsko stanje)
    @Operation(summary = "Get financial summary", description = "Returns income/expense summary for a user in a given month (format: YYYY-MM)")
    @ApiResponse(responseCode = "200", description = "Summary returned")
    @GetMapping("/summary")
    public ResponseEntity<UserReportDTO> getSummary(
            @Parameter(description = "User ID", required = true) @RequestParam Long userId,
            @Parameter(description = "Month (YYYY-MM)", required = true) @RequestParam String month
    ) {
        return ResponseEntity.ok(
                userReportService.getSummary(userId, month)
        );
    }

    // 2️⃣ STATISTIKA PO KATEGORIJAMA
    @Operation(summary = "Get category statistics", description = "Returns spending statistics per category for a user in a given month")
    @ApiResponse(responseCode = "200", description = "Category statistics returned")
    @GetMapping("/category")
    public ResponseEntity<List<UserReportDTO>> getCategoryStats(
            @Parameter(description = "User ID", required = true) @RequestParam Long userId,
            @Parameter(description = "Month (YYYY-MM)", required = true) @RequestParam String month
    ) {
        return ResponseEntity.ok(
                userReportService.getCategoryStatistics(userId, month)
        );
    }

    // 3️⃣ GENERISANJE MJESEČNOG IZVJEŠTAJA
    @Operation(summary = "Generate monthly report", description = "Generates and persists a monthly report for a user")
    @ApiResponse(responseCode = "200", description = "Monthly report generated")
    @GetMapping("/monthly")
    public ResponseEntity<User_report> generateMonthly(
            @Parameter(description = "User ID", required = true) @RequestParam Long userId,
            @Parameter(description = "Month (YYYY-MM)", required = true) @RequestParam String month
    ) {
        return ResponseEntity.ok(
                userReportService.generateMonthlyReport(userId, month)
        );
    }

    // 4️⃣ EXPORT CSV
    @Operation(summary = "Export report to CSV", description = "Downloads a CSV file of the monthly report")
    @ApiResponse(responseCode = "200", description = "CSV file returned")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @Parameter(description = "User ID", required = true) @RequestParam Long userId,
            @Parameter(description = "Month (YYYY-MM)", required = true) @RequestParam String month
    ) {
        return ResponseEntity.ok(
                userReportService.exportReportToCSV(userId, month)
        );
    }

    // 5️⃣ GET SVI REPORTI
    @Operation(summary = "Get all reports for user")
    @ApiResponse(responseCode = "200", description = "List of reports")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<User_report>> getAllReports(@Parameter(description = "User ID") @PathVariable Long userId) {
        return ResponseEntity.ok(
                userReportService.getReportsByUser(userId)
        );
    }

    // 6️⃣ GET PO ID
    @Operation(summary = "Get report by ID")
    @ApiResponse(responseCode = "200", description = "Report found")
    @GetMapping("/{id}")
    public ResponseEntity<User_report> getById(@Parameter(description = "Report ID") @PathVariable Long id) {
        return ResponseEntity.ok(
                userReportService.getReportById(id)
        );
    }

    // 7️⃣ DELETE
    @Operation(summary = "Delete report")
    @ApiResponse(responseCode = "200", description = "Report deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@Parameter(description = "Report ID") @PathVariable Long id) {
        userReportService.deleteReport(id);
        return ResponseEntity.ok("Report obrisan");
    }
}