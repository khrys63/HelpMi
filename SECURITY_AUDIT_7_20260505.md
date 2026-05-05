# Rapport d'audit de sécurité — JiraLike
*Date : 2026-05-05 — Modèle : Claude Sonnet 4.6*

---

## ✅ Ce qui fonctionne bien

| Domaine | Détail |
|---|---|
| Authentification | OAuth2/JWT via Keycloak + PATs fonctionnels (corrigé A6) |
| SQL Injection | 100% JPQL paramétré avec `@Param` — aucune concaténation |
| Validation inputs | Bean Validation (`@NotBlank`, `@Size`, `@Pattern`, `@Future`) + `@Valid` aux controllers |
| Upload fichiers | Détection MIME via Apache Tika, allowlist stricte, noms UUID côté S3, `Content-Disposition: attachment` |
| Frozen tickets | `requireEditable()` appliqué dans TicketService, CommentService, AttachmentService, TicketLinkService |
| Rate limiting | 10 créations de PAT/heure par utilisateur, sliding window thread-safe |
| Désérialisation | Pas de `ObjectInputStream` — Jackson configuré sans `defaultTyping` |
| Gestion erreurs | `GlobalExceptionHandler` — pas de stack trace dans les réponses HTTP |
| Secrets prod | Variables d'environnement, `.env` dans `.gitignore`, pas de secrets en dur en production |

---

## 🟠 Élevé

### H1 — Broken Access Control sur `moveTicket` (déplacement vers projet non autorisé)

**Fichier** : `TicketService.java:moveTicket()`

`moveTicket()` vérifiait l'accès de l'utilisateur au projet **source** (`findTicket(projectId, ticketId)` → `requireProjectAccess`) mais chargeait le projet **cible** avec `projectService.findActive(targetProjectId)` sans vérifier que l'utilisateur y a accès.

Un utilisateur pouvait ainsi déplacer un ticket vers n'importe quel projet actif du système, y compris des projets auxquels il n'appartient pas — exposant potentiellement le ticket à d'autres utilisateurs.

**Code vulnérable :**
```java
public TicketResponse moveTicket(UUID projectId, UUID ticketId, UUID targetProjectId) {
    Ticket ticket = findTicket(projectId, ticketId);   // ← accès source vérifié
    requireEditable(ticket);
    requireCanModify(currentUserService.getCurrentUser(), ticket);
    String oldProject = ticket.getProject().getKey();
    var targetProject = projectService.findActive(targetProjectId);  // ← pas de contrôle d'accès
    // ...
}
```

**Correction :**
```java
public TicketResponse moveTicket(UUID projectId, UUID ticketId, UUID targetProjectId) {
    Ticket ticket = findTicket(projectId, ticketId);
    requireEditable(ticket);
    requireCanModify(currentUserService.getCurrentUser(), ticket);
    projectService.requireProjectAccess(targetProjectId);  // ← ajouté
    String oldProject = ticket.getProject().getKey();
    var targetProject = projectService.findActive(targetProjectId);
    // ...
}
```

> **✅ Corrigé le 2026-05-05** — `TicketService.java` : `projectService.requireProjectAccess(targetProjectId)` ajouté avant le chargement du projet cible. Si l'utilisateur n'a pas accès au projet cible, une `ForbiddenException` est levée immédiatement.

---

### H2 — Broken Access Control sur `GET /api/tickets/{ticketId}/comments`

**Fichiers** : `CommentController.java:20-23`, `CommentService.java:getComments()`

L'endpoint `GET /api/tickets/{ticketId}/comments` ne prend pas de `projectId` en paramètre et `CommentService.getComments()` retournait directement les commentaires sans vérifier que l'utilisateur a accès au projet auquel appartient le ticket.

Contrairement aux endpoints de `TicketService` (qui imposent un `projectId` dans le path et appellent `requireProjectAccess`), un utilisateur connaissant un `ticketId` (par devinette, lien partagé, ou fuite partielle) pouvait récupérer tous les commentaires d'un ticket dans un projet auquel il n'a pas accès.

**Code vulnérable :**
```java
// CommentService.java
@Transactional(readOnly = true)
public List<CommentResponse> getComments(UUID ticketId) {
    return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)  // ← pas de contrôle
            .stream().map(this::toResponse).toList();
}
```

**Correction :**
```java
// CommentService.java
@Transactional(readOnly = true)
public List<CommentResponse> getComments(UUID ticketId) {
    Ticket ticket = findTicket(ticketId);
    projectService.requireProjectAccess(ticket.getProject().getId());  // ← ajouté
    return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
            .stream().map(this::toResponse).toList();
}
```

`ProjectService` injecté dans `CommentService` via `@RequiredArgsConstructor` (même package, pas de dépendance circulaire).

> **✅ Corrigé le 2026-05-05** — `CommentService.java` : `ProjectService` ajouté aux dépendances. `getComments()` résout désormais le ticket en base puis appelle `requireProjectAccess()` avant de retourner les commentaires. Test `getComments_returnsMappedList` mis à jour pour mocker `ticketRepository.findById` et le mock `ProjectService` ajouté à la classe de test.

