# HelpMi — Spécification fonctionnelle et technique

> Outil de ticketing de type Jira / Zammad / MantisBT, orienté suivi interne de tickets.
> Version 1.0 — 2026-05-08

---

## 1. Vue d'ensemble

HelpMi est une application web de gestion de tickets destinée aux équipes internes de support et développement. L'application permet de créer, catégoriser, assigner, suivre et clôturer des tickets, avec une gestion fine des projets, des organisations et des rôles.

### Points de contact

| Service | URL dev | URL prod (exemple) |
|---|---|---|
| Application SPA | http://app.localhost | https://app.helpmi.example.com |
| API backend | http://api.localhost | https://api.helpmi.example.com |
| Keycloak | http://auth.localhost | https://auth.helpmi.example.com |
| MinIO API | http://minio.localhost | https://minio.helpmi.example.com |
| phpMyAdmin (dev) | http://pma.localhost | — |
| Mailhog (dev) | http://localhost:8025 | — |

---

## 2. Architecture globale

```
Navigateur
  ├──► app.<domain>  →  Frontend (Nginx / Vue3 SPA)
  │                     └──► /api/*  →  Backend (Spring Boot)
  │                                 ├──► MariaDB
  │                                 ├──► MinIO (S3)
  │                                 └──► Keycloak (validation JWT)
  └──► auth.<domain>  →  Keycloak (login, issuance JWT)
```

Le frontend ne communique **jamais** directement avec MariaDB ou MinIO.

### Services Docker

| Service | Image | Port hôte | Volume | Rôle |
|---|---|---|---|---|
| Traefik | traefik:v3.3 | 80 / 443 | — | Reverse proxy, routage basé sur les labels Docker |
| MariaDB | mariadb:11.8.6 | 3306 | mariadb_data | Persistance applicative |
| Keycloak | keycloak:26.6 | 8180 | — | Authentification OIDC |
| MinIO | minio/minio:latest | 9000 | minio_data | Stockage pièces jointes |
| Backend | helpmi/backend | 8080 | — | API REST + migrations Flyway |
| Frontend | helpmi/frontend | via Traefik | — | SPA Vue3 |
| phpMyAdmin | phpmyadmin:5.2.3 | 8081 | — | dev uniquement |
| Mailhog | mailhog:v1.0.1 | 1025 / 8025 | — | dev uniquement |

### Flux réseau

```
Navigateur (port 3000)
  ├──► /api/* → backend:8080
  │           ├──► mariadb:3306
  │           ├──► minio:9000
  │           └──► keycloak:8080 (validation JWT)
  └──► keycloak:8180 (login, token)
```

### Volumes critiques

| Volume | Service | Contenu | Critique |
|---|---|---|---|
| `mariadb_data` | MariaDB | Toute la base applicative | **OUI** |
| `minio_data` | MinIO | Pièces jointes | **OUI** |

> ⚠️ Ne jamais lancer `docker compose down -v` en production sans sauvegarde préalable.

### Keycloak sans volume

Le realm est réimporté depuis `keycloak/realm-export.json` au démarrage via `--import-realm`. Toute modification manuelle dans Keycloak Admin Console est perdue.

---

## 3. Stack technique

### Backend — Java / Spring Boot

| Brique | Version | Détail |
|---|---|---|
| Java | 21 | Runtime |
| Spring Boot | 3.5.13 | Framework principal |
| Spring Security | — | OAuth2 Resource Server (JWT) |
| Spring Data JPA | — | Accès données |
| Hibernate | — | ORM |
| Flyway | — | Migrations DB |
| MariaDB JDBC | — | Pilote MariaDB |
| AWS S3 SDK v2 | 2.42.41 | Client S3 (S3-compatible) |
| Apache Tika | 2.9.2 | Détection MIME des fichiers |
| Lombok | — | Générateur de code |
| Spring Boot Actuator | — | Health check |
| Spring Mail | — | Envoi SMTP |
| JaCoCo | 0.8.13 | Couverture tests |
| Spring Validation | — | Bean Validation |

### Frontend — Vue 3

| Brique | Version | Détail |
|---|---|---|
| Vue 3 | 3.5.34 | Framework SPA (Composition API) |
| Vue Router | 4.6.4 | Routage client |
| Pinia | 2.2.8 | Store global |
| Vue I18n | 9.14.5 | Internationalisation |
| Axios | 1.16.0 | Client HTTP |
| Keycloak JS | 26.2.4 | SDK client Keycloak |
| ApexCharts | 5.10.6 | Graphiques SVG (donut, bar) |
| Vue3-ApexCharts | 1.11.1 | Composants Vue pour ApexCharts |
| Tailwind CSS | 3.4.14 | CSS utility-first |
| Vite | 6.4.2 | Build tool |

---

## 4. Configuration

### 4.1 Fichier `.env`

Tous les secrets sont externalisés dans `.env` (exclu du dépôt).

