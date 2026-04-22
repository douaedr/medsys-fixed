# MedSys — Système de Gestion Médicale

Architecture microservices complète pour la gestion hospitalière.

## Architecture

| Service | Port | Description |
|---------|------|-------------|
| gateway | 8080 | Spring Cloud Gateway (point d'entrée unique) |
| auth-service | 8081 | Authentification JWT, gestion comptes |
| patient-service | 8082 | Dossiers patients, statistiques |
| medical-record-service | 8083 | Consultations, ordonnances, documents |
| appointment-service | 8084 | Rendez-vous, créneaux, liste d'attente |
| billing-service | 8085 | Facturation, paiements |
| notification-service | 8086 | Emails, notifications (RabbitMQ consumer) |
| frontend | 5173 (dev) / 80 (prod) | React 18 + Vite |

## Prérequis

- Docker 24+ et Docker Compose v2
- Java 21 + Maven 3.9 (pour le développement local)
- Node.js 20+ (pour le développement frontend local)

## Démarrage rapide

```bash
# 1. Copier la configuration
cp .env.example .env
# Éditer .env selon votre environnement

# 2. Lancer tous les services
docker compose up -d

# 3. Vérifier l'état
docker compose ps
docker compose logs -f gateway
```

La première fois, le démarrage prend ~2 minutes (MySQL + RabbitMQ doivent être prêts avant les services Java).

## Comptes par défaut

| Email | Mot de passe | Rôle |
|-------|-------------|------|
| admin@medsys.ma | Admin@2026 | ADMIN |
| directeur@medsys.ma | Directeur@2026 | DIRECTEUR |

## Accès

- **Application** : http://localhost:8080 (via gateway) ou http://localhost:5173 (Vite dev)
- **Swagger auth-service** : http://localhost:8081/swagger-ui.html
- **Swagger patient-service** : http://localhost:8082/swagger-ui.html
- **Swagger appointment-service** : http://localhost:8084/swagger-ui.html
- **RabbitMQ Management** : http://localhost:15672 (guest/guest)

## Développement local

### Backend (un service à la fois)

```bash
# Démarrer uniquement les dépendances
docker compose up -d mysql rabbitmq

# Lancer un service en local
cd auth-service
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
# Proxy vers gateway:8080 configuré dans vite.config.js
```

## Variables d'environnement clés (.env)

```env
JWT_SECRET=<clé secrète partagée entre tous les services>
MYSQL_ROOT_PASSWORD=rootpass
MYSQL_PASSWORD=medsyspass
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=votre@gmail.com
MAIL_PASSWORD=votre_app_password
```

## Structure des bases de données

| Base | Service |
|------|---------|
| ms_auth_db | auth-service |
| ms_patient_db | patient-service |
| ms_medical_db | medical-record-service |
| ms_rdv_db | appointment-service |
| ms_billing_db | billing-service |

Le script `scripts/init-databases.sql` crée automatiquement toutes les bases.

## Rôles et permissions

| Rôle | Accès |
|------|-------|
| PATIENT | Son dossier, ses RDV, ses factures, messagerie |
| MEDECIN | Patients, dossiers médicaux, RDV, ordonnances |
| SECRETAIRE | Patients, RDV (confirmation), facturation |
| INFIRMIER | Lecture dossiers, consultations |
| ADMIN | Gestion complète des comptes utilisateurs |
| DIRECTEUR | Lecture seule + statistiques globales |

## Communication asynchrone (RabbitMQ)

Exchange: `medsys.exchange` (topic)

| Routing key | Producteur | Consommateurs |
|-------------|-----------|---------------|
| appointment.created | appointment-service | — |
| appointment.confirmed | appointment-service | notification-service, billing-service |
| appointment.cancelled | appointment-service | notification-service |
| appointment.noshow | appointment-service | — |
| user.created | auth-service | notification-service |
| user.logged_in | auth-service | — |

## Arrêt

```bash
docker compose down          # Arrête les conteneurs (données conservées)
docker compose down -v       # Arrête ET supprime les volumes (RESET complet)
```
