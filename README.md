# Suivi des incidents informatiques et de cybersécurité

Application web de déclaration et de suivi d'incidents, avec authentification **JWT**, autorisation par rôle et contrôle d'accès aux données.
Stack : Spring Boot 3.3 · Spring Security · JWT (jjwt) · JPA · H2 / MySQL · React 18 · Axios · Vite.

```
incident-management/
├── backend/    Spring Boot (port 8080)
├── frontend/   React + Vite (port 5173)
└── docs/       diagrammes (architecture, entités) et collection Postman
```

## Prérequis

Java 17+, Maven 3.9+, Node 18+ et npm. MySQL 8 uniquement si vous utilisez le profil `mysql`.

## Lancer le backend (port 8080)

```bash
cd backend
mvn test                  # lance les tests d'intégration
mvn spring-boot:run       # démarre l'API avec H2 en mémoire
```

Variables d'environnement (voir `backend/.env.example` ; Spring ne lit pas ce fichier tout seul, exportez-les dans le terminal ou dans votre IDE) :

| Variable | Rôle | Défaut |
|---|---|---|
| `JWT_SECRET` | secret HMAC, 32 caractères minimum | secret de développement (un avertissement est affiché) |
| `JWT_ACCESS_MINUTES` | durée de vie de l'access token | 5 |
| `JWT_REFRESH_DAYS` | durée de vie du refresh token | 7 |
| `CORS_ALLOWED_ORIGINS` | origines autorisées | `http://localhost:5173` |
| `SEED_ENABLED` | crée les comptes de démonstration | true |

Console H2 : http://localhost:8080/h2-console, JDBC URL `jdbc:h2:mem:incidentsdb`, utilisateur `sa`, mot de passe vide.

**MySQL** : `SPRING_PROFILES_ACTIVE=mysql DB_USERNAME=... DB_PASSWORD=... mvn spring-boot:run` (la base `incidents_db` est créée si besoin, Hibernate crée les tables). Le script `backend/db/schema-mysql.sql` documente le schéma ; il est optionnel.

## Lancer le frontend (port 5173)

```bash
cd frontend
npm install
npm run dev               # http://localhost:5173
```
Si le backend n'est pas sur `http://localhost:8080`, copiez `.env.example` en `.env` et adaptez `VITE_API_URL`.

## Comptes de test

Créés au démarrage du backend (mot de passe commun **`Password@123`**, démonstration uniquement) :

| Rôle | Email |
|---|---|
| ADMIN | admin@example.com |
| TECHNICIAN | tech1@example.com, tech2@example.com |
| EMPLOYEE | employee1@example.com, employee2@example.com |

`/auth/register` crée toujours un EMPLOYEE (le rôle ne peut pas être choisi). Un ADMIN peut promouvoir un compte existant via la page *Utilisateurs* (`PUT /api/admin/users/{id}/role`).

## Principaux endpoints

| Méthode | URL | Accès |
|---|---|---|
| POST | `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout` | public |
| GET, PUT | `/api/profile` | authentifié |
| PUT | `/api/profile/password` | authentifié |
| GET | `/api/dashboard` | authentifié (contenu selon le rôle) |
| POST | `/api/incidents` | EMPLOYEE |
| GET | `/api/incidents/my` | EMPLOYEE |
| GET | `/api/incidents/assigned` | TECHNICIAN |
| GET | `/api/incidents` | ADMIN |
| GET | `/api/incidents/{id}` | propriétaire, technicien affecté ou ADMIN (sinon 403) |
| PUT | `/api/incidents/{id}/status` | TECHNICIAN (affecté) ou ADMIN |
| PUT | `/api/incidents/{id}/assign/{technicianId}` | ADMIN |
| POST | `/api/incidents/{id}/comments` | TECHNICIAN (affecté) ou ADMIN |
| GET | `/api/admin/users`, `/api/admin/statistics` | ADMIN |
| PUT | `/api/admin/users/{id}/enable`, `/disable`, `/role` | ADMIN |

Erreurs : `{"status": 404, "message": "Incident introuvable"}` (+ `errors` par champ pour un 400 de validation). 400 données invalides · 401 non authentifié ou identifiants incorrects · 403 accès interdit (ou compte désactivé à la connexion) · 404 introuvable · 409 email déjà utilisé.

## Règles métier

Cycle des statuts : `OPEN → ASSIGNED` (affectation par l'ADMIN) `→ IN_PROGRESS → RESOLVED → CLOSED` (clôture par l'ADMIN). Un incident `RESOLVED` peut être repris (`IN_PROGRESS`). Un incident `CLOSED` n'est plus modifiable que par un ADMIN, qui peut le rouvrir (historisé). Les notes de traitement sont internes : visibles du technicien et de l'admin, pas de l'employé, qui voit en revanche l'historique.

## Fonctionnalités réalisées

JWT (email, date de création, expiration, rôle) · refresh token avec rotation et révocation à la déconnexion · routes protégées côté backend et React (`ProtectedRoute`, `RoleRoute`) · contrôle du propriétaire côté serveur · validation (`@Valid`) et erreurs centralisées (`@RestControllerAdvice`) · 3 tableaux de bord par rôle · historique des incidents · notes de traitement · profil et changement de mot de passe · gestion des utilisateurs (activer, désactiver, changer de rôle) · statistiques · recherche et filtres · interface adaptative · expiration de session (suppression du JWT, retour à la page de connexion avec message).

## Vérifier

1. `cd backend && mvn test` : tests des 9 cas de sécurité du devoir, du scénario complet et des règles métier.
2. Collection Postman : importer `docs/incident-security.postman_collection.json` (dossiers *Scénario* et *Tests de sécurité*). Le cas du JWT expiré demande un token expiré dans la variable `expiredToken` (par exemple en démarrant le backend avec `JWT_ACCESS_MINUTES=1` puis en attendant).
3. Navigateur : se connecter avec chaque compte et dérouler le scénario du devoir.

## Dépannage

- **401 « Token expiré » ou retour sur /login** : normal après 5 minutes sans activité si le refresh token n'est plus valide ; reconnectez-vous.
- **Erreur CORS** : le frontend doit tourner sur `http://localhost:5173` (ou ajoutez l'origine dans `CORS_ALLOWED_ORIGINS`).
- **`app.jwt.secret ... 32 caractères` au démarrage** : votre `JWT_SECRET` est trop court.
- **Port déjà utilisé** : 8080 (backend) ou 5173 (le frontend refuse de changer de port, volontairement, à cause de CORS).
- **Comptes de test absents** : ils ne sont créés que si la base est vide et `SEED_ENABLED=true`. Avec H2 en mémoire, la base est recréée à chaque démarrage.

## Limites connues

- Les jetons sont stockés dans `localStorage` (imposé par le devoir) : ils sont lisibles par tout script de la page en cas de faille XSS. React échappe le contenu affiché, ce qui limite ce risque.
- Pas de limitation du nombre de tentatives de connexion.