| Variable | Valeur dev | Description |
|---|---|---|
| `DB_USER` | helpmi | Utilisateur MariaDB |
| `DB_PASSWORD` | *secret* | Mot de passe MariaDB |
| `DB_ROOT_PASSWORD` | *secret* | Mot de passe root MariaDB |
| `KEYCLOAK_ADMIN` | admin | Login admin Keycloak |
| `KEYCLOAK_ADMIN_PASSWORD` | *secret* | Mot de passe admin Keycloak |
| `APP_KEYCLOAK_ISSUER_URI` | `http://keycloak:8080/realms/helpmi` | Issuer JWT |
| `VITE_KEYCLOAK_URL` | `http://auth.localhost` | URL Keycloak (bake au build) |
| `VITE_KEYCLOAK_REALM` | helpmi | Nom du realm |
| `VITE_KEYCLOAK_CLIENT_ID` | helpmi-frontend | Client ID OIDC |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Origines CORS |
| `MINIO_ROOT_USER` | helpmi | Access key MinIO |
| `MINIO_ROOT_PASSWORD` | *secret* | Secret key MinIO |
| `TRAEFIK_DOMAIN` | localhost | Domaine de base |
| `ACME_EMAIL` | — | Email Let's Encrypt (prod) |

### 4.2 Services SMTP (notifications email)

| Variable | Défaut | Description |
|---|---|---|
| `APP_MAIL_HOST` | localhost | Hôte SMTP |
| `APP_MAIL_PORT` | 1025 | Port SMTP |
| `APP_MAIL_USERNAME` | — | Identifiant SMTP |
| `APP_MAIL_PASSWORD` | — | Mot de passe SMTP |
| `APP_MAIL_SMTP_AUTH` | false | Activer auth SMTP |
| `APP_MAIL_STARTTLS` | false | Activer STARTTLS |
| `APP_MAIL_FROM` | noreply@helpmi.local | Adresse expéditeur |
| `APP_BASE_URL` | http://localhost:5173 | URL frontend (liens emails) |

### 4.3 S3 externe (production)

Pour remplacer MinIO par un S3 tiers (AWS, Scaleway, OVH…) :

| Variable | Défaut | Description |
|---|---|---|
| `APP_S3_ENDPOINT` | `http://minio:9000` | URL endpoint |
| `APP_S3_REGION` | us-east-1 | Région S3 |
| `APP_S3_BUCKET` | helpmi | Nom du bucket |
| `APP_S3_ACCESS_KEY` | *MINIO_ROOT_USER* | Access key |
| `APP_S3_SECRET_KEY` | *MINIO_ROOT_PASSWORD* | Secret key |

### 4.4 Keycloak externe (production)

Pour utiliser un Keycloak tiers :

1. Supprimer le service `keycloak` du compose
2. Remplacer les variables `APP_KEYCLOAK_ISSUER_URI` et `VITE_KEYCLOAK_URL`
3. Le realm doit exposer le rôle `ADMIN` dans le claim JWT `realm_access.roles`

---

## 5. Authentification et gestion des utilisateurs

### 5.1 Mode d'authentification

- **OIDC (OpenID Connect)** via Keycloak
- **Flux PKCE** avec `S256` (code challenge method)
- **Flux standard** (Authorization Code) + **Direct Access Grants** (password grant, activé en dev uniquement)
- **Refresh token** toutes les 30 secondes (tentative `updateToken(60)`)

### 5.2 Personal Access Tokens (PAT)

Les utilisateurs peuvent créer des tokens d'accès personnel pour appeler l'API en ligne de commande.

- **Stockage** : hash SHA-256 dans la table `personal_tokens`
- **Validation** : filtre `PersonalTokenFilter` (au-dessus du filtre JWT) détecte les tokens sans 2 points (un JWT a toujours 2 dots)
- **Exécution** : `last_used_at` mis à jour à chaque authentification
- **Expiry** : optionnel via `expires_at`
- **Audit** : actions `PAT_CREATED`, `PAT_REVOKED`, `PAT_AUTH_SUCCESS`, `PAT_AUTH_FAILURE`

### 5.3 Rôles

#### Rôles globaux (Keycloak realm)

| Rôle | Description |
|---|---|
| `ADMIN` | Accès admin complet + droits utilisateur |
| `USER` | Utilisateur standard |

#### Rôles par projet (application)

| Code | Libellé | Droits |
|---|---|---|
| `MANAGER` | Gestionnaire | Droits étendus (modifier assigné) |
| `MEMBER` | Membre | Droits standard |

### 5.4 Organisations

Les utilisateurs non-admin sont rattachés à une ou plusieurs organisations. Un utilisateur sans organisation voit un écran d'attente (`/pending-org`) jusqu'à ce qu'un admin lui en affecte une.

Les projets sont rattachés à des organisations — un utilisateur ne voit que les projets de ses organisations.

### 5.5 Table de jointure utilisateur → projet

Table `user_projects` : `user_id`, `project_id`, `role` (MANAGER ou MEMBER). Cette table est modifiable par un admin.

### 5.6 Table de jointure utilisateur → organisation

Table `user_organizations` : `user_id`, `organization_id` (many-to-many).

### 5.7 Seed de développement

L'application démarre avec 4 comptes de test importés depuis `keycloak/realm-export.json` et pré-seedés en base :

| Email | Mot de passe | Rôle |
|---|---|---|
| `admin@helpmi.local` | `admin123` | ADMIN |
| `admin2@helpmi.local` | `admin123` | ADMIN |
| `user1@helpmi.local` | `user123` | USER |
| `user2@helpmi.local` | `user123` | USER |

---

## 6. Fonctionnalités de l'application

### 6.1 Projets

**Entité** : `projects` — `id`, `name`, `key` (max 10, unique), `description`, `ticket_sequence`, `created_by`, `active`, `archived`, `created_at`

#### Fonctionnalités

