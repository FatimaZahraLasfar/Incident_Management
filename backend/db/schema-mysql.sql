-- Script de référence MySQL 8+ : équivalent de ce que Hibernate génère avec ddl-auto=update
-- à partir des entités (backend/src/main/java/com/example/incidents/entity).
-- Il n'est PAS exécuté automatiquement. Il sert à créer la base à la main ou à documenter le schéma.
-- Avec le profil "mysql", Hibernate crée/met à jour les tables seul : ce script est optionnel.

CREATE DATABASE IF NOT EXISTS incidents_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE incidents_db;

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    password    VARCHAR(100) NOT NULL,            -- hash BCrypt
    role        VARCHAR(20)  NOT NULL,
    enabled     BIT          NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('EMPLOYEE', 'TECHNICIAN', 'ADMIN'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS incidents (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    title        VARCHAR(150)  NOT NULL,
    description  VARCHAR(4000) NOT NULL,
    category     VARCHAR(20)   NOT NULL,
    priority     VARCHAR(20)   NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    created_by   BIGINT        NOT NULL,
    assigned_to  BIGINT        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_incidents_created_by  FOREIGN KEY (created_by)  REFERENCES users (id),
    CONSTRAINT fk_incidents_assigned_to FOREIGN KEY (assigned_to) REFERENCES users (id),
    CONSTRAINT chk_incidents_category CHECK (category IN
        ('NETWORK', 'MALWARE', 'PHISHING', 'ACCESS', 'SOFTWARE', 'HARDWARE', 'DATA_LEAK', 'OTHER')),
    CONSTRAINT chk_incidents_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_incidents_status CHECK (status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    INDEX idx_incident_created_by (created_by),
    INDEX idx_incident_assigned_to (assigned_to),
    INDEX idx_incident_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS incident_history (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    incident_id   BIGINT       NOT NULL,
    action_type   VARCHAR(30)  NOT NULL,
    old_value     VARCHAR(150) NULL,
    new_value     VARCHAR(150) NULL,
    message       VARCHAR(500) NOT NULL,
    performed_by  BIGINT       NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_history_incident FOREIGN KEY (incident_id)  REFERENCES incidents (id),
    CONSTRAINT fk_history_user     FOREIGN KEY (performed_by) REFERENCES users (id),
    CONSTRAINT chk_history_action CHECK (action_type IN ('CREATED', 'ASSIGNED', 'STATUS_CHANGED')),
    INDEX idx_history_incident (incident_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS incident_comments (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    incident_id  BIGINT        NOT NULL,
    author_id    BIGINT        NOT NULL,
    content      VARCHAR(2000) NOT NULL,
    created_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_incident FOREIGN KEY (incident_id) REFERENCES incidents (id),
    CONSTRAINT fk_comment_author   FOREIGN KEY (author_id)   REFERENCES users (id),
    INDEX idx_comment_incident (incident_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    token_hash  VARCHAR(64) NOT NULL,             -- SHA-256 hexadécimal du token, jamais le token lui-même
    user_id     BIGINT      NOT NULL,
    expires_at  DATETIME(6) NOT NULL,
    revoked     BIT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_refresh_user (user_id)
) ENGINE=InnoDB;
