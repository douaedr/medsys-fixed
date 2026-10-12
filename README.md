# MedSys - Hospital Management System

MedSys is a hospital management system built as an academic project (PFA) at EHEIM, Oujda (Morocco), between October 2025 and May 2026. It follows a Spring Boot microservices architecture with a React web frontend and a React Native mobile app.

> Academic project - not intended for clinical use.

## Architecture

| Component | Technology | Port |
|---|---|---|
| ms-auth (authentication) | Spring Boot | 8082 |
| ms-patient-personnel (patients and personnel) | Spring Boot | 8081 |
| appointment-service | Spring Boot | 8085 |
| chatbot-service | Spring Boot | 8083 |
| Web frontend | React + Vite | 5173 |
| Database | MySQL | 3307 |

Access control: JWT-based role-based access control (RBAC).

<!-- TO VERIFY: service names/ports match the current folders; mobile app (React Native / Expo) is in this repository -->

## Team and contributions

Team project supervised by Mr. Soufiane Dahmani.

- **Douae Derouich** - main responsibility: Authentication service (`ms-auth`) and Patient service.
- **Meryem Ammari, Ouissal Yahyaoui, Ibtihal Lakhsim** - teammates.
<!-- TO VERIFY: add each teammate's exact contribution -->

## Configuration

No secrets are stored in this repository. Copy `.env.example` to `.env` and fill in your own values (for example the API key used by the chatbot service).