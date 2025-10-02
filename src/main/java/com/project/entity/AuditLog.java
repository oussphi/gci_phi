package com.project.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_name", nullable = false, length = 255)
    private String entityName;

    @Column(name = "entity_id", nullable = false, length = 255)
    private String entityId;

    @Column(name = "action", nullable = false, length = 20)
    private String action; // CREATE, UPDATE, DELETE

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Lob
    @Column(name = "diff_before")
    private String diffBefore;

    @Lob
    @Column(name = "diff_after")
    private String diffAfter;

    public Long getId() { return id; }
    public String getEntityName() { return entityName; }
    public String getEntityId() { return entityId; }
    public String getAction() { return action; }
    public String getUsername() { return username; }
    public Instant getEventTime() { return eventTime; }
    public String getDiffBefore() { return diffBefore; }
    public String getDiffAfter() { return diffAfter; }

    public void setId(Long id) { this.id = id; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public void setAction(String action) { this.action = action; }
    public void setUsername(String username) { this.username = username; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public void setDiffBefore(String diffBefore) { this.diffBefore = diffBefore; }
    public void setDiffAfter(String diffAfter) { this.diffAfter = diffAfter; }
}

