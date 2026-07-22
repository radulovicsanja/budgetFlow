package com.example.budgetFlow.DTO;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class CsvImportResultDTO {

    private int importedCount;
    private int failedCount;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