- **Création** : un seul admin peut créer un projet ; il est automatiquement ajouté comme MEMBER
- **Édition** : modification du nom et de la description (la clé est figée)
- **Désactivation** : suppression logique (`active = false`) — irréversible depuis l'interface
- **Archivage** : un projet archivé est invisible pour les non-admins, exclu des dashboards, ne peut plus recevoir de tickets. Archivage réversible.
- **Liste** : affiche nom, clé, nombre de tickets actifs, organisations associées, rôle de l'utilisateur courant. Les projets archivés sont visibles uniquement par les admins avec un badge.

#### Permissions

| Action | Admin | Manager | Membre |
|---|---|---|---|
| Voir la liste | Ses projets | Ses projets | Ses projets |
| Créer / modifier / désactiver | ✅ | ✗ | ✗ |

### 6.2 Organisations

**Entité** : `organizations` — `id`, `name`, `active`, `created_at`

#### Fonctionnalités

- **CRUD** : admin peut créer et supprimer des organisations
- **Rattachement projets** : un admin peut attacher des projets à une organisation
- **Vision** : affichage des gestionnaires et membres rattachés (lecture seule)
- **Recherche utilisateur** : champ de recherche libre sur la liste des utilisateurs

#### Permissions

| Action | Admin |
|---|---|
| CRUD | ✅ |
| Gestion des utilisateurs | ✗ (lecture seule : gestionnaires / membres) |

### 6.3 Utilisateurs

**Entité** : `users` — `id`, `keycloak_id`, `email`, `firstName`, `lastName`, `role`, `active`, `theme`, `locale`, `createdAt`

#### Fonctionnalités

- **Liste** : admin peut filtrer par texte libre
- **Rôle** : changement de rôle (ADMIN / USER)
- **Activation / désactivation** : désactiver un compte ne supprime pas les données
- **Organisations** : affectation à une ou plusieurs organisations
- **Projets et rôles** : affectation à des projets avec rôle (MANAGER ou MEMBER)
- **Recherche** : champ libre pour trouver un utilisateur par nom/email

### 6.4 Tickets

**Entité** : `tickets` — `id`, `reference`, `title`, `description`, `status`, `resolution_type`, `priority`, `type`, `project_id`, `reporter_id`, `assignee_id`, `created_at`, `updated_at`, `due_date`, `closed_at`

#### Champs

| Champ | Description |
|---|---|
| `reference` | Unique, format `PROJ-N` (clé du projet + séquence) |
| `title` | Titre du ticket |
| `description` | Corps du ticket (optionnel) |
| `status` | OPEN, IN_PROGRESS, STAND_BY, RESOLVED, CLOSED, CANCELLED |
| `resolution_type` | CORRECTED, WORKAROUND, ABANDONED, DUPLICATE (uniquement si status = RESOLVED) |
| `priority` | LOW, MEDIUM, HIGH, CRITICAL |
| `type` | TASK, BUG, FEATURE (configurable) |
| `project` | Projet parent |
| `reporter` | Utilisateur ayant créé le ticket |
| `assignee` | Utilisateur assigné au ticket |
| `due_date` | Date d'échéance (facultative) |
| `closed_at` | Date de clôture |
| `organizations` | Organisations concernées (many-to-many) |
| `labels` | Étiquettes (many-to-many) |
| `watchers` | Observateurs (many-to-many) |

#### Machine à états

```
OPEN ──────────────────────────────────────────┐
  │                                            │
  ├──► IN_PROGRESS ──► STAND_BY ──► OPEN       │
  │         │               │                  ▼
  │         │               └──► IN_PROGRESS  CANCELLED (figé)
  │         │                                  │
  │         └──► RESOLVED ──► OPEN             │
  │                   │                        │
  ├──► STAND_BY       └──► CLOSED (figé)       │
  │                              │             │
  └──► CANCELLED                 └─────────────┴──► OPEN
```

| Depuis \ Vers | OPEN | IN_PROGRESS | STAND_BY | RESOLVED | CLOSED | CANCELLED |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **OPEN** | — | ✅ | ✅ | | | ✅ |
| **IN_PROGRESS** | | — | ✅ | ✅ | | ✅ |
| **STAND_BY** | ✅ | ✅ | — | | | ✅ |
| **RESOLVED** | ✅ | | | — | ✅ | |
| **CLOSED** *(figé)* | ✅ | | | | — | |
| **CANCELLED** *(figé)* | ✅ | | | | | — |

#### Résolution et réouverture

- **Résolution** : quand on résout un ticket, on doit saisir un `resolution_type` (obligatoire, défaut "Corrigé") et un commentaire optionnel.
- **Réouverture** : quand on réouvre un ticket fermé ou annulé, un commentaire est obligatoire.

#### Tickets figés (CLOSED et CANCELLED)

Un ticket clôturé ou annulé est figé : tous ses champs sont en lecture seule. La seule action autorisée est la réouverture.

#### Tickets récurrents (ANNUEL, MENSUEL, TRIMESTRIEL)

- Types de ticket périodiques
- À la fermeture (`CLOSED`) d'un ticket récurrent, un ticket clone est automatiquement créé avec la date d'échéance décalée (mois, trimestre, année)
- Le clonage ne se déclenche pas pour les statuts CANCELLED ou RESOLVED

#### Clonage et déplacement

- **Cloner** : duplique un ticket sur le même projet, avec tous les champs copiés sauf les tickets liés
- **Déplacer** : déplace un ticket vers un autre projet. Le ticket obtient une nouvelle référence dans le projet cible. L'ancienne référence devient invalide et non réutilisable.

#### Filtrage

