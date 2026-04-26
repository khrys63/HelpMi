# HelpMi

Outil de ticketing interne inspiré de Jira. Gestion de projets, tickets, commentaires, pièces jointes et utilisateurs, avec authentification via Keycloak (ou mode développement sans auth).

---

## Fonctionnalités

- **Projets** : création, liste, désactivation
- **Tickets** : création, édition, changement de statut (machine à états), priorité, type, date d'échéance, assigné, clients, labels
- **Tickets récurrents** : types ANNUEL, MENSUEL, TRIMESTRIEL — à la fermeture, un ticket identique est recréé automatiquement avec la date d'échéance décalée
- **Cloner / Déplacer** : duplication d'un ticket ou déplacement vers un autre projet avec nouvelle référence
- **Liens entre tickets** : BLOCKS, IS_BLOCKED_BY, RELATES_TO, DUPLICATES, etc.
- **Commentaires** : ajout, édition, suppression
- **Pièces jointes** : upload et téléchargement de fichiers
- **Labels** : création à la volée ou depuis l'admin
- **Clients** : rattachement de clients à un ticket
- **Administration** : gestion des valeurs de configuration (statuts, priorités, types, types de liens), clients, labels
- **Notifications toast** : retours visuels après les actions clés

---

## Stack technique

| Couche | Technologie |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Spring Security (OAuth2 JWT) |
| Persistance | PostgreSQL 16, Hibernate/JPA, Flyway |
| Frontend | Vue 3 (Composition API), Pinia, Vue Router 4, Tailwind CSS, Axios |
| Auth | Keycloak 24 (ou mode dev sans auth) |
| Build | Maven, Vite |
| Conteneurs | Docker, Docker Compose |

---

## Structure du projet

```
.
├── backend/                        # API Spring Boot
│   ├── src/main/java/com/helpmi/
│   │   ├── controller/             # REST controllers (tickets, projets, commentaires, …)
│   │   ├── service/                # Logique métier
│   │   ├── domain/                 # Entités JPA
│   │   ├── dto/                    # Request / Response DTOs
│   │   ├── repository/             # Spring Data JPA
│   │   ├── security/               # DevAuthFilter, CurrentUserService
│   │   └── config/                 # SecurityConfig, StorageConfig
│   └── src/main/resources/
│       ├── application.yml         # Config commune
│       ├── application-dev.yml     # Config mode développement local
│       ├── application-prod.yml    # Config production (variables d'env)
│       └── db/
│           ├── migration/          # Migrations Flyway (V1 → V6)
│           └── dev-seed/           # Données de test (profil dev uniquement)
│
├── frontend/                       # SPA Vue 3
│   └── src/
│       ├── views/                  # Pages (Projets, Ticket, Admin, …)
│       ├── components/             # Composants réutilisables
│       ├── stores/                 # Pinia (auth, config, toast)
│       ├── services/api.js         # Client Axios centralisé
│       └── router/                 # Routes Vue Router
│
├── keycloak/
│   └── realm-export.json           # Import du realm Keycloak
│
└── docker-compose.yml              # Postgres + Keycloak + Backend + Frontend
```

---

## Lancer le projet

### Mode développement (sans Docker pour le backend/frontend)

Prérequis : Java 21+, Node 20+, Docker

**1. Démarrer Postgres**

```bash
docker-compose up -d postgres
```

**2. Démarrer le backend**

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Le profil `dev` désactive l'authentification Keycloak et injecte automatiquement un utilisateur ADMIN. Les données de test sont insérées via Flyway au premier démarrage.

**3. Démarrer le frontend**

```bash
cd frontend
npm install
npm run dev
```

L'application est disponible sur `http://localhost:5173`. Le proxy Vite redirige `/api` vers `http://localhost:8080`.

---

### Mode production (Docker Compose complet)

```bash
docker-compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Keycloak | http://localhost:8180 |

Le realm Keycloak `helpmi` est importé automatiquement au premier démarrage.

---

## Migrations de base de données

Les migrations sont gérées par Flyway et s'appliquent automatiquement au démarrage.

| Fichier | Contenu |
|---|---|
| `V1__init.sql` | Schéma initial (users, projects, tickets, comments, attachments) |
| `V2__ticket_links.sql` | Liens entre tickets |
| `V3__config_values.sql` | Valeurs de configuration (statuts, priorités, types, …) |
| `V4__clients_labels.sql` | Clients et labels |
| `V5__due_date.sql` | Date d'échéance sur les tickets |
| `V6__recurring_types.sql` | Types récurrents (ANNUEL, MENSUEL, TRIMESTRIEL) |

Les fichiers dans `db/dev-seed/` ne sont chargés qu'avec le profil `dev`.

---

## Variables à personnaliser

Plusieurs valeurs de configuration sont actuellement **commitées en dur** dans les fichiers de configuration. Elles doivent être adaptées à votre environnement avant tout déploiement.

### Base de données — `application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/helpmi
    username: helpmi
    password: helpmi_pass        # ← à changer en production
```

### Base de données — `docker-compose.yml`

```yaml
postgres:
  environment:
    POSTGRES_DB: helpmi
    POSTGRES_USER: helpmi
    POSTGRES_PASSWORD: helpmi_pass   # ← à changer
```

### Backend production — variables d'environnement Docker Compose

Le profil `prod` ne lit pas de valeurs en dur : tout est passé par variables d'environnement dans `docker-compose.yml`.

```yaml
backend:
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/helpmi
    SPRING_DATASOURCE_USERNAME: helpmi
    SPRING_DATASOURCE_PASSWORD: helpmi_pass       # ← à changer
    APP_KEYCLOAK_ISSUER_URI: http://keycloak:8080/realms/helpmi
    APP_STORAGE_PATH: /app/uploads
```

### Keycloak — `keycloak/realm-export.json`

Le fichier contient des comptes de démonstration avec des mots de passe par défaut :

| Email | Rôle | Mot de passe par défaut |
|---|---|---|
| `admin@helpmi.local` | ADMIN | défini dans le realm export |
| `agent@helpmi.local` | AGENT | défini dans le realm export |
| `client@helpmi.local` | CLIENT | défini dans le realm export |

Ces comptes ne sont utilisés qu'en mode Keycloak (production). En mode `dev`, l'authentification est simulée sans mot de passe.

### Frontend — `frontend/Dockerfile` et `.env.*`

Les fichiers `.env.development` et `.env.production` contiennent encore l'ancien nom de realm Keycloak (`jiralike`) — à mettre à jour si vous n'utilisez pas Docker Compose pour le build (qui surcharge ces valeurs via `ARG`) :

```
VITE_KEYCLOAK_REALM=helpmi
VITE_KEYCLOAK_CLIENT_ID=helpmi-frontend
```

Le `Dockerfile` frontend contient aussi les valeurs par défaut des `ARG` Keycloak — à adapter si vous rebuildez en dehors de Docker Compose.

---

## Tests

```bash
cd backend
mvn test
```

Les tests utilisent Mockito (mode strict) et couvrent les services principaux : `TicketService`, `ProjectService`, `CommentService`, `ClientService`, `LabelService`, `UserService`, `TicketLinkService`, `AdminConfigService`.
