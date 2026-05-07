# HelpMi — Runbook d'exploitation

> Document destiné à un DevOps intervenant sans connaissance préalable de l'application.
> Version 1.2 — 2026-05-07

---

## Sommaire

- [Architecture](#architecture)
- [Incidents P1 — Critiques](#incidents-p1--critiques)
- [Incidents P2 — Majeurs](#incidents-p2--majeurs)
- [Incidents P3 — Modérés](#incidents-p3--modérés)
- [Incidents P4 — Mineurs](#incidents-p4--mineurs)
- [Opérations routinières](#opérations-routinières)
- [Sauvegarde & restauration](#sauvegarde--restauration)
- [Redéploiement](#redéploiement)
- [Journal d'audit](#journal-daudit)
- [Archivage des projets](#archivage-des-projets)
- [Traefik](#traefik)
- [Variables d'environnement](#variables-denvironnement)
- [Quick reference](#quick-reference)

---

## Architecture

### URLs d'accès

| Service | URL dev | URL prod exemple | Remarque |
|---|---|---|---|
| Application | http://app.localhost | https://app.helpmi.example.com | SPA Vue 3 |
| API backend | http://api.localhost | https://api.helpmi.example.com | REST Spring Boot |
| Keycloak | http://auth.localhost | https://auth.helpmi.example.com | Admin : /admin |
| MinIO API | http://minio.localhost | https://minio.helpmi.example.com | S3-compatible |
| MinIO Console | http://minio-console.localhost | — | dev uniquement |
| phpMyAdmin | http://pma.localhost | — | dev uniquement |
| Traefik dashboard | http://traefik.localhost ou :8090 | — | état du routage |

> **`TRAEFIK_DOMAIN`** dans `.env` : changer `localhost` en `helpmi.example.com` bascule toutes les URLs.
> Le frontend doit être **rebuild** car `VITE_KEYCLOAK_URL` est baked dans l'image au build.

### Services & ports

| Service | Conteneur | Port hôte | Volume | Rôle |
|---|---|---|---|---|
| MariaDB 11 | helpmi_mariadb | 3306 | mariadb_data | Persistence applicative |
| Keycloak 26.6 | helpmi_keycloak | 8180 | aucun (stateless*) | Authentification / SSO |
| MinIO | helpmi_minio | 9000 / 9001 (console dev) | minio_data | Stockage pièces jointes |
| Backend Spring Boot | helpmi_backend | 8080 | — | API REST + migrations Flyway |
| Traefik v3 | helpmi_traefik | 80 / :8090 dashboard | — | Reverse proxy / SSL |
| Frontend Vue3/Nginx | helpmi_frontend | via Traefik | — | SPA + proxy /api |
| phpMyAdmin | helpmi_phpmyadmin | 8081 | — | dev uniquement |

> **\* Keycloak sans volume :** le realm est réimporté depuis `keycloak/realm-export.json` à chaque démarrage via `--import-realm`.
> Toute modification manuelle dans l'admin Keycloak est perdue au `compose down` — reporter dans le fichier d'export.

### Flux réseau

```
Navigateur
  ├──► :3000  helpmi_frontend (Nginx)
  │           └──► /api/* ──► helpmi_backend :8080
  │                           ├──► helpmi_mariadb :3306
  │                           ├──► helpmi_minio   :9000
  │                           └──► helpmi_keycloak:8080 (validation JWT)
  └──► :8180  helpmi_keycloak (login, token)
```

Le frontend ne communique **jamais** directement avec MariaDB ou MinIO.

### Volumes critiques

| Volume | Service | Contenu | Critique |
|---|---|---|---|
| `mariadb_data` | MariaDB | Toute la base applicative | **OUI** |
| `minio_data` | MinIO | Pièces jointes | **OUI** |

> **⚠️ Ne jamais lancer** `docker compose down -v` en production sans sauvegarde préalable.

---

## Incidents P1 — Critiques

Application totalement inaccessible. Intervention immédiate requise.

### Un ou plusieurs conteneurs sont arrêtés / en crash loop

```bash
# Diagnostic rapide
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Logs du conteneur défaillant
docker logs helpmi_backend --tail 100
docker logs helpmi_backend 2>&1 | grep -i "error\|exception\|fatal"

# Redémarrer le service
docker compose restart backend
# ou
docker compose up -d

# Vérifier après redémarrage
docker ps --format "table {{.Names}}\t{{.Status}}"
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
```

> Si le backend redémarre en boucle, chercher en priorité une erreur Flyway ou une impossibilité de contacter MariaDB.

### MariaDB ne démarre pas

```bash
docker logs helpmi_mariadb --tail 200
# Chercher : "InnoDB: Unable to lock ./ibdata1" (processus orphelin)
#            "Table is marked as crashed"

# Cas processus orphelin
docker stop helpmi_mariadb && docker rm helpmi_mariadb
docker compose up -d mariadb

# Cas corruption InnoDB — repair mode
# 1. Ajouter dans docker-compose.yml sous environment de mariadb :
#    MARIADB_EXTRA_FLAGS: --innodb-force-recovery=1
docker compose stop mariadb
docker compose up -d mariadb
docker logs helpmi_mariadb -f  # attendre "ready for connections"

# Dump immédiat avant tout
docker exec helpmi_mariadb sh -c \
  'mysqldump -u root -p$MYSQL_ROOT_PASSWORD helpmi' > backup_recovery.sql

# Retirer le flag et redémarrer normalement
```

### Disque plein

```bash
df -h
docker system df
du -sh /var/lib/docker/volumes/*/

# Nettoyer (SANS supprimer les volumes)
docker system prune -f

# Tronquer les logs de conteneurs
truncate -s 0 $(docker inspect --format='{{.LogPath}}' helpmi_backend)
truncate -s 0 $(docker inspect --format='{{.LogPath}}' helpmi_mariadb)
```

Prévention : ajouter dans `docker-compose.yml` sur chaque service :
```yaml
logging:
  options:
    max-size: "50m"
    max-file: "3"
```

---

## Incidents P2 — Majeurs

Fonctionnalité clé dégradée, utilisateurs impactés.

### Backend refuse de démarrer — erreur Flyway

```bash
docker logs helpmi_backend 2>&1 | grep -A 5 "FlywayException\|Migration"
```

Messages courants :
- `Validate failed: Migration checksum mismatch` → fichier SQL modifié après application
- `Migration V14__ failed` → erreur SQL dans une nouvelle migration
- `Table 'flyway_schema_history' doesn't exist` → base vierge, premier démarrage (normal)

**Cas checksum mismatch :**
```sql
-- Se connecter
docker exec -it helpmi_mariadb mariadb -u helpmi -p helpmi

-- État des migrations
SELECT version, description, success, checksum
FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;

-- Supprimer l'entrée ayant échoué (success=0 uniquement)
DELETE FROM flyway_schema_history WHERE success = 0;
```

```bash
docker compose restart backend && docker logs helpmi_backend -f
```

> ⚠️ **Ne jamais modifier** un fichier `V*.sql` déjà appliqué en production. Toute correction passe par un nouveau fichier `V(n+1)__fix_xxx.sql`.

### Authentification impossible — Keycloak KO

```bash
# Vérifier que Keycloak répond
curl -s http://auth.localhost/realms/helpmi/.well-known/openid-configuration \
  | python3 -m json.tool | head -10

# Redémarrer Keycloak
docker compose restart keycloak
docker logs helpmi_keycloak -f | grep -i "started\|error"

# Smoke test — récupérer un token
curl -s -X POST http://auth.localhost/realms/helpmi/protocol/openid-connect/token \
  -d "client_id=helpmi-frontend" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123" \
  | python3 -m json.tool | grep "access_token\|error"
```

Si le realm manque : `docker compose down keycloak && docker compose up -d keycloak` (réimporte depuis `keycloak/realm-export.json`).

### Upload de fichiers impossible — MinIO KO

```bash
# Santé
curl -sf http://localhost:9000/minio/health/live && echo "OK" || echo "KO"

docker compose restart minio
docker logs helpmi_minio --tail 50
```

> Le bucket `helpmi` est **créé automatiquement** par le backend au démarrage via le SDK S3.
> Si le bucket est manquant, vérifier les logs backend — une erreur de connexion MinIO l'empêche de le créer.

### Frontend inaccessible (page blanche / 502)

```bash
docker logs helpmi_frontend --tail 50
curl -I http://localhost:3000

# Si 502 sur /api/* — vérifier le backend
docker ps | grep helpmi_backend
curl -s http://localhost:8080/actuator/health

docker compose restart frontend
```

---

## Incidents P3 — Modérés

Fonctionnalité partiellement dégradée, contournement possible.

### Backend lent / timeouts sur l'API

```bash
docker stats --no-stream helpmi_backend helpmi_mariadb

# Requêtes SQL actives
docker exec -it helpmi_mariadb mariadb -u root -p helpmi -e "SHOW PROCESSLIST;"

# Connexions actives
docker exec helpmi_mariadb mariadb -u root -p helpmi -e \
  "SELECT count(*) FROM information_schema.PROCESSLIST;"

docker compose restart backend
```

### Erreurs CORS

Symptôme : *"Access-Control-Allow-Origin"* dans la console navigateur.

```bash
# Dans .env, APP_CORS_ALLOWED_ORIGINS doit correspondre à l'URL du navigateur
docker compose up -d --force-recreate backend
```

### Token JWT invalide / 401 en boucle

```bash
# Issuer retourné par Keycloak
curl -s http://auth.localhost/realms/helpmi/.well-known/openid-configuration \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['issuer'])"

# Issuer attendu par le backend
docker inspect helpmi_backend | grep KEYCLOAK_ISSUER
```

Les deux valeurs doivent être identiques. Si ce n'est pas le cas : mettre à jour `APP_KEYCLOAK_ISSUER_URI` dans `.env`, puis `docker compose up -d --force-recreate backend`.

---

## Incidents P4 — Mineurs

Impact limité, résolution planifiable.

### Un utilisateur ne peut pas se connecter

1. Keycloak admin (`http://auth.localhost/admin`) → Realm helpmi → Users → vérifier : Enabled, Email Verified, pas de lock.
2. Reset mot de passe : Users → l'utilisateur → Credentials → Reset password.
3. Vérifier le rôle en base :
```sql
SELECT id, email, role, active FROM users WHERE email = 'user@example.com';
```

### Un utilisateur voit la page "en attente d'affectation"

Ce n'est pas un bug : tout utilisateur sans organisation est bloqué par design. Un admin doit lui affecter une organisation depuis **Admin → Utilisateurs**.

Affectation directe en base (urgence) :
```sql
-- Trouver l'organisation cible
SELECT id, name FROM organizations WHERE active = 1;

-- Affecter l'utilisateur (table de jointure many-to-many)
INSERT INTO user_organizations (user_id, organization_id)
VALUES ('UUID-USER', 'UUID-ORG');
```

---

## Opérations routinières

```bash
# État général
docker ps -a --format "table {{.Names}}\t{{.Status}}"
docker stats
curl -s http://localhost:8080/actuator/health

# Logs en direct
docker logs -f helpmi_backend
docker logs -f helpmi_mariadb
docker logs helpmi_backend 2>&1 | grep -i error | tail -50

# Base de données
docker exec -it helpmi_mariadb mariadb -u helpmi -p helpmi
docker exec helpmi_mariadb mariadb -u helpmi -p helpmi -e \
  "SELECT status, COUNT(*) FROM tickets GROUP BY status;"
docker exec helpmi_mariadb mariadb -u helpmi -p helpmi -e \
  "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

# MinIO
docker exec helpmi_minio du -sh /data/helpmi/ 2>/dev/null || \
  docker exec helpmi_minio du -sh /data/
curl -sf http://localhost:9000/minio/health/live && echo OK
```

---

## Sauvegarde & restauration

### Dump MariaDB

```bash
docker exec helpmi_mariadb sh -c \
  'mysqldump -u root -p$MYSQL_ROOT_PASSWORD --single-transaction helpmi' \
  > backup_helpmi_$(date +%Y%m%d_%H%M%S).sql

head -20 backup_helpmi_*.sql  # vérification
```

### Restauration MariaDB

> ⚠️ **Destructif** — écrase la base existante. Faire un dump avant.

```bash
docker exec -i helpmi_mariadb sh -c \
  'mariadb -u root -p$MYSQL_ROOT_PASSWORD helpmi' \
  < backup_helpmi_20260101_120000.sql
```

### Sauvegarde MinIO

```bash
docker run --rm \
  -v minio_data:/source:ro \
  -v $(pwd)/backup_minio:/backup \
  alpine tar czf /backup/minio_$(date +%Y%m%d).tar.gz -C /source .
```

### Restauration MinIO

```bash
docker run --rm \
  -v minio_data:/target \
  -v $(pwd)/backup_minio:/backup \
  alpine tar xzf /backup/minio_20260101.tar.gz -C /target
```

### Reset environnement de développement

> ⚠️ **Développement uniquement.** Supprime toutes les données.

```bash
docker compose down -v
docker compose up -d
docker ps --format "table {{.Names}}\t{{.Status}}"

# Mode dev avec phpMyAdmin
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

---

## Redéploiement

```bash
# 1. Sauvegarde obligatoire
docker exec helpmi_mariadb sh -c \
  'mysqldump -u root -p$MYSQL_ROOT_PASSWORD --single-transaction helpmi' \
  > backup_before_deploy_$(date +%Y%m%d_%H%M%S).sql

# 2. Mettre à jour les sources
git pull origin main

# 3. Rebuild et redémarrage
docker compose build --no-cache backend frontend
docker compose up -d --force-recreate backend frontend

# 4. Vérifier les migrations Flyway
docker logs helpmi_backend 2>&1 | grep -E "Flyway|migration|Migrating"
# Chercher "Successfully applied X migrations"

# 5. Smoke test
curl -s http://localhost:8080/actuator/health
curl -I http://localhost:3000
```

---

## Journal d'audit

La table `audit_log` enregistre les actions sensibles. Consultable depuis **Administration → Journal** (interface filtrée, 50 entrées/page) ou en SQL.

### Actions auditées

| Action | Déclencheur |
|---|---|
| `PAT_CREATED` / `PAT_REVOKED` | Gestion des tokens d'accès personnels |
| `PAT_AUTH_SUCCESS` / `PAT_AUTH_FAILURE` | Authentification via PAT |
| `USER_ROLE_CHANGED` | Changement de rôle par un admin |
| `USER_ACTIVATED` / `USER_DEACTIVATED` | Activation/désactivation d'un compte |
| `USER_ORG_ADDED` / `USER_ORG_REMOVED` | Modification des organisations d'un utilisateur |
| `USER_PROJECTS_UPDATED` | Modification des projets/rôles d'un utilisateur |
| `PROJECT_CREATED` | Création d'un projet |
| `PROJECT_ARCHIVED` / `PROJECT_UNARCHIVED` | Archivage ou désarchivage d'un projet |
| `ORGANIZATION_CREATED` / `ORGANIZATION_DELETED` | Création/suppression d'une organisation |
| `TICKET_DELETED` | Suppression d'un ticket (admin seulement) |
| `ATTACHMENT_DELETED` | Suppression d'une pièce jointe |
| `ACCESS_DENIED` | Tentative d'accès refusée par Spring Security |

### Requêtes SQL

```sql
-- Dernières 100 entrées
SELECT created_at, action, actor_email, target_type, target_id, details, ip_address
FROM audit_log ORDER BY created_at DESC LIMIT 100;

-- Actions sensibles des dernières 24h
SELECT * FROM audit_log
WHERE action IN ('PAT_AUTH_FAILURE', 'ACCESS_DENIED', 'USER_DEACTIVATED')
  AND created_at > NOW() - INTERVAL 1 DAY
ORDER BY created_at DESC;

-- Activité d'un utilisateur
SELECT * FROM audit_log WHERE actor_email = 'user@example.com'
ORDER BY created_at DESC LIMIT 50;

-- Purge des entrées de plus de 6 mois
DELETE FROM audit_log WHERE created_at < NOW() - INTERVAL 6 MONTH;
```

---

## Archivage des projets

Un projet archivé est **invisible pour les non-admins**, exclu des dashboards, et ne peut plus recevoir de tickets. L'archivage est **réversible**.

| Comportement | Actif | Archivé |
|---|---|---|
| Visible (non-admin) | ✅ | ❌ |
| Visible (admin) | ✅ | ✅ (badge) |
| Tickets dans les dashboards | ✅ | ❌ |
| Création de tickets | ✅ | ❌ |
| Archivage / désarchivage | Admin | Admin |

### Via l'interface

Administration → Projets → bouton sur la ligne → *Archiver* ou *Désarchiver*.

### En SQL (urgence)

```sql
SELECT key, name, archived, active FROM projects ORDER BY name;

UPDATE projects SET archived = TRUE WHERE key = 'PROJ';
UPDATE projects SET archived = FALSE WHERE key = 'PROJ';
```

> L'archivage (`archived`) est distinct de la désactivation (`active = false`). Un projet désactivé est supprimé logiquement et non récupérable via l'interface.

---

## Traefik

### Un service n'est pas joignable via son sous-domaine

```bash
# Dashboard : http://traefik.localhost ou http://localhost:8090/dashboard/
# Vérifier HTTP → Routers → statut Enabled

# Labels du conteneur
docker inspect helpmi_frontend | grep -A 20 '"Labels"'

# Réseau
docker inspect helpmi_frontend | grep -A 5 '"Networks"'
# helpmi_proxy doit apparaître

# Logs Traefik
docker logs helpmi_traefik --tail 100 2>&1 | grep -i "error\|warn"

# Forcer la redécouverte
docker compose restart traefik
```

### Keycloak — "invalid issuer" après changement de domaine

```bash
# Issuer encodé dans les tokens
curl -s http://auth.localhost/realms/helpmi \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['issuer'])"

# Issuer attendu par le backend
docker inspect helpmi_backend | grep KEYCLOAK_ISSUER
```

Mettre à jour `TRAEFIK_DOMAIN` dans `.env`, puis :
```bash
docker compose up -d --force-recreate keycloak backend
```

> Le frontend doit aussi être rebuild car `VITE_KEYCLOAK_URL` est baked dans l'image.

### Migration HTTPS (production)

Prérequis : nom de domaine pointé sur le serveur, port 443 ouvert.

```bash
# 1. .env
TRAEFIK_DOMAIN=helpmi.example.com
ACME_EMAIL=ops@helpmi.example.com
APP_CORS_ALLOWED_ORIGINS=https://app.helpmi.example.com

# 2. Fichier ACME (requis par Traefik)
mkdir -p traefik/letsencrypt
touch traefik/letsencrypt/acme.json
chmod 600 traefik/letsencrypt/acme.json

# 3. Dans docker-compose.yml, décommenter les blocs # HTTPS :
#    - entrypoints websecure et redirection HTTP→HTTPS
#    - port 443:443
#    - volume ./traefik/letsencrypt:/letsencrypt
#    - labels tls.certresolver=letsencrypt sur chaque router

# 4. Rebuild frontend (VITE_KEYCLOAK_URL baked dans l'image)
docker compose build --no-cache frontend
docker compose up -d

# 5. Vérifier le certificat
docker logs helpmi_traefik -f | grep -i "acme\|certificate\|error"
curl -sv https://app.helpmi.example.com 2>&1 | grep -i "SSL\|certificate\|issuer"
```

> Let's Encrypt : limite de 5 certificats/domaine/semaine. En test, utiliser le staging ACME.

---

## Variables d'environnement

| Variable | Service(s) | Description | Valeur dev |
|---|---|---|---|
| `TRAEFIK_DOMAIN` | Traefik, Frontend (build) | Domaine de base | localhost |
| `DB_USER` | MariaDB, Backend | Utilisateur applicatif | helpmi |
| `DB_PASSWORD` | MariaDB, Backend | Mot de passe applicatif | *secret* |
| `DB_ROOT_PASSWORD` | MariaDB | Mot de passe root | *secret* |
| `KEYCLOAK_ADMIN` | Keycloak | Login admin | admin |
| `KEYCLOAK_ADMIN_PASSWORD` | Keycloak | Mot de passe admin | *secret* |
| `APP_KEYCLOAK_ISSUER_URI` | Backend | URL publique du realm (validation JWT) | http://auth.localhost/realms/helpmi |
| `APP_CORS_ALLOWED_ORIGINS` | Backend | Origine autorisée CORS | http://localhost:3000 |
| `MINIO_ROOT_USER` | MinIO, Backend | Access key MinIO | helpmi |
| `MINIO_ROOT_PASSWORD` | MinIO, Backend | Secret key MinIO | *secret* |
| `VITE_KEYCLOAK_URL` | Frontend (build) | URL Keycloak vue par le navigateur | http://auth.localhost |
| `VITE_KEYCLOAK_REALM` | Frontend (build) | Nom du realm | helpmi |
| `VITE_KEYCLOAK_CLIENT_ID` | Frontend (build) | Client ID Keycloak | helpmi-frontend |
| **SMTP (notifications email)** | | | |
| `APP_MAIL_HOST` | Backend | Hôte SMTP | localhost |
| `APP_MAIL_PORT` | Backend | Port SMTP | 1025 |
| `APP_MAIL_USERNAME` | Backend | Identifiant SMTP | — |
| `APP_MAIL_PASSWORD` | Backend | Mot de passe SMTP | — |
| `APP_MAIL_SMTP_AUTH` | Backend | Activer auth SMTP | false |
| `APP_MAIL_STARTTLS` | Backend | Activer STARTTLS | false |
| `APP_MAIL_FROM` | Backend | Adresse expéditeur | noreply@helpmi.local |
| `APP_BASE_URL` | Backend | URL publique frontend (liens emails) | http://localhost:5173 |
| **S3 externe (si MinIO remplacé)** | | | |
| `APP_S3_ENDPOINT` | Backend | URL endpoint S3 | http://minio:9000 |
| `APP_S3_REGION` | Backend | Région S3 | us-east-1 |
| `APP_S3_BUCKET` | Backend | Nom du bucket | helpmi |
| `APP_S3_ACCESS_KEY` | Backend | Access key S3 | = MINIO_ROOT_USER |
| `APP_S3_SECRET_KEY` | Backend | Secret key S3 | = MINIO_ROOT_PASSWORD |

> Les variables `VITE_*` sont baked dans l'image frontend au build. Toute modification nécessite un rebuild.

---

## Quick reference

| Situation | Commande |
|---|---|
| État de tous les conteneurs | `docker ps -a --format "table {{.Names}}\t{{.Status}}"` |
| Logs d'un service | `docker logs -f helpmi_<service>` |
| Redémarrer un service | `docker compose restart <service>` |
| Redémarrer tout | `docker compose up -d` |
| Health backend | `curl -s http://localhost:8080/actuator/health` |
| Health MinIO | `curl -sf http://localhost:9000/minio/health/live` |
| Token Keycloak (smoke test) | `curl -s -X POST http://auth.localhost/realms/helpmi/protocol/openid-connect/token -d "client_id=helpmi-frontend&grant_type=password&username=admin&password=admin123"` |
| Console MariaDB interactive | `docker exec -it helpmi_mariadb mariadb -u helpmi -p helpmi` |
| Migrations appliquées | `docker exec helpmi_mariadb mariadb -u helpmi -p helpmi -e "SELECT version,success FROM flyway_schema_history;"` |
| Dump base | `docker exec helpmi_mariadb sh -c 'mysqldump -u root -p$MYSQL_ROOT_PASSWORD helpmi' > backup.sql` |
| Consommation ressources | `docker stats --no-stream` |
| Espace disque Docker | `docker system df` |
| Nettoyage (sans volumes) | `docker system prune -f` |
| Reset complet dev | `docker compose down -v && docker compose up -d` |
| Rebuild + redéploiement | `docker compose build --no-cache backend frontend && docker compose up -d --force-recreate backend frontend` |
| Dashboard Traefik | http://traefik.localhost ou http://localhost:8090/dashboard/ |
| Routers Traefik (API) | `curl -s http://localhost:8090/api/http/routers \| python3 -m json.tool \| grep '"name"'` |
| Issuer Keycloak | `curl -s http://auth.localhost/realms/helpmi \| python3 -c "import sys,json; print(json.load(sys.stdin)['issuer'])"` |
| Journal d'audit (SQL) | `SELECT created_at, action, actor_email FROM audit_log ORDER BY created_at DESC LIMIT 50;` |
| Archiver un projet (SQL) | `UPDATE projects SET archived = TRUE WHERE key = 'PROJ';` |
