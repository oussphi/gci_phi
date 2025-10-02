package com.project.listener;

import com.project.entity.AuditLog;
import com.project.repository.AuditLogRepository;
import com.project.util.JsonUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AuditEntityListener {

    private static AuditLogRepository staticAuditRepo;
    private static boolean enabled = true;

    @Autowired
    public void init(AuditLogRepository repo) {
        staticAuditRepo = repo;
    }

    @Autowired
    public void initEnabled(@Value("${app.audit.enabled:true}") boolean isEnabled) {
        enabled = isEnabled;
    }

    @PrePersist
    public void prePersist(Object entity) {
        writeLog(entity, "CREATE", null, JsonUtils.toJsonSafe(entity));
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        // For simplicity, only store after image on update; before image may require deep copy
        writeLog(entity, "UPDATE", null, JsonUtils.toJsonSafe(entity));
    }

    @PreRemove
    public void preRemove(Object entity) {
        writeLog(entity, "DELETE", JsonUtils.toJsonSafe(entity), null);
    }

    private void writeLog(Object entity, String action, String beforeJson, String afterJson) {
        if (!enabled || staticAuditRepo == null) return;
        AuditLog log = new AuditLog();
        log.setEntityName(entity.getClass().getSimpleName());
        // Try to find id reflectively
        try {
            Object id = entity.getClass().getMethod("getId").invoke(entity);
            log.setEntityId(id != null ? String.valueOf(id) : "");
        } catch (Exception ignored) {
            log.setEntityId("");
        }
        log.setAction(action);
        log.setEventTime(Instant.now());
        log.setDiffBefore(beforeJson);
        log.setDiffAfter(afterJson);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            log.setUsername(authentication.getName());
        } else {
            log.setUsername("system");
        }
        staticAuditRepo.save(log);
    }
}


