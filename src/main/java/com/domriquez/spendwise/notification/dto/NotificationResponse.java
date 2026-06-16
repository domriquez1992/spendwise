package com.domriquez.spendwise.notification.dto;

import java.time.Instant;

public record NotificationResponse(Long id, String message, Instant createdAt) {
}
