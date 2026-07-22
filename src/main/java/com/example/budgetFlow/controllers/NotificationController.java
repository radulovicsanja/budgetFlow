package com.example.budgetFlow.controllers;

import com.example.budgetFlow.DTO.NotificationRequest;
import com.example.budgetFlow.entity.Notification;
import com.example.budgetFlow.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Notifications", description = "In-app notifications and bill reminders")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Create reminder/notification")
    @ApiResponse(responseCode = "200", description = "Created")
    @PostMapping
    public ResponseEntity<Notification> create(@RequestBody @Valid NotificationRequest request) {
        return ResponseEntity.ok(notificationService.create(request));
    }

    @Operation(summary = "Get my notifications")
    @ApiResponse(responseCode = "200", description = "List returned")
    @GetMapping
    public ResponseEntity<List<Notification>> getMine(
            @Parameter(description = "If true, only unread") @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return ResponseEntity.ok(notificationService.getMyNotifications(unreadOnly));
    }

    @Operation(summary = "Unread count (for badge)")
    @ApiResponse(responseCode = "200", description = "Count returned")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount());
    }

    @Operation(summary = "Mark notification as read")
    @ApiResponse(responseCode = "200", description = "Marked as read")
    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markRead(@Parameter(description = "Notification ID") @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @Operation(summary = "Mark all my notifications as read")
    @ApiResponse(responseCode = "200", description = "All marked as read")
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        int count = notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("markedRead", count));
    }

    @Operation(summary = "Delete notification")
    @ApiResponse(responseCode = "200", description = "Deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@Parameter(description = "Notification ID") @PathVariable Long id) {
        notificationService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Notifikacija obrisana."));
    }
}
