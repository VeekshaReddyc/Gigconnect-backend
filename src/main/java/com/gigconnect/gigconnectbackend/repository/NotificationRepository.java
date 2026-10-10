package com.gigconnect.gigconnectbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gigconnect.gigconnectbackend.entity.Notification;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdAndRecipientRoleOrderByCreatedAtDesc(
            Long recipientId, String recipientRole);

    Optional<Notification> findByIdAndRecipientIdAndRecipientRole(
            Long id, Long recipientId, String recipientRole);
}