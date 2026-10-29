# Incident Management System

A full-stack web application for reporting, assigning, tracking, and resolving **IT and cybersecurity incidents**. The project demonstrates role-based access control, JWT authentication, incident lifecycle management, and protected REST APIs.

> **Project status:** Academic / demonstration project. Use the seeded credentials only for local testing. Configure secure secrets and review the security limitations before any production deployment.

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [User Roles and Permissions](#user-roles-and-permissions)
- [Incident Lifecycle](#incident-lifecycle)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Clone the Repository](#clone-the-repository)
  - [Run the Backend](#run-the-backend)
  - [Run the Frontend](#run-the-frontend)
  - [Configure MySQL](#configure-mysql)
- [Demo Accounts](#demo-accounts)
- [API Overview](#api-overview)
- [Validation and Error Handling](#validation-and-error-handling)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Configuration Reference](#configuration-reference)
- [Troubleshooting](#troubleshooting)
- [Security Notes and Known Limitations](#security-notes-and-known-limitations)
- [Possible Improvements](#possible-improvements)
- [Author](#author)

## Overview

Incident Management helps an organization centralize incident reporting and follow-up instead of relying on scattered messages or manual tracking.

Employees can report incidents and follow their progress. Technicians can access incidents assigned to them, update their status, and add internal treatment notes. Administrators can manage assignments, users, roles, and overall statistics.

The application enforces permissions on the backend, so access to an incident is checked by the server rather than relying only on frontend navigation.

## Key Features

### Authentication and account management

- User registration and login.
- JWT-based authentication.
- Access tokens with configurable expiration.
- Refresh-token flow with rotation and revocation on logout.
- Profile viewing and editing.
- Password change.
- Account enable/disable management by administrators.
- New registrations receive the `EMPLOYEE` role by default.

### Incident management

- Create and track incidents.
- View incidents according to the connected user's role.
- Assign incidents to technicians (administrator).
- Update incident status according to permissions and lifecycle rules.
- Add internal treatment notes.
- Consult incident history.
- Search and filter incident records.
- Restrict incident details to the owner, assigned technician, or administrator as appropriate.

### Administration and reporting

- Role-specific dashboards.
- User administration and role management.
- Incident statistics.
- Incident assignment and follow-up.
- Centralized validation and API error responses.

### User interface

- React-based single-page application.
- Client-side route protection.
- Role-aware navigation and pages.
- HTTP requests handled with Axios.
- Development server powered by Vite.
- Responsive interface.

## Technology Stack

| Area | Technologies |
|---|---|
| Backend | Java 17+, Spring Boot 3.3, Maven |
| Security | Spring Security, JWT (`jjwt`) |
| Persistence | Spring Data JPA / Hibernate |
| Databases | H2 (default development profile), MySQL 8 (optional profile) |
| Frontend | React 18, JavaScript |
| HTTP client | Axios |
| Frontend tooling | Vite, Node.js, npm |
| API testing | Postman |
| Documentation | Architecture/entity diagrams and Postman collection in `docs/` |

## Architecture

The application is organized into a frontend and a backend:

```text
Browser
  |
  v
React 18 + Vite (http://localhost:5173)
  |
  | HTTP requests + JWT
  v
Spring Boot REST API (http://localhost:8080)
  |
  +-- Spring Security / JWT authentication and authorization
  +-- Incident and user business rules
  +-- Validation and centralized exception handling
  |
  v
Spring Data JPA / Hibernate
  |
  +-- H2 in-memory database (default)
  +-- MySQL 8 (optional profile)
```

### Request flow

1. A user signs in through the React interface.
2. The backend validates the credentials and issues authentication tokens.
3. The frontend sends the access token with protected API requests.
4. Spring Security authenticates the request and checks the required permissions.
5. The application applies the business rules and accesses data through JPA.
6. The API returns the result or a structured error response.

**Important:** Frontend route guards improve navigation and user experience, but the backend remains responsible for enforcing authorization.

## User Roles and Permissions

The application defines three roles.

| Role | Main responsibilities |
|---|---|
| `EMPLOYEE` | Report incidents, view personal incidents, consult progress and history, manage their profile. |
| `TECHNICIAN` | View incidents assigned to them, update their status, and add internal treatment notes. |
| `ADMIN` | View all incidents, assign technicians, manage users and roles, enable/disable accounts, access statistics, and manage incident closure. |

The exact permission checks are enforced by the backend. For example, an employee cannot access another employee's incident simply by changing the incident ID in a request.

## Incident Lifecycle

The normal incident workflow is:

```text
OPEN
  |
  | Administrator assigns a technician
  v
ASSIGNED
  |
  | Assigned technician starts work
  v
IN_PROGRESS
  |
  | Technician marks the incident as resolved
  v
RESOLVED
  |
  | Administrator closes the incident
  v
CLOSED
```

Additional business rules:

- A `RESOLVED` incident can be returned to `IN_PROGRESS` when further work is needed.
- A `CLOSED` incident is normally no longer editable; an administrator can reopen it, and the reopening is recorded in the history.
- Treatment notes are internal and visible to technicians and administrators, not employees.
- Employees can consult the incident history to follow progress.

## Getting Started

### Prerequisites

Install the following tools:

- **Java 17 or later**
- **Maven 3.9 or later**
- **Node.js 18 or later**
- **npm**
- **MySQL 8**, only if you want to use MySQL instead of the default H2 database
- **Git**

Check your installations:

```bash
java -version
mvn -version
node -v
npm -v
git --version
```

### Clone the Repository

```bash
git clone https://github.com/FatimaZahraLasfar/Incident_Management.git
cd Incident_Management
```

### Run the Backend

Open a terminal in the repository root:

```bash
cd backend
```

Run the backend tests:

```bash
mvn test
```

Start the Spring Boot application:

```bash
mvn spring-boot:run
```

By default, the backend starts at:

- API base URL: `http://localhost:8080`
- H2 console: `http://localhost:8080/h2-console`

The default development configuration uses an in-memory H2 database. Data stored in this database is recreated when the application restarts.

#### H2 console connection

Use the following development connection settings:

| Setting | Value |
|---|---|
| JDBC URL | `jdbc:h2:mem:incidentsdb` |
| Username | `sa` |
| Password | Leave empty |

Only use the H2 console in a local development environment.

### Run the Frontend

Open a **second terminal** from the repository root:

```bash
cd frontend
npm install
npm run dev
```

Open the frontend in your browser:

`http://localhost:5173`

Keep the backend running while using the frontend. The frontend's API URL can be configured using `VITE_API_URL`; see the frontend `.env.example` file.

Example frontend environment configuration:

```dotenv
VITE_API_URL=http://localhost:8080
```

If the project expects a different `.env` setup, follow the existing configuration in `frontend/.env.example`.

### Configure MySQL

MySQL is optional. The default development setup uses H2.

1. Install and start MySQL 8.
2. Create or ensure access to a database named `incidents_db`.
3. Configure the database username and password using environment variables.
4. Start the backend with the `mysql` Spring profile.

Example for a Unix-like shell:

```bash
cd backend
export SPRING_PROFILES_ACTIVE=mysql
export DB_USERNAME=your_mysql_username
export DB_PASSWORD=your_mysql_password
mvn spring-boot:run
```

In PowerShell:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:DB_USERNAME = "your_mysql_username"
$env:DB_PASSWORD = "your_mysql_password"
mvn spring-boot:run
```

Replace the example credentials with your own local MySQL credentials. Do not commit real passwords to Git.

The repository also contains `backend/db/schema-mysql.sql` as a reference for the MySQL schema. According to the current project documentation, Hibernate creates the tables when using the MySQL profile.

## Demo Accounts

When database seeding is enabled and the database is empty, the backend creates demonstration accounts.

| Role | Email | Password |
|---|---|---|
| Administrator | `admin@example.com` | `Password@123` |
| Technician | `tech1@example.com` | `Password@123` |
| Technician | `tech2@example.com` | `Password@123` |
| Employee | `employee1@example.com` | `Password@123` |
| Employee | `employee2@example.com` | `Password@123` |

**Demo credentials only:** These accounts and passwords are for local testing. Never use them in a deployed or production environment.

Registration through `/auth/register` creates an `EMPLOYEE` account. An administrator can change a user's role through the user-management interface.

## API Overview

Base URL:

```text
http://localhost:8080
```

The following table summarizes the main endpoints documented for the project. Protected endpoints require a valid access token unless otherwise noted.

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `POST` | `/auth/register` | Public | Register an employee account |
| `POST` | `/auth/login` | Public | Authenticate and obtain tokens |
| `POST` | `/auth/refresh` | Refresh-token flow | Refresh authentication |
| `POST` | `/auth/logout` | Authenticated | Log out and revoke the refresh token |
| `GET`, `PUT` | `/api/profile` | Authenticated | View or update the current profile |
| `PUT` | `/api/profile/password` | Authenticated | Change the current password |
| `GET` | `/api/dashboard` | Authenticated | Retrieve role-specific dashboard data |
| `POST` | `/api/incidents` | `EMPLOYEE` | Create an incident |
| `GET` | `/api/incidents/my` | `EMPLOYEE` | List the current employee's incidents |
| `GET` | `/api/incidents/assigned` | `TECHNICIAN` | List incidents assigned to the technician |
| `GET` | `/api/incidents` | `ADMIN` | List all incidents |
| `GET` | `/api/incidents/{id}` | Owner, assigned technician, or `ADMIN` | View an incident when authorized |
| `PUT` | `/api/incidents/{id}/status` | Assigned technician or `ADMIN` | Update incident status |
| `PUT` | `/api/incidents/{id}/assign/{technicianId}` | `ADMIN` | Assign an incident to a technician |
| `POST` | `/api/incidents/{id}/comments` | Assigned technician or `ADMIN` | Add an internal treatment note |
| `GET` | `/api/admin/users` | `ADMIN` | List/manage users |
| `GET` | `/api/admin/statistics` | `ADMIN` | Retrieve administrative statistics |
| `PUT` | `/api/admin/users/{id}/enable` | `ADMIN` | Enable a user account |
| `PUT` | `/api/admin/users/{id}/disable` | `ADMIN` | Disable a user account |
| `PUT` | `/api/admin/users/{id}/role` | `ADMIN` | Change a user's role |

For request and response payloads, consult the backend source code and the Postman collection in `docs/incident-security.postman_collection.json`.

### Example: Register an employee

```http
POST /auth/register
Content-Type: application/json
```

Use the registration payload expected by the backend DTO. Registration assigns the `EMPLOYEE` role on the server; clients cannot choose an elevated role during public registration.

### Example: Access a protected endpoint

```http
GET /api/dashboard
Authorization: Bearer <access-token>
```

Replace `<access-token>` with a valid token returned by the login endpoint.

## Validation and Error Handling

The backend uses request validation and centralized exception handling to return consistent API errors.

Common HTTP status codes:

| Status | Meaning |
|---|---|
| `400 Bad Request` | Invalid input or validation failure |
| `401 Unauthorized` | Missing, invalid, or expired authentication; invalid login credentials |
| `403 Forbidden` | Authenticated user does not have permission, or account access is disabled |
| `404 Not Found` | Requested incident or resource does not exist |
| `409 Conflict` | Email address is already registered |

A typical error response may look like:

```json
{
  "status": 404,
  "message": "Incident introuvable"
}
```

Validation errors may also include an `errors` object with field-specific details.

## Testing

### Run backend tests

From the `backend/` directory:

```bash
mvn test
```

The project documentation describes tests covering security scenarios, the complete incident workflow, and business rules.

### Test with Postman

1. Open Postman.
2. Import `docs/incident-security.postman_collection.json`.
3. Run the requests in the **Scenario** and **Security Tests** folders.
4. Authenticate first for protected endpoints.
5. Check authorization behavior using employee, technician, and administrator accounts.

The expired-token test requires an expired access token. For example, configure `JWT_ACCESS_MINUTES=1`, start the backend, obtain a token, and wait for it to expire before running the test.

### Suggested manual verification

- Register a new employee.
- Sign in with each demo role.
- Create an incident as an employee.
- Verify that the employee can only view their own incidents.
- Assign the incident to a technician as an administrator.
- Update the status and add an internal note as the assigned technician.
- Resolve the incident and close it as an administrator.
- Confirm that unauthorized operations return the expected HTTP status.

## Project Structure

```text
Incident_Management/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   ├── db/
│   │   └── schema-mysql.sql
│   ├── .env.example
│   └── pom.xml
├── frontend/
│   ├── src/
│   ├── public/
│   ├── .env.example
│   └── package.json
├── docs/
│   ├── incident-security.postman_collection.json
│   └── architecture / entity diagrams
├── .gitignore
└── README.md
```

The tree above summarizes the main repository areas. Exact subfolders and filenames may evolve as the project develops.

## Configuration Reference

Backend configuration can be supplied through environment variables. Spring Boot does **not** automatically load `backend/.env.example` as an environment file; export the variables in your shell or configure them in your IDE.

| Variable | Purpose | Documented default |
|---|---|---|
| `JWT_SECRET` | HMAC signing secret; at least 32 characters | Development fallback (warning displayed) |
| `JWT_ACCESS_MINUTES` | Access-token lifetime in minutes | `5` |
| `JWT_REFRESH_DAYS` | Refresh-token lifetime in days | `7` |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins | `http://localhost:5173` |
| `SEED_ENABLED` | Enables creation of demo accounts | `true` |
| `SPRING_PROFILES_ACTIVE` | Selects the Spring profile | Default H2 configuration |
| `DB_USERNAME` | MySQL username when using the MySQL profile | Not applicable |
| `DB_PASSWORD` | MySQL password when using the MySQL profile | Not applicable |

Example PowerShell configuration for a local development session:

```powershell
$env:JWT_SECRET = "replace-with-a-long-random-development-secret"
$env:JWT_ACCESS_MINUTES = "5"
$env:JWT_REFRESH_DAYS = "7"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
$env:SEED_ENABLED = "true"
```

Use a strong, unique secret and an appropriate token lifetime for any environment beyond local development.

## Troubleshooting

### Backend does not start

- Verify that Java 17+ and Maven are installed.
- Check whether port `8080` is already in use.
- Review the startup logs for database or JWT configuration errors.
- Ensure `JWT_SECRET` is at least 32 characters long if you override it.

### Frontend cannot call the backend

- Confirm the backend is running at `http://localhost:8080`.
- Confirm the frontend is running at `http://localhost:5173`.
- Check `VITE_API_URL` in the frontend environment configuration.
- Ensure `CORS_ALLOWED_ORIGINS` includes the frontend origin.

### A token expires or the application redirects to login

Access tokens have a short default lifetime. If the refresh token is also invalid or expired, sign in again.

### Demo accounts are missing

Demo accounts are created only when seeding is enabled and the database is empty. With the in-memory H2 database, the data is recreated after a backend restart.

### A port is already in use

- Backend: `8080`
- Frontend: `5173`

Stop the process using the required port or adjust the configuration carefully. The current project documentation expects the frontend origin to remain `http://localhost:5173` unless CORS is updated accordingly.

## Security Notes and Known Limitations

This project implements role-based authorization, server-side ownership checks, JWT authentication, refresh-token rotation/revocation, input validation, and centralized error handling. It is still a demonstration project and has documented limitations:

- Tokens are stored in `localStorage` as required by the project specification. A successful XSS attack could expose tokens accessible to page scripts.
- Login-attempt rate limiting is not implemented.
- Demo accounts use a shared, publicly documented password and must never be used outside local testing.
- The development JWT fallback secret is not suitable for production.
- The H2 console should remain a local-development tool and must not be exposed publicly.

Before production use, review the authentication design, add rate limiting and appropriate monitoring, use production-grade secret management, enforce HTTPS, and conduct security testing.

## Possible Improvements

Potential next steps include:

- Add login rate limiting and account lockout or progressive delays.
- Add automated frontend tests and broader backend integration tests.
- Add pagination and more advanced incident filters where needed.
- Add audit logging for administrative actions and sensitive changes.
- Improve deployment configuration and environment-specific profiles.
- Add CI workflows for build, tests, and static analysis.
- Reassess token storage and session protections for the intended deployment environment.

These are suggestions for future development, not claims that the features are already implemented.

## Author

**Fatima Zahra Lasfar**

- GitHub: [FatimaZahraLasfar](https://github.com/FatimaZahraLasfar)
- Portfolio: [My Portfolio](https://fatimazahralasfar.github.io/My-Portfolio/)

---

If you find this project useful, feel free to explore the repository, open an issue, or suggest an improvement.
