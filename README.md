# HelpMi

Outil de ticketing interne. Gestion de projets, tickets, commentaires, pièces jointes et utilisateurs, avec authentification via Keycloak (ou mode développement sans auth).

![HelpMi](/frontend/src/assets/HelpMi_256.png "HelpMi")

---

## Fonctionnalités

- **Projets** : création, liste, désactivation
- **Organisations** : les utilisateurs non-admin sont rattachés à une organisation qui détermine les projets visibles ; un utilisateur sans organisation voit un écran d'attente jusqu'à ce qu'un admin l'affecte
- **Tickets** : création, édition, changement de statut (machine à états), priorité, type, date d'échéance, assigné, clients, labels
- **Tickets récurrents** : types ANNUEL, MENSUEL, TRIMESTRIEL — à la fermeture, un ticket identique est recréé automatiquement avec la date d'échéance décalée
- **Cloner / Déplacer** : duplication d'un ticket ou déplacement vers un autre projet avec nouvelle référence
- **Liens entre tickets** : BLOCKS, IS_BLOCKED_BY, RELATES_TO, DUPLICATES, etc.
- **Commentaires** : ajout, édition, suppression
- **Pièces jointes** : upload et téléchargement de fichiers
- **Labels** : création à la volée ou depuis l'admin
- **Clients** : rattachement de clients à un ticket
- **Administration** : gestion des valeurs de configuration (statuts, priorités, types, types de liens), clients, labels, organisations
- **Notifications toast** : retours visuels après les actions clés

---

## Stack technique

| Couche | Technologie |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Spring Security (OAuth2 JWT) |
| Persistance | MariaDB 11, Hibernate/JPA, Flyway |
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
│   │   ├── security/               # PersonalTokenFilter, DevAuthFilter, CurrentUserService
│   │   └── config/                 # SecurityConfig, StorageConfig, StartupSafetyCheck
│   └── src/main/resources/
│       ├── application.yml         # Config commune
│       ├── application-dev.yml     # Config mode développement local
│       ├── application-prod.yml    # Config production (variables d'env)
│       └── db/
│           ├── migration/          # Migrations Flyway (V1 → V8)
│           └── dev-seed/           # Données de test (profil dev uniquement)
│
├── frontend/                       # SPA Vue 3
│   └── src/
│       ├── views/                  # Pages (Projets, Ticket, Admin, Profil, …)
│       ├── components/             # Composants réutilisables
│       ├── stores/                 # Pinia (auth, config, toast)
│       ├── services/api.js         # Client Axios centralisé
│       └── router/                 # Routes Vue Router
│
├── keycloak/
│   └── realm-export.json           # Import du realm Keycloak
│
├── docker-compose.yml              # MariaDB + Keycloak + Backend + Frontend (prod)
└── docker-compose.dev.yml          # Surcharge dev : phpMyAdmin (port 8081)
```

---

## Lancer le projet

### Mode développement (sans Docker pour le backend/frontend)

**Prérequis** : Java 21+, Node 20+, Docker

**1. Démarrer MariaDB + phpMyAdmin**

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d mariadb phpmyadmin
```

| Service | URL |
|---|---|
| MariaDB | `localhost:3306` |
| phpMyAdmin | http://localhost:8081 |

> Pour MariaDB seul (sans phpMyAdmin) : `docker compose up -d mariadb`

**2. Démarrer le backend**

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Le profil `dev` désactive l'authentification Keycloak et injecte automatiquement un utilisateur. Les données de test sont insérées via Flyway au premier démarrage.

**Changer d'utilisateur de test** : modifier `app.dev.user-email` dans `backend/src/main/resources/application-dev.yml` puis redémarrer le backend.

| Email | Rôle |
|---|---|
| `admin@helpmi.local` | ADMIN (défaut) |
| `agent@helpmi.local` | AGENT |
| `client@helpmi.local` | CLIENT |

**3. Démarrer le frontend**

