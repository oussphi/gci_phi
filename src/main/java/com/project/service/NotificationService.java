package com.project.service;

import com.project.entity.Commande;
import com.project.entity.Notification;
import com.project.entity.User;
import com.project.repository.NotificationRepository;
import com.project.repository.UserRepository;
import com.project.service.port.NotificationPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService implements NotificationPort {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    @Autowired(required = false)
    private SimpMessagingTemplate broker;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void notifyManager(User manager, Commande commande, String title, String message) {
        // Construire la liste des destinataires: manager + tous les OKACHA
        java.util.LinkedHashSet<User> recipients = new java.util.LinkedHashSet<>();
        if (manager != null) recipients.add(manager);
        try {
            var okachas = userRepository.findByRole_Name("ROLE_OKACHA");
            if (okachas != null) recipients.addAll(okachas);
        } catch (Exception ignored) {}

        for (User r : recipients) {
            if (r == null) continue;
            Notification n = new Notification();
            n.setRecipient(r);
            n.setCommande(commande);
            n.setTitle(title);
            n.setMessage(message);
            n = notificationRepository.save(n);
            pushWs(r, n);
        }
    }

    @Override
    public void notifyUser(User user, Commande commande, String title, String message) {
        if (user == null) return;
        Notification n = new Notification();
        n.setRecipient(user);
        n.setCommande(commande);
        n.setTitle(title);
        n.setMessage(message);
        n = notificationRepository.save(n);
        pushWs(user, n);
    }

    private void pushWs(User user, Notification n) {
        try {
            if (broker != null && user != null && user.getId() != null) {
                var payload = new java.util.HashMap<String, Object>();
                payload.put("id", n.getId());
                payload.put("title", n.getTitle());
                payload.put("message", n.getMessage());
                payload.put("commandeId", n.getCommande() != null ? n.getCommande().getId() : null);
                payload.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
                broker.convertAndSend("/topic/notifications/" + user.getId(), payload);
            }
        } catch (Exception ignored) {}
    }

    public List<Notification> latestFor(User user) {
        return notificationRepository.findTop10ByRecipientOrderByCreatedAtDesc(user);
    }

    public long unreadCount(User user) {
        return notificationRepository.countByRecipientAndReadFlagFalse(user);
    }

    public void markRead(Long id, User expectedRecipient) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (expectedRecipient == null || !expectedRecipient.getId().equals(n.getRecipient().getId())) return;
            if (!n.isReadFlag()) {
                n.setReadFlag(true);
                notificationRepository.save(n);
            }
        });
    }

    public java.util.Optional<Notification> findById(Long id) {
        return notificationRepository.findById(id);
    }

    public Notification save(Notification n) {
        return notificationRepository.save(n);
    }
}
