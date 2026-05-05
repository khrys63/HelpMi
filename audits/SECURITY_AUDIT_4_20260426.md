# Audit de sécurité & couverture de tests — HelpMi (itération 4)

Date : 2026-04-26

---

## 1. Couverture de tests

### Résultats globaux

| Périmètre | Itération 1 | Itération 2 | Itération 3 | Itération 4 |
|---|---|---|---|---|
| Instructions | 60 % | 73 % | 76 % | **77 %** |
| Lignes | 59 % | 71 % | 74 % | **77 %** |
| Branches | 51 % | 64 % | 68 % | **74 %** |

201 tests unitaires Mockito (+ 8 depuis l'itération 3).

### Par classe — services et composants de sécurité

| Classe | Lignes | Branches |
|---|---|---|
| `LabelService`, `ClientService`, `CommentService` | **100 %** | 100 % |
| `UserService`, `RateLimiterService` | **100 %** | 100 % |
| `PersonalTokenFilter`, `StartupSafetyCheck` | **100 %** | 100 % |
| `ProjectService` | **100 %** | 89 % |
| `TicketLinkService` | **100 %** | 86 % |
| `OrganizationService` | 97 % | 88 % |
| `TicketService` | 97 % | 80 % |
| `AdminConfigService`, `PersonalTokenService` | 95–96 % | 82–100 % |
| `AttachmentService` | 95 % | 83 % |
| `GlobalExceptionHandler` | 89 % | — |
| `AdminLabelController` | 36 % | 100 % |
| `CurrentUserService`, tous les autres controllers | **0 %** | 0 % |

### Périmètres toujours à 0 %

| Périmètre | Raison |
|---|---|
| Tous les controllers (sauf `AdminLabelController` partiel) | Pas de `@SpringBootTest` |
| `CurrentUserService` | Dépend du `SecurityContext` Spring + JWT réel |
| `DevAuthFilter`, `SecurityConfig` | Nécessite contexte Spring Security complet |
| `StorageConfig`, `HelpMiApplication` | Beans d'infrastructure |

---

## 2. Statut des corrections — itérations précédentes

| Risque | Itération | Statut |
|---|---|---|
| H1 — CORS trop permissif | 1 | ✅ Corrigé |
| H2 — `security.disabled` en prod | 1 | ✅ Corrigé |
| M3 — Rate limiting PAT | 1 | ✅ Corrigé |
| H1 — `requireCanModify` sur TicketService | 2 | ✅ Corrigé |
| H2 — `requireAdmin()` sur `findOrCreate` | 2 | ✅ Corrigé |
| M1 — Content-Disposition header injection | 2 | ✅ Corrigé |
| M2 — phpMyAdmin en production | 2 | ✅ Corrigé |
| M3 — Whitelist actuator | 2 | ✅ Corrigé |
| F1 — Email null dans JWT | 2 | ✅ Corrigé |
| F2 — `@Valid` sur TicketController | 2 | ✅ Corrigé |
| F3 — `parseFilter` sans limite | 2 | ✅ Corrigé |
| M3 — `GET /api/users` exposait l'annuaire complet | 3 | ✅ Corrigé (restreint ADMIN/AGENT) |
| H1 — `TicketLinkService.deleteLink` sans autorisation | 3 | ⏳ Toujours ouvert |
| M1 — Type MIME client restitué tel quel | 3 | ⏳ Toujours ouvert |
| M2 — Pageable non borné | 3 | ⏳ Toujours ouvert |
| M4 — Absence de security headers HTTP | 3 | ⏳ Toujours ouvert |
| F1 — `@NotBlank` + `@Valid` sur PersonalTokenController | 1 | ⏳ Toujours ouvert |
| F2 — Champs texte sans `@Size` | 3 | ⏳ Toujours ouvert |
| F3 — `q` sans borne supérieure dans `search` | 3 | ⏳ Toujours ouvert |
| F4 — Rate limiting limité à la création de PAT | 3 | ⏳ Toujours ouvert |
| M2 — Mot de passe Keycloak trivial | 1 | ⏳ Toujours ouvert |
| M4 — Secrets committés en clair | 1 | ⏳ Toujours ouvert |
| F3 — Alerte expiration imminente PAT | 1 | ⏳ Toujours ouvert |

---

## 3. Nouveaux risques identifiés

### 🔴 Critique

Aucun.

---

### 🟠 Haute

#### H1 — Isolation par organisation contournée sur les endpoints tickets

**Fichiers** : `TicketService.java`, `AttachmentController.java`, `CommentController.java`

L'isolation par organisation est correctement appliquée sur `ProjectService.getAllProjects()` et `ProjectService.getProject()`. Cependant, toutes les opérations sur les tickets utilisent `projectService.findActive(projectId)` au lieu de `projectService.getProject(projectId)`, court-circuitant le contrôle d'accès organisationnel :

```java
// TicketService.createTicket — pas de vérification org
var project = projectService.findActive(projectId);  // ← bypasse requireProjectAccess

// TicketService.getTickets — pas de vérification org
ticketRepository.findByProjectIdWithFilters(projectId, ...)  // ← accès direct
```

Un utilisateur CLIENT connaissant l'UUID d'un projet d'une autre organisation peut :
- Lire tous les tickets de ce projet (`GET /api/projects/{id}/tickets`)
- Lire le détail d'un ticket (`GET /api/projects/{id}/tickets/{ticketId}`)
- Créer un ticket dans ce projet (`POST /api/projects/{id}/tickets`)

**Correctif** : ajouter `projectService.requireProjectAccess(project)` dans `TicketService`, ou remplacer les appels `findActive` par `getProject` (qui inclut déjà la vérification).

```java
// Option : exposer requireProjectAccess comme méthode publique dans ProjectService
public void requireProjectAccess(UUID projectId) {
    requireProjectAccess(findActive(projectId));
}

// Puis dans TicketService
public Page<TicketResponse> getTickets(UUID projectId, ...) {
    projectService.requireProjectAccess(projectId);
    ...
}
```

---

### 🟡 Modérée

#### M1 — Utilisateur inactif peut toujours s'authentifier

**Fichier** : `CurrentUserService.java`

Le flag `active` sur `User` sert de soft-delete (l'utilisateur disparaît des listes) mais n'empêche pas l'accès à l'application. `getCurrentUser()` ne vérifie pas ce champ :

```java
return userRepository.findByKeycloakId(keycloakId)
        .orElseGet(() -> createUserFromJwt(jwt));
// ↑ si le user existe et est inactif, il est retourné tel quel — accès accordé
```

Un compte désactivé par un admin reste fonctionnel jusqu'à la prochaine rotation de token KC (expiry + refresh).

**Correctif** :
```java
User user = userRepository.findByKeycloakId(keycloakId)
        .orElseGet(() -> createUserFromJwt(jwt));
if (!user.isActive()) {
    throw new ForbiddenException("Compte désactivé");
}
return user;
```

---

### 🔵 Faible

#### F1 — `TicketLinkService.search` : `q` toujours sans borne supérieure

**Fichier** : `TicketLinkService.java`

La borne inférieure (`< 2`) a été ajoutée mais pas la borne supérieure :

```java
if (q == null || q.length() < 2) return List.of();
// ↑ une chaîne de 10 000 caractères passe toujours
```

**Correctif** :
```java
if (q == null || q.length() < 2 || q.length() > 100) return List.of();
```

---

#### F2 — Cumul des risques ouverts depuis les audits précédents

Les points suivants restent ouverts sans nouveau développement depuis l'audit 3 :

| Référence | Description |
|---|---|
| Audit 3 — H1 | `TicketLinkService.deleteLink` sans vérification d'autorisation |
| Audit 3 — M1 | `Content-Type` renvoyé depuis la valeur client (risque XSS / erreur 500) |
| Audit 3 — M2 | Pageable non borné (`max-page-size` non configuré) |
| Audit 3 — M4 | Absence de headers HTTP de sécurité (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`) |
| Audit 3 — F2 | Champs texte libres sans `@Size` applicatif |
| Audit 1 — F1 | `@NotBlank` + `@Valid` manquants sur `PersonalTokenController` |
| Audit 1 — M2 | Mot de passe Keycloak trivial dans le realm export |
| Audit 1 — M4 | Secrets (DB, Keycloak) committés en clair dans `docker-compose.yml` |

---

## 4. Points positifs (confirmés à l'itération 4)

| Pratique | Statut |
|---|---|
| Toutes les requêtes SQL via JPA paramétré | ✅ Pas d'injection SQL |
| PAT hashés SHA-256, jamais stockés en clair | ✅ |
| Autorisation vérifiée au niveau service | ✅ |
| CORS restreint à l'origine configurée | ✅ |
| Blocage démarrage `security.disabled` + profil `prod` | ✅ |
| Actuator HTTP restreint à `/health` uniquement | ✅ |
| phpMyAdmin absent du compose production | ✅ |
| Aucun `v-html` / `innerHTML` dans le frontend Vue | ✅ Pas de XSS frontend |
| Tokens Keycloak rafraîchis via PKCE S256 | ✅ |
| Tokens PAT jamais en `localStorage` / `sessionStorage` | ✅ En mémoire uniquement |
| `GET /api/users` restreint ADMIN/AGENT | ✅ Corrigé itération 4 |
| Organisation : filtrage projets au niveau service | ✅ |
| Gestion utilisateurs : auto-modification bloquée | ✅ |
| Affectation org bloquée pour les ADMINs | ✅ |

---

## 5. Synthèse des actions recommandées

| Priorité | Action | Fichier(s) |
|---|---|---|
| 🟠 Court terme | Appliquer `requireProjectAccess` dans `TicketService` (H1) | `TicketService.java`, `ProjectService.java` |
| 🟠 Court terme | Ajouter le check `!user.isActive()` dans `getCurrentUser` (M1) | `CurrentUserService.java` |
| 🟠 Court terme | Ajouter contrôle d'autorisation sur `deleteLink` (audit 3 H1) | `TicketLinkService.java` |
| 🟡 Moyen terme | Forcer `application/octet-stream` sur le download (audit 3 M1) | `AttachmentController.java` |
| 🟡 Moyen terme | Ajouter `spring.data.web.pageable.max-page-size: 100` (audit 3 M2) | `application.yml` |
| 🟡 Moyen terme | Ajouter security headers HTTP (audit 3 M4) | `SecurityConfig.java` |
| 🔵 Long terme | Borne supérieure sur `q` dans `TicketLinkService.search` (F1) | `TicketLinkService.java` |
| 🔵 Long terme | `@NotBlank` + `@Valid` sur `PersonalTokenController` (audit 1 F1) | DTO + Controller |
| 🔵 Long terme | `@Size(max=...)` sur les champs texte libres (audit 3 F2) | DTOs request |
| 🔵 Long terme | Externaliser les secrets dans `.env` non-committé (audit 1 M4) | `docker-compose.yml` |
| 🔵 Long terme | Alerte expiration imminente des PAT (audit 1 F3) | frontend |
| 🔵 Long terme | Tests `@WebMvcTest` / `@SpringBootTest` sur les endpoints critiques | tests |
