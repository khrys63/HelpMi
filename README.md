# HelpMi

Outil de ticketing interne. Gestion de projets, tickets, commentaires, pièces jointes et utilisateurs, avec authentification via Keycloak.

![HelpMi](/frontend/src/assets/HelpMi_256.png "HelpMi")

---

## Fonctionnalités

- **Projets** : création, liste, désactivation
- **Organisations** : les utilisateurs non-admin sont rattachés à une organisation qui détermine les projets visibles ; un utilisateur sans organisation voit un écran d'attente jusqu'à ce qu'un admin l'affecte
- **Tickets** : création, édition, changement de statut (machine à états), priorité, type, date d'échéance, assigné, clients, labels
- **Tickets récurrents** : types ANNUEL, MENSUEL, TRIMESTRIEL — à la fermeture, un ticket identique est recréé automatiquement avec la date d'échéance décalée
- **Cloner / Déplacer** : duplication d'un ticket ou déplacement vers un autre projet avec nouvelle référence
- **Liens entre tickets** : BLOCKS, IS_BLOCKED_BY, RELATES_TO, DUPLICATES, etc. — libellés direct et inverse configurables
- **Commentaires** : ajout, édition, suppression
- **Pièces jointes** : upload et téléchargement via stockage objet S3/MinIO
- **Labels** : création à la volée ou depuis l'admin
- **Clients** : rattachement de clients à un ticket
- **Administration** : gestion des valeurs de configuration (statuts, priorités, types, types de liens), clients, labels, organisations
- **Notifications toast** : retours visuels après les actions clés

---

## Rôles et droits

Il existe deux **rôles globaux** (`ADMIN` et `USER`) et deux **rôles par projet** (`MANAGER` et `MEMBER`).

### Prérequis

Tout utilisateur, y compris ADMIN, doit appartenir à au moins une organisation.  
Un utilisateur sans organisation est bloqué sur un écran d'attente jusqu'à affectation par un admin.  
Chaque utilisateur ne voit que les **projets de sa liste personnelle** (configurée individuellement par un admin avec un rôle par projet).

### Rôles par projet

| Code | Libellé UI | Signification |
|---|---|---|
| `MANAGER` | Gestionnaire | Droits étendus : peut modifier l'assigné d'un ticket en plus de tous les droits MEMBER |
| `MEMBER` | Membre | Droits standard : lecture, création, modification des champs ticket, changement de statut, commentaires |

### Projets

| Action | ADMIN | MANAGER | MEMBER |
|---|---|---|---|
| Voir la liste | Ses projets uniquement | Ses projets | Ses projets |
| Créer / modifier / désactiver | ✅ | ✗ | ✗ |

> À la création d'un projet, l'admin est automatiquement ajouté comme MEMBER.

### Tickets

| Action | ADMIN | MANAGER | MEMBER |
|---|---|---|---|
| Lire (liste + détail) | ✅ | ✅ | ✅ |
| Créer | ✅ | ✅ | ✅ |
| Modifier titre, description, priorité, type, date d'échéance | ✅ | ✅ | ✅ |
| Modifier les organisations et les étiquettes | ✅ | ✅ | ✅ |
| Changer le statut (réouvrir, annuler, clôturer…) | ✅ | ✅ | ✅ |
| Modifier l'assigné | ✅ | ✅ | ✗ (lecture seule) |
| Cloner / déplacer | ✅ | ✅ | ✅ si reporter ou assigné |
| Supprimer | ✅ | ✗ | ✗ |

### Commentaires

| Action | ADMIN | MANAGER | MEMBER |
|---|---|---|---|
| Ajouter | ✅ | ✅ | ✅ |
| Modifier / Supprimer | ✅ n'importe lequel | ✅ les siens | ✅ les siens |

### Pièces jointes

| Action | ADMIN | MANAGER | MEMBER |
|---|---|---|---|
| Uploader / Télécharger | ✅ | ✅ | ✅ |
| Supprimer | ✅ n'importe laquelle | ✅ les siennes | ✅ les siennes |

### Liens entre tickets

| Action | ADMIN | MANAGER | MEMBER |
|---|---|---|---|
| Créer un lien | ✅ | ✅ | ✅ |
| Supprimer un lien | ✅ n'importe lequel | ✅ n'importe lequel | ✅ les siens |

### Utilisateurs

| Action | ADMIN | MANAGER / MEMBER |
|---|---|---|
| Liste des utilisateurs assignables à un projet | ✅ | ✅ |
| Gestion des comptes (rôle, actif/inactif, organisation, projets) | ✅ | ✗ |

### Administration (réservé ADMIN)

- Organisations : CRUD, rattachement projets/utilisateurs
- Labels : CRUD
- Valeurs de configuration (statuts, priorités, types, types de liens, rôles projet) : CRUD

---

## Stack technique

