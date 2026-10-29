package com.example.incidents.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** Note de traitement ajoutée par un technicien (ou un admin) sur un incident. */
@Entity
@Table(name = "incident_comments", indexes = @Index(name = "idx_comment_incident", columnList = "incident_id"))
public class IncidentComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public IncidentComment() { }

    public IncidentComment(Incident incident, User author, String content) {
        this.incident = incident;
        this.author = author;
        this.content = content;
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public User getAuthor() { return author; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
