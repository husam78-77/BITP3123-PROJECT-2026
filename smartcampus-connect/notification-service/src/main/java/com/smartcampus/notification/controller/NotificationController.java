// First Commit - Ahmed Abdulrahman Ahmed Ali Gamel - B032320114
// git commit -m "Add Notification REST controller - Ahmed B032320114"
package com.smartcampus.notification.controller;

import com.smartcampus.notification.entity.Notification;
import com.smartcampus.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // GET all notifications
    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    // GET notifications by type
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Notification>> getByType(@PathVariable String type) {
        return ResponseEntity.ok(notificationService.getByType(type));
    }

    // GET notifications by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Notification>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(notificationService.getByStatus(status));
    }
}