| Couche | Technologie |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Spring Security (OAuth2 JWT) |
| Persistance | MariaDB 11, Hibernate/JPA, Flyway |
| Frontend | Vue 3 (Composition API), Pinia, Vue Router 4, Tailwind CSS, Axios |
| Auth | Keycloak 26 |
| Stockage fichiers | MinIO (S3-compatible) — interchangeable avec AWS S3, Scaleway, OVH… |
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
│   │   ├── security/               # PersonalTokenFilter, CurrentUserService
│   │   ├── storage/                # StorageService (interface) + S3StorageService
│   │   └── config/                 # SecurityConfig, StorageConfig (S3Client)
│   └── src/main/resources/
│       ├── application.yml         # Config commune
│       ├── application-dev.yml     # Config mode développement local
│       ├── application-prod.yml    # Config production (variables d'env)
│       └── db/
│           ├── migration/          # Migrations Flyway (V1 → V10)
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
│   └── realm-export.json           # Import du realm Keycloak (3 comptes de test inclus)
│
├── docker-compose.yml              # MariaDB + Keycloak + MinIO + Backend + Frontend
└── docker-compose.dev.yml          # Surcharge dev : console MinIO (port 9001), phpMyAdmin (port 8081)
```

---

## Lancer le projet

### Mode développement (sans Docker pour le backend/frontend)

**Prérequis** : Java 21+, Node 20+, Docker

**1. Créer le fichier `.env`**

```bash
cp .env.example .env
# puis ajuster les valeurs si nécessaire
```

**2. Démarrer les services d'infrastructure**

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d mariadb keycloak minio phpmyadmin
```

| Service | URL | Rôle |
|---|---|---|
| MariaDB | `localhost:3306` | Base de données |
| Keycloak | `http://auth.localhost` | Authentification |
| MinIO API | `http://localhost:9000` | Stockage des pièces jointes |
| MinIO Console | `http://localhost:9001` | Interface d'administration MinIO |
| phpMyAdmin | `http://localhost:8081` | Interface SQL |

> Pour démarrer uniquement les services strictement nécessaires (sans phpMyAdmin) :
> ```bash
> docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d mariadb keycloak minio
> ```

Le realm Keycloak `helpmi` est importé automatiquement au premier démarrage. Trois comptes de test sont disponibles :

| Utilisateur | Email | Mot de passe | Rôle |
|---|---|---|---|
| admin | `admin@helpmi.local` | `admin123` | ADMIN |
| admin2 | `admin2@helpmi.local` | `admin123` | ADMIN |
| user1 | `user1@helpmi.local` | `user123` | USER |
| user2 | `user2@helpmi.local` | `user123` | USER |

**3. Démarrer le backend**

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Le profil `dev` configure la connexion à MariaDB locale et à Keycloak (port 8180). Les données de test (utilisateurs, projets, tickets) sont insérées via Flyway au premier démarrage.

À la première connexion d'un compte Keycloak, le backend retrouve l'utilisateur pré-seedé par email et met à jour son `keycloakId` — aucune duplication n'est créée.

**4. Démarrer le frontend**

> **Important — première utilisation :** `node_modules` n'est pas inclus dans le dépôt.
> Il faut impérativement exécuter `npm install` avant le premier `npm run dev`.

```bash
cd frontend
npm install   # à faire une seule fois (ou après chaque mise à jour de package.json)
npm run dev
```

L'application est disponible sur `http://localhost:5173`. Le proxy Vite redirige `/api` vers `http://localhost:8080`. La page de connexion Keycloak s'affiche automatiquement.

---

### Mode production (Docker Compose complet)

```bash
cp .env.example .env
# éditer .env avec les vraies valeurs
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Keycloak | http://auth.localhost |
| MinIO API | http://localhost:9000 |

Le realm Keycloak `helpmi` est importé automatiquement au premier démarrage. MinIO crée automatiquement le bucket `helpmi` au premier démarrage du backend.

> **Console MinIO** et **phpMyAdmin** ne sont **pas** inclus dans le compose de production. Pour les activer, utilisez la commande de surcharge dev ci-dessus.

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
| `V9__stand_by_status.sql` | Statut STAND_BY (entre EN_COURS et RÉSOLU) |
| `V10__link_type_inverse_label.sql` | Colonne `inverse_label` sur les types de liens |
| `V11__user_org_role_and_projects.sql` | Table `user_projects` avec PK composite, colonne `organization_role` sur les utilisateurs |
| `V12__project_roles.sql` | Refonte des rôles : `user_projects` avec PK UUID et colonne `role` (GESTIONNAIRE/UTILISATEUR), suppression `organization_role`, migration `AGENT`/`CLIENT` → `USER`, catégorie `PROJECT_ROLE` |
| `V13__rename_project_roles.sql` | Renommage des codes rôles projet : `GESTIONNAIRE` → `MANAGER`, `UTILISATEUR` → `MEMBER` (dans `user_projects` et `config_values`) |

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
| `KEYCLOAK_ADMIN` | Login admin Keycloak intégré |
| `KEYCLOAK_ADMIN_PASSWORD` | Mot de passe admin Keycloak intégré |
| `APP_KEYCLOAK_ISSUER_URI` | Issuer JWT Keycloak (backend) |
| `VITE_KEYCLOAK_URL` | URL Keycloak (frontend) |
| `VITE_KEYCLOAK_REALM` | Nom du realm |
| `VITE_KEYCLOAK_CLIENT_ID` | Client ID OIDC |
| `APP_CORS_ALLOWED_ORIGINS` | URL publique du frontend (obligatoire en prod) |
| `MINIO_ROOT_USER` | Login administrateur MinIO |
| `MINIO_ROOT_PASSWORD` | Mot de passe administrateur MinIO |

> `APP_CORS_ALLOWED_ORIGINS` est **obligatoire** en production. Le backend refuse de démarrer si elle est absente.

### Keycloak externe (production)

Par défaut le compose démarre un Keycloak intégré. Pour utiliser un Keycloak tiers (Auth0, votre propre instance…), définir dans `.env` :

| Variable | Défaut (KC intégré) | Description |
|---|---|---|
| `APP_KEYCLOAK_ISSUER_URI` | `http://keycloak:8080/realms/helpmi` | Issuer JWT — lu par le backend pour valider les tokens |
| `VITE_KEYCLOAK_URL` | `http://auth.localhost` | URL de base Keycloak — utilisée par le frontend au build |
| `VITE_KEYCLOAK_REALM` | `helpmi` | Nom du realm |
| `VITE_KEYCLOAK_CLIENT_ID` | `helpmi-frontend` | Client ID OIDC public |

