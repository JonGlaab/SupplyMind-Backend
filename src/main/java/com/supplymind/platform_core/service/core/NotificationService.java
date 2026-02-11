package com.supplymind.platform_core.service.core;

import com.supplymind.platform_core.common.enums.NotificationType;
import com.supplymind.platform_core.common.enums.ReferenceType;
import com.supplymind.platform_core.common.enums.Role;
import com.supplymind.platform_core.dto.core.notification.NotificationDTO;
import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.model.core.Notification;
import com.supplymind.platform_core.repository.auth.UserRepository;
import com.supplymind.platform_core.repository.core.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // --- 1. CREATE NOTIFICATIONS ---

    /**
     * Notify a specific single user.
     */
    public void notifyUser(Long userId, String title, String message,
                           NotificationType type,
                           Long refId, ReferenceType refType) {

        Notification notif = Notification.builder()
                .recipientId(userId)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(refId)
                .referenceType(refType)
                .build();

        notificationRepository.save(notif);
    }

    /**
     * Notify ALL users who have a specific Role (e.g., ALL MANAGERS).
     */
    public void notifyRole(Role role, String title, String message,
                           NotificationType type,
                           Long refId, ReferenceType refType) {

        List<User> users = userRepository.findByRole(role);

        if (users.isEmpty()) return;

        List<Notification> notifications = users.stream().map(user -> Notification.builder()
                .recipientId(user.getId())
                .title(title)
                .message(message)
                .type(type)
                .referenceId(refId)
                .referenceType(refType)
                .build()
        ).collect(Collectors.toList());

        notificationRepository.saveAll(notifications);
    }

    // --- 2. READ NOTIFICATIONS ---

    public List<NotificationDTO> getUserNotifications(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> list = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
        // Only update the ones that are unread to save DB writes
        List<Notification> unread = list.stream().filter(n -> !n.isRead()).toList();

        if (!unread.isEmpty()) {
            unread.forEach(n -> n.setRead(true));
            notificationRepository.saveAll(unread);
        }
    }

    // --- Helper: Map Entity to DTO ---
    private NotificationDTO mapToDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType().name())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType().name())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .timeAgo(calculateTimeAgo(n.getCreatedAt()))
                .build();
    }

    private String calculateTimeAgo(Instant created) {
        Duration diff = Duration.between(created, Instant.now());
        long seconds = diff.getSeconds();

        if (seconds < 60) return "Just now";
        if (seconds < 3600) return (seconds / 60) + "m ago";
        if (seconds < 86400) return (seconds / 3600) + "h ago";
        return (seconds / 86400) + "d ago";
    }
}