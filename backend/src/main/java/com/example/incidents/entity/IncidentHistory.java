package com.example.incidents.entity;

import com.example.incidents.enums.HistoryAction;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "incident_history", indexes = @Index(name = "idx_history_incident", columnList = "incident_id"))
public class IncidentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private HistoryAction actionType;

    @Column(name = "old_value", length = 150)
    private String oldValue;

    @Column(name = "new_value", length = 150)
    private String newValue;

    @Column(nullable = false, length = 500)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public IncidentHistory() { }

    public IncidentHistory(Incident incident, HistoryAction actionType, String oldValue, String newValue,
                           String message, User performedBy) {
        this.incident = incident;
        this.actionType = actionType;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.message = message;
        this.performedBy = performedBy;
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public HistoryAction getActionType() { return actionType; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getMessage() { return message; }
    public User getPerformedBy() { return performedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