- **Multi-sélection** pour statut, priorité et type (listes déroulantes)
- **Filtre assigné** : par utilisateur
- Les valeurs `CANCELLED` et `CLOSED` sont décochées par défaut

### 6.5 Commentaires

**Entité** : `comments` — `id`, `ticket_id`, `author_id`, `body`, `edited`, `created_at`, `updated_at`

#### Permissions

| Action | Admin | Manager | Membre |
|---|---|---|---|
| Ajouter | ✅ | ✅ | ✅ |
| Modifier / Supprimer | ✅ n'importe lequel | ✅ les siens | ✅ les siens |

- Chaque commentaire affiche son horodatage (création + modification)
- Les commentaires peuvent être modifiés et supprimés selon les règles ci-dessus

### 6.6 Pièces jointes

**Entité** : `attachments` — `id`, `ticket_id`, `file_name`, `stored_name`, `content_type`, `size`, `uploaded_by`, `uploaded_at`

#### Fonctionnalités

- Upload via l'API (taille max : 50MB par requête, 10MB par fichier)
- Téléchargement via URL signée `/api/attachments/{id}`
- Suppression (admin supprime n'importe quelle PJ, gestionnaire et membre seulement les leurs)
- Validation du type MIME par Apache Tika (analyse binaire, pas le nom de fichier)
- Stockage avec nom unique (UUID) dans le bucket S3/MinIO

### 6.7 Liens entre tickets

**Entité** : `ticket_links` — `id`, `source_ticket_id`, `target_ticket_id`, `link_type`, `created_by`, `created_at`

#### Types de liens

Types de liens configurables (ajout, modification, suppression en admin) :

| Type | Libellé direct | Libellé inverse |
|---|---|---|
| BLOCKS | bloque | est bloqué par |
| IS_BLOCKED_BY | est bloqué par | bloque |
| RELATES_TO | est lié à | est lié à |
| DUPLICATES | duplique | est dupliqué par |
| ... | ... | ... |

#### Permissions

| Action | Admin | Manager | Membre |
|---|---|---|---|
| Créer un lien | ✅ | ✅ | ✅ |
| Supprimer un lien | ✅ n'importe lequel | ✅ n'importe lequel | ✅ les siens |

#### Configuration

- Les types de liens sont configurés par paires (direct + inverse)
- Chaque type a un libellé configurable en fr, en, bg
- Les libellés directs et inverses sont saisis séparément

### 6.8 Labels (étiquettes)

**Entité** : `labels` — `id`, `name`, `color`, `created_at`

- CRUD en admin
- Création à la volée à partir de l'écran de ticket (si le label n'existe pas, il est créé)
- Multisélection à l'attachement d'un ticket
- Couleur configurable

### 6.9 Watchers (observateurs)

- On peut ajouter/supprimer des utilisateurs comme observateurs d'un ticket
- Les watchers reçoivent des notifications email (commentaire, changement de statut)
- On ne peut ajouter que des utilisateurs ayant un rôle dans le projet ou dans les organisations concernées

### 6.10 Notifications email

#### Événements déclencheurs

