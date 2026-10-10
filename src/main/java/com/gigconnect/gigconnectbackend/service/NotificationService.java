package com.gigconnect.gigconnectbackend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigconnect.gigconnectbackend.entity.Notification;
import com.gigconnect.gigconnectbackend.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification notifyFreelancer(
            Long freelancerId,
            Long gigId,
            String title,
            String message) {

        Notification notification = new Notification();

        notification.setRecipientId(freelancerId);
        notification.setRecipientRole("FREELANCER");
        notification.setGigId(gigId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    public List<Notification> getFreelancerNotifications(
            Long freelancerId) {

        return notificationRepository
                .findByRecipientIdAndRecipientRoleOrderByCreatedAtDesc(
                        freelancerId, "FREELANCER");
    }

    @Transactional
    public boolean markAsRead(
            Long notificationId,
            Long freelancerId) {

        return notificationRepository
                .findByIdAndRecipientIdAndRecipientRole(
                        notificationId,
                        freelancerId,
                        "FREELANCER")
                .map(notification -> {
                    notification.setRead(true);
                    notificationRepository.save(notification);
                    return true;
                })
                .orElse(false);
    }
}