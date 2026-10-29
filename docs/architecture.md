# Architecture générale

```mermaid
flowchart TB
    subgraph Browser["Navigateur — React 18 + Vite (:5173)"]
        P[Pages] --> AC[AuthContext]
        P --> PR[ProtectedRoute / RoleRoute]
        P --> S[services/*.js]
        S --> AX[Axios api.js<br/>interceptor requête : Authorization Bearer<br/>interceptor réponse : 401 → refresh ou retour /login]
    end
    AX -- "HTTP / JSON" --> CORS
    subgraph Backend["Spring Boot (:8080)"]
        CORS[CorsFilter] --> JF[JwtAuthenticationFilter]
        JF --> SC[SecurityFilterChain<br/>règles d'URL + 401/403 JSON]
        SC --> C[Controllers<br/>@Valid + @PreAuthorize]
        C --> SV[Services<br/>règles métier + contrôle du propriétaire]
        SV --> R[Repositories Spring Data JPA]
    end
    R --> DB[(H2 en mémoire / MySQL)]
```

## Flux d'authentification

```mermaid
sequenceDiagram
    participant R as React
    participant B as Backend
    R->>B: POST /auth/login {email, password}
    B-->>R: {token (JWT, 5 min), refreshToken, role, fullName}
    R->>B: GET /api/... (Authorization: Bearer token)
    B-->>R: 200
    Note over R,B: l'access token expire
    R->>B: GET /api/... (token expiré)
    B-->>R: 401 "Token expiré"
    R->>B: POST /auth/refresh {refreshToken}
    B-->>R: nouveau token + nouveau refreshToken (rotation)
    R->>B: requête rejouée avec le nouveau token
    Note over R,B: refresh impossible → suppression du JWT, redirection /login, message « session expirée »
```

## Sécurité en couches

1. **Règles d'URL** (`SecurityConfig`) : `/auth/**` et `/h2-console/**` publics, `/api/admin/**` ADMIN, `/api/**` authentifié.
2. **`@PreAuthorize`** sur les contrôleurs (rôle).
3. **Services** : droit d'accéder à CET incident (propriétaire, technicien affecté) vérifié côté serveur.
4. **Filtre JWT** : l'utilisateur est rechargé en base à chaque requête, donc un compte désactivé ou un rôle modifié prend effet immédiatement.
