package com.example.budgetFlow.DTO;

import com.example.budgetFlow.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class NotificationRequest {

    @NotBlank(message = "Naslov je obavezan")
    @Size(max = 120, message = "Naslov može imati maksimalno 120 karaktera")
    private String title;

    @NotBlank(message = "Poruka je obavezna")
    @Size(max = 500, message = "Poruka može imati maksimalno 500 karaktera")
    private String message;

    /** Default: BILL_REMINDER */
    private NotificationType type = NotificationType.BILL_REMINDER;

    /** Za podsjetnike (npr. datum plaćanja računa). */
    private LocalDate dueDate;
}
