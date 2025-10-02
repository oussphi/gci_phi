package com.project.service.port;

import com.project.entity.Commande;
import com.project.entity.User;

/**
 * Port interface to send notifications, used by domain services.
 */
public interface NotificationPort {
    void notifyManager(User manager, Commande commande, String title, String message);
    default void notifyUser(User user, Commande commande, String title, String message) {
        // no-op by default to keep functional behavior for tests
    }
}
