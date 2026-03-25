package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.UserReportDTO;
import com.example.budgetFlow.entity.User_report;
import com.example.budgetFlow.service.UserReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class UserReportController {

    private final UserReportService userReportService;

    // 1️⃣ SUMMARY (finansijsko stanje)
    @GetMapping("/summary")
    public ResponseEntity<UserReportDTO> getSummary(
            @RequestParam Long userId,
            @RequestParam String month
    ) {
        return ResponseEntity.ok(
                userReportService.getSummary(userId, month)
        );
    }

    // 2️⃣ STATISTIKA PO KATEGORIJAMA
    @GetMapping("/category")
    public ResponseEntity<List<UserReportDTO>> getCategoryStats(
            @RequestParam Long userId,
            @RequestParam String month
    ) {
        return ResponseEntity.ok(
                userReportService.getCategoryStatistics(userId, month)
        );
    }

    // 3️⃣ GENERISANJE MJESEČNOG IZVJEŠTAJA
    @GetMapping("/monthly")
    public ResponseEntity<User_report> generateMonthly(
            @RequestParam Long userId,
            @RequestParam String month
    ) {
        return ResponseEntity.ok(
                userReportService.generateMonthlyReport(userId, month)
        );
    }

    // 4️⃣ EXPORT CSV
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam Long userId,
            @RequestParam String month
    ) {
        return ResponseEntity.ok(
                userReportService.exportReportToCSV(userId, month)
        );
    }

    // 5️⃣ GET SVI REPORTI
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<User_report>> getAllReports(@PathVariable Long userId) {
        return ResponseEntity.ok(
                userReportService.getReportsByUser(userId)
        );
    }

    // 6️⃣ GET PO ID
    @GetMapping("/{id}")
    public ResponseEntity<User_report> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                userReportService.getReportById(id)
        );
    }

    // 7️⃣ DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        userReportService.deleteReport(id);
        return ResponseEntity.ok("Report obrisan");
    }
}