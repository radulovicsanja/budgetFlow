package com.example.budgetFlow.service;

import com.example.budgetFlow.DTO.UserReportDTO;
import com.example.budgetFlow.entity.User_report;

import java.util.List;

public interface UserReportService {

    User_report getReportById(Long id);

    List<User_report> getReportsByUser(Long userId);

    User_report getReportByUserAndMonth(Long userId, String month);

    User_report updateReport(Long id, UserReportDTO dto);

    void deleteReport(Long id);

    UserReportDTO getSummary(Long userId, String month);

    List<UserReportDTO> getCategoryStatistics(Long userId, String month);

    User_report generateMonthlyReport(Long userId, String month);

    byte[] exportReportToCSV(Long userId, String month);
}