package com.domriquez.spendwise.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByOwnerUsernameOrderByCreatedAtDesc(String ownerUsername);

    long countByOwnerUsername(String ownerUsername);
}