> **Important — première utilisation :** `node_modules` n'est pas inclus dans le dépôt.
> Il faut impérativement exécuter `npm install` avant le premier `npm run dev`.

```bash
cd frontend
npm install   # à faire une seule fois (ou après chaque mise à jour de package.json)
npm run dev
```

L'application est disponible sur `http://localhost:5173`. Le proxy Vite redirige `/api` vers `http://localhost:8080`.

---

### Mode production (Docker Compose complet)

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Keycloak | http://localhost:8180 |

Le realm Keycloak `helpmi` est importé automatiquement au premier démarrage.

> **phpMyAdmin** n'est **pas** inclus dans le compose de production. Pour l'activer en développement uniquement, utilisez la commande de surcharge ci-dessus.

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
| `V7__personal_tokens.sql` | Tokens d'accès personnels (PAT) |
| `V8__organizations.sql` | Organisations : table `organizations`, FK sur `users`, table de jointure `organization_projects` |

Les fichiers dans `db/dev-seed/` ne sont chargés qu'avec le profil `dev`.

---

## Variables à personnaliser

### Fichier `.env` (Docker Compose)

Les mots de passe et credentials sont externalisés dans un fichier `.env` à la racine du projet, **non commité** (déjà dans `.gitignore`).

```bash
cp .env.example .env
# puis éditer .env avec vos valeurs
```

| Variable | Rôle |
|---|---|
| `DB_USER` | Utilisateur MariaDB |
| `DB_PASSWORD` | Mot de passe MariaDB |
| `DB_ROOT_PASSWORD` | Mot de passe root MariaDB |
| `KEYCLOAK_ADMIN` | Login admin Keycloak |
| `KEYCLOAK_ADMIN_PASSWORD` | Mot de passe admin Keycloak |
| `APP_CORS_ALLOWED_ORIGINS` | URL publique du frontend (obligatoire en prod) |

> `APP_CORS_ALLOWED_ORIGINS` est **obligatoire** en production. Le backend refuse de démarrer si elle est absente.

### Backend local — `application-dev.yml`

En mode développement (backend lancé hors Docker), les credentials MariaDB restent dans `backend/src/main/resources/application-dev.yml`. Ce fichier n'est pas chargé en production.

```yaml
spring:
  datasource:
    password: helpmi_pass   # ← à synchroniser avec DB_PASSWORD dans .env
```

### Keycloak — `keycloak/realm-export.json`

Le fichier contient des comptes de démonstration. Ces comptes ne sont utilisés qu'en mode Keycloak (production) ; en mode `dev`, l'authentification est simulée sans mot de passe.

| Email | Rôle |
|---|---|
| `admin@helpmi.local` | ADMIN |
| `agent@helpmi.local` | AGENT |
| `client@helpmi.local` | CLIENT |

---

## Tests

### Lancer les tests

```bash
cd backend
mvn test          # tests uniquement
mvn verify        # tests + rapport de couverture JaCoCo
```

Le rapport HTML de couverture est généré dans `backend/target/site/jacoco/index.html`.

### Couverture actuelle

210 tests unitaires Mockito (sans base de données ni contexte Spring).

| Service / Composant | Couverture lignes |
|---|---|
| `LabelService`, `ClientService`, `CommentService` | 100 % |
| `UserService`, `RateLimiterService` | 100 % |
| `PersonalTokenFilter`, `StartupSafetyCheck` | 100 % |
| `ProjectService` | 100 % |
| `TicketLinkService` | 100 % |
| `OrganizationService` | 97 % |
| `TicketService` | 97 % |
| `AdminConfigService`, `PersonalTokenService` | 95–96 % |
| `AttachmentService` | 95 % |
| `GlobalExceptionHandler` | 89 % |
| Couverture globale | **~77 % lignes, ~74 % branches** |

Les controllers ne sont pas couverts (pas de tests d'intégration `@SpringBootTest`).
