package com.example.budgetFlow.repository;

import com.example.budgetFlow.entity.User_report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserReportRepository extends JpaRepository<User_report, Long> {

    List<User_report> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
