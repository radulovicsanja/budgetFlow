package com.example.budgetFlow.service;

import com.example.budgetFlow.DTO.CsvImportResultDTO;
import com.example.budgetFlow.entity.Category;
import com.example.budgetFlow.entity.Transaction;
import com.example.budgetFlow.entity.TransactionType;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.BudgetRepository;
import com.example.budgetFlow.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Uvoz transakcija iz CSV-a (budget,category,amount,date…). */
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,                 // 2026-07-15
            DateTimeFormatter.ofPattern("d.M.yyyy"),          // 15.7.2026
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),        // 15.07.2026
            DateTimeFormatter.ofPattern("d/M/yyyy"),          // 15/7/2026
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),        // 15/07/2026
            DateTimeFormatter.ofPattern("d-M-yyyy"),          // 15-7-2026
            DateTimeFormatter.ofPattern("dd-MM-yyyy")         // 15-07-2026
    );

    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final TransactionService transactionService;
    private final UserService userService;
    private final DefaultCategoryService defaultCategoryService;

    public byte[] getTemplateCsv() {
        String month = YearMonth.now().format(MONTH_FORMAT);
        // Datumi u navodnicima + UTF-8 BOM da Excel ne pokvari format
        String template = ""
                + "budget,category,amount,date,type,description\n"
                + month + ",Hrana,25.50,\"" + month + "-15\",Trošak,Market\n"
                + month + ",Prihod,100.00,\"" + month + "-01\",Prihod,Plata\n";
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = template.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(body, 0, out, bom.length, body.length);
        return out;
    }

    public CsvImportResultDTO importTransactions(MultipartFile file, boolean confirmFromUnallocated) {
        if (file == null || file.isEmpty()) {
            throw new CustomException("CSV fajl je obavezan.");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".csv")) {
            throw new CustomException("Dozvoljen je samo .csv fajl.");
        }

        User user = userService.getCurrentUser();
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int failed = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean headerSkipped = false;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = stripBom(line).trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (!headerSkipped) {
                    headerSkipped = true;
                    if (isHeader(line)) {
                        continue;
                    }
                }

                try {
                    importRow(user, line, confirmFromUnallocated);
                    imported++;
                } catch (Exception ex) {
                    failed++;
                    errors.add("Red " + lineNumber + ": " + ex.getMessage());
                }
            }
        } catch (CustomException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CustomException("Greška pri čitanju CSV fajla: " + ex.getMessage());
        }

        if (imported == 0 && failed == 0) {
            throw new CustomException("CSV fajl ne sadrži podatke za uvoz.");
        }

        return CsvImportResultDTO.builder()
                .importedCount(imported)
                .failedCount(failed)
                .errors(errors)
                .build();
    }

    private void importRow(User user, String line, boolean confirmFromUnallocated) {
        char delimiter = detectDelimiter(line);
        String[] cols = splitCsv(line, delimiter);
        if (cols.length < 4) {
            throw new CustomException("Očekivane kolone: budget,category,amount,date[,type][,description]");
        }

        String budgetMonth = cols[0].trim().replace("\"", "");
        String categoryName = cols[1].trim().replace("\"", "");
        String amountRaw = cols[2].trim().replace("\"", "");
        String dateRaw = cols[3].trim().replace("\"", "");
        String typeRaw = cols.length > 4 ? cols[4].trim().replace("\"", "") : "";
        String description = cols.length > 5 ? cols[5].trim().replace("\"", "") : "CSV import";

        validateMonth(budgetMonth);

        LocalDate date = parseDate(dateRaw);

        String dateMonth = date.format(MONTH_FORMAT);
        if (!budgetMonth.equals(dateMonth)) {
            throw new CustomException(
                    "Kolona budget (" + budgetMonth + ") ne odgovara mjesecu datuma (" + dateMonth + ")."
            );
        }

        if (!budgetRepository.existsByUserIdAndMonth(user.getId(), budgetMonth)) {
            throw new CustomException("Budžet za mjesec " + budgetMonth + " ne postoji. Prvo kreiraj budžet.");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountRaw.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new CustomException("Neispravan iznos: " + amountRaw);
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("Iznos mora biti pozitivan.");
        }

        TransactionType type = typeRaw.isBlank()
                ? TransactionType.EXPENSE
                : TransactionType.from(typeRaw);

        Category category;
        if (type == TransactionType.INCOME) {
            category = defaultCategoryService.getOrCreateIncomeCategory(user);
        } else {
            category = categoryRepository
                    .findByUserIdAndNameIgnoreCase(user.getId(), categoryName)
                    .orElseThrow(() -> new CustomException(
                            "Kategorija nije pronađena: " + categoryName + ". Koristi tačan naziv (npr. Hrana)."
                    ));
        }

        Transaction transaction = new Transaction(
                amount,
                type,
                description.isBlank() ? "CSV import" : description,
                date,
                user,
                category
        );

        transactionService.save(transaction, confirmFromUnallocated);
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new CustomException("Datum je obavezan (npr. 2026-07-15 ili 15.07.2026).");
        }
        String value = raw.trim();
        // Excel ponekad doda vrijeme: 2026-07-15 00:00:00
        if (value.contains(" ")) {
            value = value.split(" ")[0];
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        throw new CustomException(
                "Datum mora biti YYYY-MM-DD ili DD.MM.YYYY (dobijeno: " + raw + ")."
        );
    }

    private char detectDelimiter(String line) {
        int commas = 0;
        int semis = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ',') commas++;
            if (c == ';') semis++;
        }
        return semis > commas ? ';' : ',';
    }

    private void validateMonth(String month) {
        try {
            YearMonth.parse(month, MONTH_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new CustomException("Budget (mjesec) mora biti u formatu YYYY-MM.");
        }
    }

    private boolean isHeader(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.startsWith("budget") && lower.contains("category");
    }

    private String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    private String[] splitCsv(String line, char delimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == delimiter && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }
}
