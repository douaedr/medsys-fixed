# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**MedSys** is a microservices-based hospital management system with separate services for authentication, patient/medical records, appointments, and notifications. It serves web (React/Vite) and mobile (React Native/Expo) clients.

## Service Map

| Service | Port | Package | Purpose |
|---------|------|---------|---------|
| `ms-auth` | 8082 | `com.hospital.auth` | JWT auth, user accounts, password reset |
| `ms-patient-personnel` | 8081 | `com.hospital.patient` | Patients, medical records, documents |
| `ms-rdv` | 8083 | `ma.medsys.rdv` | Appointment scheduling |
| `ms-notify` | 8084 | `ma.medsys.notify` | WebSocket/RabbitMQ notifications |
| `api-gateway` | 8080 | — | Spring Cloud Gateway (optional) |
| `medsys-web` | 5173 | — | React/Vite web frontend |
| `medsys-mobile` | — | — | React Native/Expo mobile app |

## Commands

### Backend (Java 21 / Maven)
```bash
# Run a service
cd ms-auth && mvn spring-boot:run
cd ms-patient-personnel && mvn spring-boot:run
cd ms-rdv && mvn spring-boot:run
cd ms-notify && mvn spring-boot:run

# Build without tests
mvn clean package -DskipTests

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName
```

### Frontend
```bash
# Web
cd medsys-web && npm install && npm run dev     # dev server on :5173
cd medsys-web && npm run build                  # production build

# Mobile
cd medsys-mobile && npm install && npm start    # Expo dev server
cd medsys-mobile && npm run android
```

### Startup Order
1. MySQL on port **3307** (XAMPP or Docker)
2. `ms-auth` (8082)
3. `ms-patient-personnel` (8081)
4. `medsys-web` (5173) — Vite proxy handles API routing
5. Optional: `ms-rdv`, `ms-notify`, `api-gateway`

## Architecture

### Inter-Service Communication
- **Synchronous**: REST over HTTP with Bearer JWT. `ms-patient-personnel` calls `ms-auth` at `GET /api/v1/auth/verify?token=...` to validate tokens on every request.
- **Asynchronous**: RabbitMQ events. When a user registers in `ms-auth`, an event triggers `ms-patient-personnel` to create the corresponding `Patient` record. The `patientId` is stored back in `ms_auth_db.user_accounts.patientId`.

### Database
Two independent MySQL schemas on port **3307**:
- `ms_auth_db` — user accounts, tokens, brute-force state
- `ms_patient_db` — full medical schema (patients, dossiers, consultations, prescriptions, labs, imaging, hospitalizations)

Hibernate `ddl-auto: update` auto-creates/migrates tables on startup. A full manual schema is at `docs/sql/mpd.sql`.

### JWT
The **same `jwt.secret` value must be set in every service**. It is read from the `JWT_SECRET` env variable (see `.env.example`). Tokens are passed as `Authorization: Bearer <token>` and stored in `sessionStorage` (web) / `AsyncStorage` (mobile).

### Vite Dev Proxy (`medsys-web/vite.config.js`)
```
/api/v1/auth  → http://localhost:8082
/api/v1/admin → http://localhost:8082
/api/v1       → http://localhost:8081
```
The admin route must target 8082, not 8081 (this was a fixed bug — BUG-03).

## Code Conventions

### Java Microservice Layout
Every service follows the same structure:
```
src/main/java/com/hospital/{service}/
├── config/          # SecurityConfig, CorsConfig, RabbitMQ config
├── controller/      # @RestController — HTTP layer only
├── dto/             # *RequestDTO, *ResponseDTO — never expose entities directly
├── entity/          # @Entity JPA classes
├── enums/           # UPPER_SNAKE_CASE values
├── exception/       # Custom exceptions + @ControllerAdvice GlobalExceptionHandler
├── mapper/          # Entity ↔ DTO conversion
├── messaging/       # RabbitMQ producers/consumers
├── repository/      # Spring Data JPA interfaces
├── security/        # JwtService, auth filters
└── service/         # Business logic
```

### Naming
- Entities: `PascalCase` — `DossierMedical`, `LigneOrdonnance`
- DTOs: suffixed — `PatientRequestDTO`, `PatientResponseDTO`
- DB columns: Hibernate auto-maps camelCase fields to `snake_case`
- All REST endpoints are versioned: `/api/v1/`

### Key Roles (enum `Role` in ms-auth)
Active: `PATIENT`, `DOCTOR`, `SECRETARY`, `ADMIN`
Legacy (kept for compatibility): `MEDECIN`, `PERSONNEL`, `DIRECTEUR`

### Pagination
Services accept `?page=0&size=20&sortBy=id&direction=asc` via Spring Data `Pageable`.

## Configuration

Copy `.env.example` to `.env` and configure before starting. Key variables:
- `DB_AUTH_URL`, `DB_PATIENT_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET` — must be identical across all services
- `MS_PATIENT_URL`, `MS_AUTH_URL` — inter-service base URLs
- `SMTP_HOST`, `SMTP_USERNAME`, `SMTP_PASSWORD` — Gmail SMTP for password reset

Each service reads these via `application.yml` with `${VAR:default}` syntax.

## API Documentation

Swagger UI is available at each running service:
- `http://localhost:8082/swagger-ui.html` (ms-auth)
- `http://localhost:8081/swagger-ui.html` (ms-patient-personnel)

## Known Constraints

- JWT expiration defaults to 15 minutes (`JWT_EXPIRATION=900000`). Set to `86400000` for development.
- Mobile `medsys-mobile/src/api/api.js` has the backend base URL — update to match your machine's IP when testing on a real device.
- File uploads (patient documents) are stored locally under `ms-patient-personnel/uploads/patients/`; there is no cloud storage integration.
