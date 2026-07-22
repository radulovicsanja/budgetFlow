package com.example.budgetFlow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BudgetFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(BudgetFlowApplication.class, args);
	}
}
