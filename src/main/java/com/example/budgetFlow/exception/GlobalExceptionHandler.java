package com.example.budgetFlow.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
            Map.entry("amount", "Iznos"),
            Map.entry("type", "Tip transakcije"),
            Map.entry("date", "Datum"),
            Map.entry("description", "Opis"),
            Map.entry("categoryId", "Kategorija"),
            Map.entry("month", "Mjesec"),
            Map.entry("totalAmount", "Ukupan iznos"),
            Map.entry("additionalIncome", "Dodatni prihod"),
            Map.entry("percentage", "Procenat"),
            Map.entry("allocatedAmount", "Iznos raspodjele"),
            Map.entry("email", "Email"),
            Map.entry("password", "Lozinka"),
            Map.entry("username", "Korisničko ime"),
            Map.entry("oldPassword", "Stara lozinka"),
            Map.entry("newPassword", "Nova lozinka"),
            Map.entry("token", "Token"),
            Map.entry("name", "Naziv"),
            Map.entry("title", "Naslov"),
            Map.entry("message", "Poruka"),
            Map.entry("typeId", "Tip kategorije"),
            Map.entry("userId", "Korisnik"),
            Map.entry("totalIncome", "Ukupni prihod"),
            Map.entry("totalExpenses", "Ukupni rashodi"),
            Map.entry("totalSavings", "Ukupna štednja")
    );

    @ExceptionHandler(BudgetOverspendException.class)
    public ResponseEntity<Map<String, Object>> handleBudgetOverspend(BudgetOverspendException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", ex.getMessage());
        body.put("confirmationRequired", ex.isConfirmationRequired());
        body.put("shortage", ex.getShortage());
        body.put("unallocated", ex.getUnallocated());
        body.put("categoryRemaining", ex.getCategoryRemaining());

        HttpStatus status = ex.isConfirmationRequired()
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, String>> handleCustom(CustomException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Nemate dozvolu za ovu akciju."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Pogrešan email ili lozinka."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError err = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        if (err == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Neispravni podaci."));
        }
        return ResponseEntity.badRequest().body(Map.of("message", toFriendlyMessage(err.getField(), err.getDefaultMessage())));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraint(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> {
                    String path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "";
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return toFriendlyMessage(field, v.getMessage());
                })
                .orElse("Neispravni podaci.");
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String raw = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        if (raw != null && raw.toLowerCase().contains("category_id")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Polje „Kategorija“ mora biti popunjeno (obavezno za trošak)."
            ));
        }
        if (raw != null && raw.toLowerCase().contains("null")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Neko obavezno polje nije popunjeno."
            ));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Podatak već postoji ili nije validan."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String detail = root.getMessage() != null ? root.getMessage() : ex.getClass().getSimpleName();
        System.err.println("UNHANDLED: " + ex.getClass().getName() + " - " + ex.getMessage());
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Greška: " + detail));
    }

    private String toFriendlyMessage(String field, String defaultMessage) {
        String label = FIELD_LABELS.getOrDefault(field, field != null && !field.isBlank() ? field : "Polje");
        String detail = translateConstraint(defaultMessage);

        if (detail == null || detail.isBlank()
                || detail.equalsIgnoreCase("must not be null")
                || detail.equalsIgnoreCase("must not be blank")
                || detail.equalsIgnoreCase("must not be empty")
                || detail.equalsIgnoreCase("ne smije biti prazno")) {
            return "Polje „" + label + "“ mora biti popunjeno.";
        }

        if (detail.toLowerCase().contains(label.toLowerCase()) || detail.contains("obavezan") || detail.contains("obavezna")) {
            return detail;
        }

        return "Polje „" + label + "“: " + detail;
    }

    private String translateConstraint(String message) {
        if (message == null) return null;
        String m = message.trim();
        return switch (m.toLowerCase()) {
            case "must not be null", "must not be blank", "must not be empty" -> null;
            case "must be greater than 0", "must be greater than or equal to 0" -> "mora biti veći od nule";
            case "must be a well-formed email address" -> "mora biti validan email";
            default -> m;
        };
    }
}