---

## 🟡 Moyen (constaté, non corrigé dans cette session)

### M1 — Credentials de développement dans `application-dev.yml`

**Fichier** : `application-dev.yml:3-5, 19-20`

Des valeurs par défaut codées en dur sont présentes pour le développement local :

```yaml
datasource:
  password: helpmi_pass          # fallback local

app.storage.s3:
  access-key: ${APP_S3_ACCESS_KEY:helpmi}
  secret-key: ${APP_S3_SECRET_KEY:helpmi_minio_pass}   # fallback local
```

Ces credentials sont dans le dépôt git. Ils correspondent aux services Docker Compose de dev (MariaDB + MinIO locaux) et ne sont pas utilisés en production (`application-prod.yml` ne contient pas de fallbacks). Le risque est faible dans ce contexte, mais une fuite du fichier dev pourrait exposer les accès à un environnement de dev partagé.

**Recommandation** : supprimer les valeurs par défaut des clés S3 et documenter dans un `.env.example` les variables à définir localement.

### M2 — Énumération des utilisateurs (reporté depuis A6)

**Fichier** : `UserService.java:getActiveUsers()`

`GET /api/users` retourne tous les utilisateurs actifs du système, tous projets confondus, à n'importe quel utilisateur authentifié. Un utilisateur peut énumérer tous les emails du système.

**Recommandation** : filtrer par organisation pour les non-admins.

```java
public List<UserResponse> getActiveUsers() {
    User current = currentUserService.getCurrentUser();
    if (current.getRole() == UserRole.ADMIN) {
        return userRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc()
            .stream().map(UserResponse::from).toList();
    }
    if (current.getOrganization() == null) return List.of();
    return userRepository.findActiveByOrganizationId(current.getOrganization().getId())
        .stream().map(UserResponse::from).toList();
}
```

### M3 — Headers de sécurité HTTP absents (reporté depuis A6)

**Fichier** : `SecurityConfig.java`

Aucun header de sécurité configuré côté applicatif. Recommandation :

```java
http.headers(h -> h
    .frameOptions(f -> f.deny())
    .contentTypeOptions(Customizer.withDefaults())
    .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000))
);
```

---

## 🔵 Faible / Informatif

### L1 — Valeurs d'enum non validées dans les filtres

**Fichier** : `TicketService.java:parseFilter()`

Les paramètres `status`, `priority`, `type` en query string sont découpés par virgule mais les valeurs ne sont pas validées contre les enums connus. Une valeur inconnue (`status=INVALID`) ne lève pas d'erreur, elle retourne silencieusement une liste vide — UX confuse mais pas de risque sécurité.

**Recommandation** : valider les valeurs et retourner une `400 Bad Request` si une valeur est inconnue.

### L2 — CORS `allowedHeaders: *`

**Fichier** : `SecurityConfig.java`

`config.setAllowedHeaders(List.of("*"))` accepte tous les headers CORS. Restreindre à `Authorization`, `Content-Type`, `Accept` réduit marginalement la surface.

### L3 — Rate limiting non distribué

**Fichier** : `RateLimiterService.java`

`ConcurrentHashMap` in-memory — contournable sur un déploiement multi-nœuds. Acceptable pour l'usage actuel, à migrer vers Redis si scale horizontal.

---

## Périmètre de l'audit

| Catégorie | Fichiers examinés |
|---|---|
| Controllers | `AdminLabelController`, `CommentController`, `TicketController`, `AttachmentController`, `UserController` |
| Services | `TicketService`, `CommentService`, `AttachmentService`, `TicketLinkService`, `ProjectService`, `PersonalTokenService`, `RateLimiterService` |
| Sécurité | `SecurityConfig`, `CurrentUserService`, `PersonalTokenFilter` |
| Configuration | `application.yml`, `application-dev.yml`, `application-prod.yml` |
| Build | `pom.xml` |
| Tests | `CommentServiceTest`, `TicketServiceTest` |

---

## Synthèse / Priorités

| Priorité | Finding | Statut |
|---|---|---|
| ~~🟠 P1~~ | ~~H1 — moveTicket sans contrôle d'accès projet cible~~ | ✅ Corrigé le 2026-05-05 |
| ~~🟠 P1~~ | ~~H2 — GET /comments sans contrôle d'accès projet~~ | ✅ Corrigé le 2026-05-05 |
| 🟡 P2 | M1 — Credentials dev dans le dépôt | À traiter |
| 🟡 P2 | M2 — Énumération utilisateurs (reporté A6) | À traiter |
| 🟡 P3 | M3 — Headers sécurité HTTP (reporté A6) | À traiter |
| 🔵 P4 | L1 — Enum non validées dans les filtres | Informatif |
| 🔵 P4 | L2, L3 — CORS headers, rate limiter | Informatif |
