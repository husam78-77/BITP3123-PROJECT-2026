// First Commit - Ahmed Abdulrahman Ahmed Ali Gamel - B032320114
// git commit -m "Add RabbitMQ event listener - Ahmed B032320114"
package com.smartcampus.notification.listener;

import com.smartcampus.notification.config.RabbitMQConfig;
import com.smartcampus.notification.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Listen for enrollment events
    @RabbitListener(queues = RabbitMQConfig.ENROLMENT_QUEUE)
    public void handleEnrolmentEvent(String message) {
        System.out.println("[Notification] Enrolment event received: " + message);
        notificationService.saveNotification("ENROLMENT", message);
    }

    // Listen for library events
    @RabbitListener(queues = RabbitMQConfig.LIBRARY_QUEUE)
    public void handleLibraryEvent(String message) {
        System.out.println("[Notification] Library event received: " + message);
        notificationService.saveNotification("LIBRARY", message);
    }
}