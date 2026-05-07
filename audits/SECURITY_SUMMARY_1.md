# Synthèse sécurité — HelpMi

**Dernière mise à jour :** 6 mai 2026
**Périmètre :** Audits A1 (26/04/2026) → A8 (06/05/2026)
**Findings ouverts :** 4
**Findings corrigés :** 32
**Risques acceptés :** 3

---

## Findings ouverts

| # | Finding | Source | Remarque |
|---|---|---|---|
| 1 | PAT tokens : SHA-256 sans sel dédié par token | A8 SEC-06 | |
| 2 | `Map<String, List<UUID>>` sans DTO ni validation (organizations, labels, watchers) | A8 SEC-05 | |
| 3 | Journalisation des actions sensibles absente | A5-L2 | **Corrigé (06/05/2026)** — table `audit_log`, `AuditService`, hooks PAT/User/Ticket, `ACCESS_DENIED`, UI admin `/admin/audit` |
| 4 | `PersonalToken.lastUsedAt` potentiellement non persisté | A5-L3 | |
| 5 | Pas d'alerte sur expiration imminente des PAT | A1-F3 | |

---

## Risques acceptés

| Finding | Décision | Raison |
|---|---|---|
| A1-M2 — Mot de passe Keycloak admin trivial committé | Accepté le 06/05/2026 | Uniquement dans le realm export de dev ; la configuration de prod utilise des secrets distincts |
| A1-M4 / A7-M1 / A8 SEC-01 — Credentials dev dans `application-dev.yml` | Accepté le 06/05/2026 | Services Docker locaux sans exposition réseau ; prod utilise des variables d'environnement |
| A3-F4 / A6-L1 / A8 SEC-08 — Rate limiting non distribué (in-memory) | Accepté le 06/05/2026 | L'application restera en déploiement single-node ; à réévaluer si architecture distribuée |

---

## Corrigés — référence complète

| Finding | Corrigé dans | Vérification code |
|---|---|---|
| A1-H1 — CORS trop permissif en production | A1 | ✅ |
| A1-H2 — `app.security.disabled` déploiement non sécurisé | A1 | ✅ |
| A1-M3 — Rate limiting sur création de PAT | A1 | ✅ |
| A2-H1 — Autorisation manquante sur méthodes `TicketService` | A2 | ✅ `findTicket` → `requireProjectAccess` |
| A2-H2 — `AdminLabelController.findOrCreate` sans vérification de rôle | A2 | ✅ `requireAdmin()` présent |
| A2-M1 — Injection dans `Content-Disposition` via nom de fichier | A2 | ✅ Sanitisation dans `AttachmentController` |
| A2-M2 — phpMyAdmin en production | A4 | ✅ Absent du compose prod |
| A2-M3 — Actuator sans whitelist explicite des endpoints exposés | A2 | ✅ Seul `/health` exposé |
| A2-F1 — Email potentiellement null dans `createUserFromJwt` | Sans objet | ✅ Méthode renommée `createOrMigrateUserFromJwt` ; guard null/blank présent ligne 47 |
| A2-F2 — `@Valid` absent sur plusieurs endpoints `TicketController` | A2 + A8 SEC-04 | ✅ |
| A2-F3 — Enums non validés dans `parseFilter` | **A8 (06/05/2026)** | ✅ Allowlists statiques `VALID_STATUSES/PRIORITIES/TYPES` ; valeur inconnue → 400 |
| A2-F4 — `RateLimiterService` non couvert par les tests | Sans objet | ✅ `RateLimiterServiceTest` existait avec 5 tests |
| A3-H1 — `TicketLinkService.deleteLink` sans contrôle d'autorisation | A6 | ✅ Contrôle admin/manager/créateur présent |
| A3-M2 — Pageable non borné | A5 | ✅ |
| A3-M3 — `GET /api/users` expose l'annuaire complet | **A8 (06/05/2026)** | ✅ Non-admin : filtré par projets partagés (`findActiveUsersInSameProjects`) |
| A3-M4 / A8 SEC-02 — Absence de security headers HTTP | A8 | ✅ `X-Content-Type-Options`, `X-Frame-Options`, `HSTS` |
| A3-F2 / A5-M4 / A8 SEC-07 — Champs texte sans limite de taille | **A8 (06/05/2026)** | ✅ `@Size(max = 10_000)` sur `description` (4 DTOs ticket/projet) |
| A3-F3 / A4-F1 / A5-M5 — Paramètre `q` sans borne supérieure | **A8 (06/05/2026)** | ✅ `q.length() > 100` → liste vide retournée |
| A4-H1 — Isolation par organisation contournée sur tickets | A5 | ✅ `findTicket` → `requireProjectAccess` |
| A4-M1 — Utilisateur inactif peut s'authentifier | A4 | ✅ `isActive` vérifié dans `CurrentUserService` (JWT + PAT) |
| A5-H1 — Fuite d'information cross-projet dans la recherche | A5 | ✅ |
| A5-H2 — PAT : aucune validation de la requête de création | A5 | ✅ |
| A5-H3 — Pagination non bornée | A5 | ✅ |
| A5-M1 — Content-Type de téléchargement issu de la saisie utilisateur | A6 | ✅ Type issu de Tika à l'upload, stocké en base, relu en base au download |
| A5-M2 — Validation du type MIME à l'upload | A6 | ✅ Whitelist Apache Tika |
| A5-M3 — `@Valid` manquant dans `AdminUserController` | **A8 (06/05/2026)** | ✅ Ajouté sur `PATCH /{id}` et `PUT /{id}/projects` |
| A6-H1 — Broken Access Control sur les pièces jointes | A6 | ✅ |
| A7-H1 — Broken Access Control sur `moveTicket` | A7 | ✅ |
| A7-H2 — Broken Access Control sur `GET /tickets/{id}/comments` | A7 | ✅ |
| A8 SEC-03 — CORS `allowedHeaders: *` | A8 | ✅ Restreint à `Content-Type, Authorization, Accept` |
| A8 SEC-04 — `@Valid` manquant sur `SetAssigneeRequest` | A8 | ✅ |

---

| A5-L2 — Journalisation des actions sensibles | **06/05/2026** | ✅ Table `audit_log` + `AuditService` + UI admin |

---

*4 findings ouverts restants, tous de sévérité faible. Prochaine révision recommandée avant mise en production.*
