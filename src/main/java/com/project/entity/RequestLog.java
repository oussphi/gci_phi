package com.project.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "request_logs")
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "method", length = 10, nullable = false)
    private String method;

    @Column(name = "path", length = 1000, nullable = false)
    private String path;

    @Column(name = "controller", length = 255)
    private String controller;

    @Column(name = "handler", length = 255)
    private String handler;

    @Column(name = "status")
    private Integer status;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getController() { return controller; }
    public String getHandler() { return handler; }
    public Integer getStatus() { return status; }
    public Long getDurationMs() { return durationMs; }
    public Instant getTimestamp() { return timestamp; }

    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setMethod(String method) { this.method = method; }
    public void setPath(String path) { this.path = path; }
    public void setController(String controller) { this.controller = controller; }
    public void setHandler(String handler) { this.handler = handler; }
    public void setStatus(Integer status) { this.status = status; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}