Puis supprimer le service `keycloak` du compose (et les variables `KEYCLOAK_ADMIN*` devenues inutiles) :

```yaml
# docker-compose.yml — commenter ou supprimer :
# keycloak:
#   image: ...
```

> Le realm doit exposer le rôle `ADMIN` dans le claim JWT `realm_access.roles`. Tout token sans ce rôle est traité comme `USER`.

### Stockage sur un S3 externe (production)

Par défaut le compose utilise le container MinIO intégré. Pour pointer vers un S3 externe (AWS, Scaleway, OVH…), passer les variables suivantes au service `backend` :

| Variable d'environnement | Défaut | Description |
|---|---|---|
| `APP_S3_ENDPOINT` | `http://minio:9000` | URL du endpoint S3 |
| `APP_S3_REGION` | `us-east-1` | Région S3 |
| `APP_S3_BUCKET` | `helpmi` | Nom du bucket |
| `APP_S3_ACCESS_KEY` | *(MINIO_ROOT_USER)* | Access key |
| `APP_S3_SECRET_KEY` | *(MINIO_ROOT_PASSWORD)* | Secret key |

Exemple pour AWS S3 :

```yaml
# dans docker-compose.yml, section environment du service backend :
APP_S3_ENDPOINT: https://s3.amazonaws.com
APP_S3_REGION: eu-west-3
APP_S3_BUCKET: mon-bucket-helpmi
APP_S3_ACCESS_KEY: AKIAIOSFODNN7EXAMPLE
APP_S3_SECRET_KEY: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

> Avec un vrai AWS S3, supprimer le service `minio` du compose et le volume `minio_data`.

### Backend local — `application-dev.yml`

En mode développement (backend lancé hors Docker), les credentials de la base de données et de MinIO sont dans `backend/src/main/resources/application-dev.yml`. Les valeurs S3 doivent correspondre à celles de votre `.env` :

```yaml
spring:
  datasource:
    password: helpmi_pass   # ← doit correspondre à DB_PASSWORD dans .env

app:
  storage:
    s3:
      access-key: helpmi          # ← doit correspondre à MINIO_ROOT_USER dans .env
      secret-key: helpmi_minio_pass  # ← doit correspondre à MINIO_ROOT_PASSWORD dans .env
```

### Keycloak — `keycloak/realm-export.json`

Le fichier contient trois comptes de démonstration importés automatiquement au premier démarrage de Keycloak :

| Email | Mot de passe | Rôle |
|---|---|---|
| `admin@helpmi.local` | `admin123` | ADMIN |
| `admin2@helpmi.local` | `admin123` | ADMIN |
| `user1@helpmi.local` | `user123` | USER |
| `user2@helpmi.local` | `user123` | USER |

Ces comptes correspondent aux utilisateurs pré-seedés en base (profil `dev`). À la première connexion, le backend les retrouve par email et met à jour leur `keycloakId`.

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

267 tests unitaires Mockito (sans base de données ni contexte Spring).

| Service / Composant | Tests |
|---|---|
| `TicketService` | 57 |
| `ProjectService` | 28 |
| `UserService` | 25 |
| `OrganizationService` | 19 |
| `PersonalTokenService` | 18 |
| `TicketLinkService` | 19 |
| `AdminConfigService` | 14 |
| `AttachmentService` | 16 |
| `CommentService`, `LabelService` | 10–12 chacun |
| `PersonalTokenFilter`, `CurrentUserService` | 7, 12 |
| `GlobalExceptionHandler`, `RateLimiterService` | 9, 5 |

Les controllers ne sont pas couverts (pas de tests d'intégration `@SpringBootTest`).
