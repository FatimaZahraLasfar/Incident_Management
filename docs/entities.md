# Diagramme des entités

```mermaid
erDiagram
    USERS ||--o{ INCIDENTS : "crée (created_by)"
    USERS ||--o{ INCIDENTS : "traite (assigned_to, optionnel)"
    INCIDENTS ||--o{ INCIDENT_HISTORY : "a un historique"
    INCIDENTS ||--o{ INCIDENT_COMMENTS : "a des notes"
    USERS ||--o{ INCIDENT_HISTORY : "performed_by"
    USERS ||--o{ INCIDENT_COMMENTS : "author_id"
    USERS ||--o{ REFRESH_TOKENS : "possède"

    USERS {
        bigint id PK
        varchar full_name
        varchar email UK
        varchar password "hash BCrypt"
        varchar role "EMPLOYEE | TECHNICIAN | ADMIN"
        bit enabled
        datetime created_at
    }
    INCIDENTS {
        bigint id PK
        varchar title
        varchar description
        varchar category
        varchar priority
        varchar status
        datetime created_at
        datetime updated_at
        bigint created_by FK
        bigint assigned_to FK "nullable"
    }
    INCIDENT_HISTORY {
        bigint id PK
        bigint incident_id FK
        varchar action_type "CREATED | ASSIGNED | STATUS_CHANGED"
        varchar old_value
        varchar new_value
        varchar message
        bigint performed_by FK
        datetime created_at
    }
    INCIDENT_COMMENTS {
        bigint id PK
        bigint incident_id FK
        bigint author_id FK
        varchar content
        datetime created_at
    }
    REFRESH_TOKENS {
        bigint id PK
        varchar token_hash UK "SHA-256"
        bigint user_id FK
        datetime expires_at
        bit revoked
    }
```

## Choix JPA (travail de recherche, section 4 du devoir)

- **`@ManyToOne(fetch = LAZY)`** pour `createdBy` et `assignedTo` : l'utilisateur n'est chargé que si on le lit. Les listes utilisent `@EntityGraph` pour les charger en une seule requête (pas de N+1). `EAGER` a été écarté : il charge toujours les utilisateurs, même inutiles.
- **Pas de `@OneToMany`** : `User.incidents` serait une collection potentiellement énorme et jamais utilisée en entier ; l'historique et les notes d'un incident sont lus par clé étrangère (`findByIncidentId...`), ce qui évite aussi les `LazyInitializationException` avec `open-in-view=false`.
- Les énumérations sont stockées en `EnumType.STRING` (lisibles, insensibles à l'ordre des constantes).
