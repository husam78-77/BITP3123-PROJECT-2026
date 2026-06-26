// First Commit - Ahmed Abdulrahman Ahmed Ali Gamel - B032320114
// git commit -m "Add Notification service logic - Ahmed B032320114"
package com.smartcampus.notification.service;

import com.smartcampus.notification.entity.Notification;
import com.smartcampus.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // Save a new notification (called by listener)
    public Notification saveNotification(String type, String message) {
        Notification notification = new Notification(
                type,
                message,
                "RECEIVED",
                LocalDateTime.now()
        );
        return notificationRepository.save(notification);
    }

    // Get all notifications
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    // Get by type
    public List<Notification> getByType(String type) {
        return notificationRepository.findByType(type);
    }

    // Get by status
    public List<Notification> getByStatus(String status) {
        return notificationRepository.findByStatus(status);
    }
}