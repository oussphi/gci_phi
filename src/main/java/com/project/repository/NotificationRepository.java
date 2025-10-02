package com.project.repository;

import com.project.entity.Notification;
import com.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop10ByRecipientOrderByCreatedAtDesc(User recipient);
    long countByRecipientAndReadFlagFalse(User recipient);
}
