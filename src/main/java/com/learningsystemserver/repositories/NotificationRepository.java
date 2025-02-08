package com.learningsystemserver.repositories;

import com.learningsystemserver.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientUsernameAndIsReadFalse(String recipientUsername);
    List<Notification> findByRecipientUsername(String recipientUsername);
    @Modifying
    @Transactional
    void deleteByRecipientUsername(String username);
}