| Événement | Destinataires |
|---|---|
| Ticket assigné à un nouvel utilisateur | Le nouvel assigné uniquement |
| Commentaire ajouté | Reporter + assigné + watchers du ticket (exclut l'auteur) |
| Statut modifié | Reporter + assigné + watchers du ticket (exclut l'acteur) |
| Utilisateur ajouté comme watcher | Les watchers nouvellement ajoutés uniquement |
| Ticket créé | Tous les gestionnaires (MANAGER) du projet |

#### Règles d'envoi

- **Exclusion de l'acteur** : la personne qui effectue l'action ne reçoit jamais d'email
- **Compte inactif** : un utilisateur désactivé ne reçoit aucune notification
- **Préférences individuelles** : chaque utilisateur contrôle ses 5 types de notification depuis son profil
- **Envoi asynchrone** : via `@Async("mailExecutor")` — une notification ratée ne fait jamais échouer la transaction
- **`sendSafe`** : absorbe toutes les exceptions

### 6.11 Journal d'audit

**Entité** : `audit_log` — `id`, `action`, `actor_id`, `actor_email`, `target_type`, `target_id`, `details`, `ip_address`, `created_at`

#### Actions auditées

| Action | Déclencheur |
|---|---|
| `PAT_CREATED` / `PAT_REVOKED` | Gestion des tokens d'accès personnels |
| `PAT_AUTH_SUCCESS` / `PAT_AUTH_FAILURE` | Authentification via PAT |
| `USER_ROLE_CHANGED` | Changement de rôle par un admin |
| `USER_ACTIVATED` / `USER_DEACTIVATED` | Activation/désactivation d'un compte |
| `USER_ORG_ADDED` / `USER_ORG_REMOVED` | Modification des organisations |
| `USER_PROJECTS_UPDATED` | Modification des projets/rôles |
| `PROJECT_CREATED` | Création d'un projet |
| `PROJECT_ARCHIVED` / `PROJECT_UNARCHIVED` | Archivage / désarchivage |
| `ORGANIZATION_CREATED` / `ORGANIZATION_DELETED` | Création / suppression d'une organisation |
| `TICKET_DELETED` | Suppression d'un ticket |
| `ATTACHMENT_DELETED` | Suppression d'une pièce jointe |
| `ACCESS_DENIED` | Tentative d'accès refusée |

#### Consultation

- Interface : **Administration → Journal** (filtres, 50 entrées/page)
- SQL : `SELECT * FROM audit_log ORDER BY created_at DESC`
- Purge : entrées de plus de 6 mois (manuelle)

---

## 7. Configuration de l'application

### 7.1 Valeurs configurables

Les valeurs suivantes sont configurables en admin :

- **Statuts** : codes, libellés (fr/en/bg), couleur, position, actif
- **Priorités** : codes, libellés, couleur, position
- **Types de ticket** : codes, libellés, couleur, position
- **Types de liens** : paires direct/inverse, libellés (fr/en/bg), couleur
- **Rôles projet** : codes, libellés (fr/en/bg), couleur
- **Labels** : noms, couleurs
- **Organisations** : noms

### 7.2 Traductions

Toutes les valeurs de configuration supportent les libellés en 3 langues :
- Français (fr) — libellé par défaut
- Anglais (en) — `labelEn`
- Bulgare (bg) — `labelBg`

Le libellé affiché est celui correspondant à la langue de l'utilisateur courant.

---

## 8. Dashboards

### 8.1 Dashboard utilisateur (`/dashboard`)

Sections :

| Section | Contenu |
|---|---|
| Mes tickets ouverts | Tickets où je suis reporter (hors CLOSED, CANCELLED, RESOLVED) |
| Tickets assignés à moi | Tickets où je suis assigné (hors CLOSED, CANCELLED, RESOLVED) |
| Tickets observés | Tickets où je suis watcher (hors CLOSED, CANCELLED, RESOLVED) |
| Tickets à venir | Tickets avec date d'échéance proche (hors CLOSED, CANCELLED, RESOLVED) |
| Statistiques projets | Compteurs par projet et statut (OPEN, IN_PROGRESS, STAND_BY) |

### 8.2 Suivi gestionnaire (`/dashboard/managers`)

Visible uniquement pour les utilisateurs qui sont MANAGER sur au moins un projet actif.

Structure :

```
Suivi gestionnaire
├─ Graphique intro : tickets ouverts par projet (bar chart ApexCharts)
└─ Projets
   ├─ Projet A (PRJ)
   │  ├─ Assignés
   │  │  ├─ Alice Dupont
   │  │  │  ├─ Statistiques : Total / Ouverts / En cours / En attente
   │  │  │  ├─ Liste des tickets (référence, statut, titre)
   │  │  │  └─ Graphique donut : répartition des statuts
   │  │  └─ Bob Martin
   │  │     └─ ...
   │  └─ Non assignés
   │     └─ Liste des tickets sans assigné
   └─ Projet B
```

### 8.3 Écran des projets (`/projects`)

Liste des projets avec : nom, clé, nombre de tickets actifs, organisations associées, rôle de l'utilisateur courant.

---

## 9. Profil utilisateur (`/profile`)

Section **Préférences** :

| Champ | Description |
|---|---|
| Thème | Clair / Sombre (persisté par utilisateur) |
| Langue | Français / English / Български |
| Notifications email | 5 toggles (assigné, commentaire, statut, watcher, ticket créé) |
| Organisations rattachées | Lecture seule |
| Projets et rôles | Lecture seule |
| Déconnexion | Bouton vers `keycloak.logout()` |

---

## 10. Interface d'administration

### 10.1 Configurations (`/admin/config`)

Gestion des valeurs de configuration (CRUD) :

- Statuts (codes, libellés, couleurs)
- Priorités
- Types de ticket
- Types de liens (par paires)
- Rôles projet
- Labels

### 10.2 Organisations (`/admin/organizations`)

CRUD des organisations, rattachement de projets, vision des utilisateurs (gestionnaires / membres).

### 10.3 Utilisateurs (`/admin/users`)

Gestion des comptes : rôle, activation, organisation, projets et rôles. Recherche par texte libre.

### 10.4 Journal d'audit (`/admin/audit`)

Consultation des entrées d'audit avec filtres.

---

## 11. Routeur frontend

### 11.1 Routes

| Route | Composant | Accès |
|---|---|---|
| `/` | → `/dashboard` | Connecté |
| `/dashboard` | `DashboardView` | Connecté |
| `/dashboard/managers` | `ManagerTrackingView` | Connecté + MANAGER |
| `/pending-org` | `PendingOrgView` | Connecté sans org |
| `/projects` | `ProjectsView` | Connecté |
| `/projects/:projectId` | `ProjectDetailView` | Connecté + projet accessible |
| `/projects/:projectId/tickets/new` | `CreateTicketView` | Connecté |
| `/projects/:projectId/tickets/:ticketId` | `TicketDetailView` | Connecté |
| `/admin/*` | Admin views | Admin uniquement |
| `/profile` | `ProfileView` | Connecté |

### 11.2 Navigation guard

```javascript
beforeEach((to) => {
  if (to.name === 'pending-org') return true
  
  const isAdmin = auth.user.role === 'ADMIN'
  const hasOrg = auth.user.organizations?.length > 0

  if (to.meta.requiresAdmin && !isAdmin) {
    return hasOrg ? { name: 'projects' } : { name: 'pending-org' }
  }

  if (!isAdmin && !hasOrg) {
    return { name: 'pending-org' }
  }

  return true
})
```

Un utilisateur non-admin sans organisation est redirigé vers `/pending-org` sur **toutes** les routes.

---

## 12. Modèle de données (migrations)

### 12.1 Historique des migrations

| Migration | Description |
|---|---|
| V1 | Schéma initial (projets, tickets, commentaires, utilisateurs, etc.) |
| V2 | Liens entre tickets (`ticket_links`) |
| V3 | Valeurs de configuration (`config_values`) |
| V4 | Clients et labels (`clients`, `labels`, `ticket_labels`) |
| V5 | Date d'échéance sur les tickets (`due_date`) |
| V6 | Types de tickets récurrents (ANNUEL, MENSUEL, TRIMESTRIEL) |
| V7 | Personal Access Tokens |
| V8 | Organisations (`organizations`, `user_organizations`, `organization_projects`) |
| V9 | Statut STAND_BY |
| V10 | Libellé inverse configurable pour les types de liens |
| V11 | Rôle utilisateur par projet + rôle dans l'organisation |
| V12 | Rôles projet (MANAGER / MEMBER) |
| V13 | Renommage des rôles projet |
| V14 | Thème utilisateur (`user_theme`) |
| V15 | Locale utilisateur (`user_locale`) |
| V16 | Traductions sur les valeurs de configuration (`labelEn`, `labelBg`) |
| V17 | Données de traduction pour les valeurs initiales |
| V18 | Historique des tickets (`ticket_history`) |
| V19 | Multi-organisation sur les tickets (`ticket_organizations`) |
| V20 | Watchers de tickets (`ticket_watchers`) |
| V21 | Préférences de notification utilisateur |
| V22 | Notification ticket créé |
| V23 | Type de résolution (`resolution_type`) |
| V24 | Journal d'audit (`audit_log`) |
| V25 | Archivage des projets (`archived`) |

### 12.2 Tables principales

**`users`**
```
id (UUID), keycloak_id (unique), email (unique),
firstName, lastName, role (enum), active,
theme, locale, createdAt
```

**`user_organizations`**
```
user_id, organization_id  (many-to-many)
```

**`user_projects`**
```
user_id, project_id, role (MANAGER/MEMBER)
```

**`projects`**
```
id, name, key (unique, 10), description,
ticket_sequence, created_by_id, active, archived, createdAt
```

**`organizations`**
```
id, name, active, createdAt
```

**`organization_projects`**
```
organization_id, project_id  (many-to-many)
```

**`tickets`**
```
id, reference (unique), title, description,
status, resolution_type, priority, type,
project_id, reporter_id, assignee_id,
created_at, updated_at, due_date, closed_at
```

**`ticket_organizations`**
```
ticket_id, organization_id
```

**`ticket_labels`**
```
ticket_id, label_id
```

**`ticket_watchers`**
```
ticket_id, user_id
```

**`ticket_links`**
```
id, source_ticket_id, target_ticket_id,
link_type, created_by_id, created_at
```

**`ticket_history`**
```
id, ticket_id, changed_by_id, changed_at,
field, oldValue, newValue
```

**`comments`**
```
id, ticket_id, author_id, body,
edited, createdAt, updatedAt
```

**`attachments`**
```
id, ticket_id, file_name, stored_name (unique),
content_type, size, uploaded_by_id, uploaded_at
```

**`config_values`**
```
id, category, code, label, labelEn, labelBg,
inverseLabel, inverseLabelEn, inverseLabelBg,
color, active, position
```

**`labels`**
```
id, name (unique), color, createdAt
```

**`personal_tokens`**
```
id, user_id, name, token_hash (unique),
createdAt, lastUsedAt, expiresAt
```

**`audit_log`**
```
id, action (enum), actor_id, actor_email,
target_type, target_id, details, ip_address, createdAt
```

---

## 13. API REST — Endpoints principaux

### 13.1 Tickets

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/api/projects/{projectId}/tickets` | Liste paginée avec filtres |
| GET | `/api/projects/{projectId}/tickets/{ticketId}` | Détail d'un ticket |
| POST | `/api/projects/{projectId}/tickets` | Créer un ticket |
| PATCH | `/api/tickets/{ticketId}` | Modifier un ticket |
| PATCH | `/api/tickets/{ticketId}/status` | Changer le statut |
| POST | `/api/tickets/{ticketId}/clone` | Cloner un ticket |
| POST | `/api/tickets/{ticketId}/move/{targetProjectId}` | Déplacer un ticket |

Filtres : `status`, `priority`, `type`, `assigneeId` (comma-separated, max 20 valeurs).

### 13.2 Commentaires

| Méthode | Endpoint | Description |
|---|---|---|
| POST | `/api/tickets/{ticketId}/comments` | Ajouter un commentaire |
| PATCH | `/api/comments/{commentId}` | Modifier un commentaire |
| DELETE | `/api/comments/{commentId}` | Supprimer un commentaire |

### 13.3 Attachements

| Méthode | Endpoint | Description |
|---|---|---|
| POST | `/api/tickets/{ticketId}/attachments` | Upload une pièce jointe |
| GET | `/api/attachments/{attachmentId}` | Télécharger |
| DELETE | `/api/attachments/{attachmentId}` | Supprimer |

### 13.4 Liens

| Méthode | Endpoint | Description |
|---|---|---|
| POST | `/api/tickets/{ticketId}/links` | Créer un lien |
| DELETE | `/api/tickets/{sourceId}/links/{linkId}` | Supprimer un lien |

### 13.5 Utilisateurs

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/api/users/me` | Profil courant |
| PATCH | `/api/users/me/theme` | Changer le thème |
| PATCH | `/api/users/me/locale` | Changer la locale |
| PATCH | `/api/users/me/notifications` | Changer les préférences |
| GET | `/api/users` | Liste (admin) |
| PATCH | `/api/users/{userId}` | Modifier (admin) |

### 13.6 Personal Tokens

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/api/tokens` | Liste des tokens |
| POST | `/api/tokens` | Créer un token |
| DELETE | `/api/tokens/{tokenId}` | Révoquer un token |

### 13.7 Admin

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/config/{category}` | Valeurs de configuration |
| POST | `/api/admin/config/{category}` | Créer |
| PATCH | `/api/admin/config/{id}` | Modifier |
| DELETE | `/api/admin/config/{id}` | Supprimer |
| GET | `/api/admin/organizations` | Liste des orgs |
| POST | `/api/admin/organizations` | Créer |
| DELETE | `/api/admin/organizations/{id}` | Supprimer |
| GET | `/api/admin/users` | Liste des utilisateurs |
| PATCH | `/api/admin/users/{userId}` | Modifier |
| GET | `/api/admin/audit` | Journal d'audit |

### 13.8 Dashboard

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/api/dashboard` | Dashboard utilisateur |
| GET | `/api/dashboard/managers` | Suivi gestionnaire |

### 13.9 Projets

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/api/projects` | Liste des projets |
| GET | `/api/projects/{id}` | Détail |
| POST | `/api/projects` | Créer |
| PATCH | `/api/projects/{id}` | Modifier |
| PATCH | `/api/projects/{id}/archive` | Archiver |
| PATCH | `/api/projects/{id}/unarchive` | Désarchiver |

### 13.10 Watchers

| Méthode | Endpoint | Description |
|---|---|---|
| PATCH | `/api/tickets/{ticketId}/watchers` | Gérer les watchers |

---

## 14. Sécurité

### 14.1 Authentification

- **OAuth2 Resource Server** avec validation JWT via Keycloak
- **PKCE S256** pour le flux client
- **Personal Access Tokens** (hash SHA-256) via filtre `PersonalTokenFilter`
- **Session stateless** (pas de sessions serveur)
- **Refresh token** toutes les 30 secondes côté frontend

### 14.2 Autorisation

- **Par rôle global** (ADMIN / USER)
- **Par projet** (MANAGER / MEMBER)
- **Par projet accessible** : vérification de la table `user_projects`
- **Par projet de l'organisation** : vérification de la table `organization_projects`
- **Accès denied handler** : retourne un audit log + 403

### 14.3 En-têtes de sécurité

| En-tête | Valeur |
|---|---|
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` |

### 14.4 CORS

Origines configurables via `APP_CORS_ALLOWED_ORIGINS`. En-têtes autorisés : `Content-Type`, `Authorization`, `Accept`.

### 14.5 Validation des entrées

- **Allowlist** pour les filtres de tickets (statut, priorité, type) — max 20 valeurs
- **Type MIME** validé par Apache Tika (analyse binaire)
- **Taille** : 50MB par requête, 10MB par fichier

### 14.6 Rate Limiting

Service `RateLimiterService` pour protéger les endpoints critiques.

---

## 15. Internationalisation (i18n)

### 15.1 Langues supportées

- Français (`fr`)
- English (`en`)
- Български (`bg`)

### 15.2 Fonctionnement

- **Par utilisateur** : la langue est stockée dans `users.locale` et appliquée au login
- **Libellés de configuration** : chaque `config_value` a `label`, `labelEn`, `labelBg`
- **Frontend** : `vue-i18n` avec fichiers de traduction dans `frontend/src/locales/`
- **Clés de traduction** dans `fr.json`, `en.json`, `bg.json`

### 15.3 Clés de traduction principales

Sections de l'application localisées :
- `common` (chargement, annuler, confirmer, etc.)
- `nav` (navigation)
- `dashboard` (sections du dashboard)
- `tickets` (champs, statuts, priorités, types, actions)
- `projects` (champs de projet)
- `admin` (écrans admin)
- `profile` (préférences utilisateur)
- `manager_tracking` (suivi gestionnaire)
- `audit` (journal d'audit)

---

## 16. Tests

### 16.1 Couverture

267 tests unitaires Mockito (sans contexte Spring ni base de données) :

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
| `CommentService`, `LabelService` | 10–12 |
| `PersonalTokenFilter`, `CurrentUserService` | 7, 12 |
| `GlobalExceptionHandler`, `RateLimiterService` | 9, 5 |

### 16.2 Outils

- **JUnit 5** (Jupiter)
- **AssertJ** pour les assertions
- **Mockito** pour les mocks
- **JaCoCo** pour la couverture (rapport HTML dans `backend/target/site/jacoco/index.html`)

### 16.3 Lancement

```bash
cd backend
mvn test          # tests uniquement
mvn verify        # tests + rapport de couverture
```

---

## 17. Plan de déploiement

### 17.1 Prérequis

- **Dev** : Java 21+, Node 20+, Docker
- **Prod** : Docker, Docker Compose, nom de domaine, certificat SSL

### 17.2 Déploiement

```bash
# 1. Sauvegarde obligatoire
docker exec helpmi_mariadb sh -c \
  'mysqldump -u root -p$MYSQL_ROOT_PASSWORD --single-transaction helpmi' \
  > backup_before_deploy.sql

# 2. Mettre à jour les sources
git pull origin main

# 3. Rebuild et redémarrage
docker compose build --no-cache backend frontend
docker compose up -d --force-recreate backend frontend

# 4. Vérifier les migrations Flyway
docker logs helpmi_backend 2>&1 | grep -E "Flyway|migration"

# 5. Smoke test
curl -s http://localhost:8080/actuator/health
curl -I http://localhost:3000
```

### 17.3 Sauvegarde

**MariaDB** :
```bash
docker exec helpmi_mariadb sh -c \
  'mysqldump -u root -p$MYSQL_ROOT_PASSWORD --single-transaction helpmi' \
  > backup_$(date +%Y%m%d_%H%M%S).sql
```

**MinIO** :
```bash
docker run --rm \
  -v minio_data:/source:ro \
  -v $(pwd)/backup_minio:/backup \
  alpine tar czf /backup/minio_$(date +%Y%m%d).tar.gz -C /source .
```

### 17.4 HTTPS (production)

1. DNS pointant vers le serveur
2. Définir `TRAEFIK_DOMAIN` et `ACME_EMAIL` dans `.env`
3. Décommenter les blocs HTTPS dans le compose
4. Créer `traefik/letsencrypt/acme.json` (chmod 600)
5. Rebuild du frontend (VITE_KEYCLOAK_URL est bake)
6. `docker compose up -d`

---

## 18. Structure du projet

```
.
├── backend/
│   ├── src/main/java/com/helpmi/
│   │   ├── controller/
│   │   │   ├── AdminAuditController.java
│   │   │   ├── AdminConfigController.java
│   │   │   ├── AdminLabelController.java
│   │   │   ├── AdminOrganizationController.java
│   │   │   ├── AdminUserController.java
│   │   │   ├── AttachmentController.java
│   │   │   ├── CommentController.java
│   │   │   ├── DashboardController.java
│   │   │   ├── ManagerTrackingController.java
│   │   │   ├── PersonalTokenController.java
│   │   │   ├── ProjectController.java
│   │   │   ├── TicketController.java
│   │   │   ├── TicketLinkController.java
│   │   │   └── UserController.java
│   │   ├── service/
│   │   │   ├── AdminConfigService.java
│   │   │   ├── AuditService.java
│   │   │   ├── AttachmentService.java
│   │   │   ├── CommentService.java
│   │   │   ├── DashboardService.java
│   │   │   ├── LabelService.java
│   │   │   ├── ManagerTrackingService.java
│   │   │   ├── NotificationService.java
│   │   │   ├── OrganizationService.java
│   │   │   ├── PersonalTokenService.java
│   │   │   ├── ProjectService.java
│   │   │   ├── RateLimiterService.java
│   │   │   ├── TicketHistoryService.java
│   │   │   ├── TicketLinkService.java
│   │   │   ├── TicketService.java
│   │   │   └── UserService.java
│   │   ├── domain/
│   │   │   ├── Attachment.java
│   │   │   ├── AuditLog.java
│   │   │   ├── Comment.java
│   │   │   ├── ConfigValue.java
│   │   │   ├── Label.java
│   │   │   ├── Organization.java
│   │   │   ├── PersonalToken.java
│   │   │   ├── Project.java
│   │   │   ├── Ticket.java
│   │   │   ├── TicketHistory.java
│   │   │   ├── TicketLink.java
│   │   │   ├── User.java
│   │   │   └── UserProject.java
│   │   ├── repository/
│   │   │   ├── (15 fichiers JPA)
│   │   │   └── TicketRepository.java  (60+ requêtes JPQL)
│   │   ├── security/
│   │   │   ├── AuditingAccessDeniedHandler.java
│   │   │   ├── CurrentUserService.java
│   │   │   └── PersonalTokenFilter.java
│   │   ├── storage/
│   │   │   ├── S3StorageService.java
│   │   │   └── StorageService.java
│   │   └── config/
│   │       ├── AsyncConfig.java
│   │       ├── SecurityConfig.java
│   │       └── StorageConfig.java
│   └── src/main/resources/
│       ├── db/migration/   (V25 fichiers)
│       └── db/dev-seed/    (6 fichiers)
├── frontend/
│   ├── src/
│   │   ├── views/          (13 vues)
│   │   ├── components/     (layout, tickets, comments, common, ui)
│   │   ├── stores/         (auth, config, locale, theme, toast)
│   │   ├── services/       (api.js)
│   │   ├── router/         (index.js)
│   │   ├── locales/        (fr.json, en.json, bg.json)
│   │   ├── utils/          (dates.js)
│   │   └── assets/         (HelpMi logo, main.css)
│   └── Dockerfile
├── keycloak/
│   └── realm-export.json
├── docker-compose.yml
├── docker-compose.dev.yml
├── .env.example
└── ops/
    ├── runbook.html
    └── runbook.md
```

---

## 19. Comptes de test

Ces comptes sont seedés dans Keycloak et en base via les migrations `dev-seed` :

| Email | Mot de passe | Rôle |
|---|---|---|
| `admin@helpmi.local` | `admin123` | ADMIN |
| `admin2@helpmi.local` | `admin123` | ADMIN |
| `user1@helpmi.local` | `user123` | USER |
| `user2@helpmi.local` | `user123` | USER |

---

## 20. Glossaire

| Terme | Définition |
|---|---|
| Ticket | Élément de base de suivi — une demande, un bug, une tâche |
| Projet | Groupe logique de tickets |
| Organisation | Groupe d'utilisateurs — détermine les projets visibles |
| Watcher | Utilisateur observant un ticket (notifications) |
| Label (étiquette) | Tag libre attaché à un ticket |
| Client (historique) | Organisation rattachée à un ticket (remplacé par multi-org) |
| Ticket figé | Ticket dans un état où tous les champs sont en lecture seule |
| PAT | Personal Access Token — token pour l'API en ligne de commande |
