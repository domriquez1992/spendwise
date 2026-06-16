package com.domriquez.spendwise.notification;

import com.domriquez.spendwise.notification.dto.NotificationResponse;
import com.domriquez.spendwise.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserProvider currentUserProvider;

    public NotificationService(NotificationRepository notificationRepository,
                               CurrentUserProvider currentUserProvider) {
        this.notificationRepository = notificationRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findForCurrentUser() {
        String username = currentUserProvider.requireCurrentUsername();
        return notificationRepository.findByOwnerUsernameOrderByCreatedAtDesc(username).stream()
                .map(n -> new NotificationResponse(n.getId(), n.getMessage(), n.getCreatedAt()))
                .toList();
    }
}
