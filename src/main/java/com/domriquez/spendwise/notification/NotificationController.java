package com.domriquez.spendwise.notification;

import com.domriquez.spendwise.notification.dto.NotificationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read access to the current user's budget notifications. Like the expense endpoints, this
 * requires authentication and only ever returns the caller's own notifications.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        return notificationService.findForCurrentUser();
    }
}
