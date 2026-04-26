# Audit de sécurité & couverture de tests — HelpMi (itération 3)

Date : 2026-04-26

---

## 1. Couverture de tests

### Résultats globaux

| Périmètre | Itération 1 | Itération 2 | Itération 3 |
|---|---|---|---|
| Instructions | 60 % | 73 % | **76 %** |
| Lignes | 59 % | 71 % | **74 %** |
| Branches | 51 % | 64 % | **68 %** |

### Par classe — services et composants de sécurité

| Classe | Lignes | Branches |
|---|---|---|
| `LabelService`, `ClientService`, `CommentService`, `UserService` | **100 %** | 100 % |
| `TicketLinkService`, `PersonalTokenFilter` | **100 %** | 86–100 % |
| `RateLimiterService`, `StartupSafetyCheck` | **100 %** | 100 % |
| `PersonalTokenService`, `AttachmentService` | **95 %** | 83–100 % |
| `AdminConfigService`, `ProjectService` | 96–97 % | 75–82 % |
| `TicketService` | **97 %** | 80 % |
| `GlobalExceptionHandler` | 89 % | — |
| `AdminLabelController` | 36 % | 100 % |
| `CurrentUserService` | **0 %** | 0 % |
| Tous les autres controllers | **0 %** | 0 % |

### Périmètres toujours à 0 %

| Périmètre | Raison |
|---|---|
| Tous les controllers (sauf `AdminLabelController` partiel) | Pas de `@SpringBootTest` |
| `CurrentUserService` | Dépend du `SecurityContext` Spring + JWT réel |
| `DevAuthFilter` | Actif uniquement en mode `disabled=true` |
| `SecurityConfig` + `JwtOnlyBearerTokenResolver` | Nécessite contexte Spring Security complet |
| `StorageConfig`, `HelpMiApplication` | Beans d'infrastructure, `@PostConstruct` |

### Recommandations couverture

1. **Priorité haute** : tester `CurrentUserService` — composant de sécurité critique (création utilisateur, extraction de rôle depuis JWT)
2. **Priorité moyenne** : ajouter des tests `@WebMvcTest` ou `@SpringBootTest` sur les endpoints critiques (auth, upload, tokens)
3. Atteindre 80 % de branches sur `TicketService` (actuellement 80 %, quelques branches dans `autoCloneRecurring` non couvertes)

---

## 2. Statut des corrections — itérations 1 et 2

| Risque | Itération | Statut |
|---|---|---|
| H1 — CORS trop permissif | 1 | ✅ Corrigé |
| H2 — `security.disabled` en prod | 1 | ✅ Corrigé |
| M3 — Rate limiting | 1 | ✅ Corrigé (PAT) |
| H1 — `requireCanModify` sur 5 méthodes TicketService | 2 | ✅ Corrigé |
| H2 — `requireAdmin()` sur `findOrCreate` | 2 | ✅ Corrigé |
| M1 — Content-Disposition header injection | 2 | ✅ Corrigé |
| M2 — phpMyAdmin en production | 2 | ✅ Corrigé |
| M3 — Whitelist actuator | 2 | ✅ Corrigé |
| F1 — Email null dans JWT | 2 | ✅ Corrigé |
| F2 — `@Valid` sur TicketController | 2 | ✅ Corrigé |
| F3 — `parseFilter` sans limite | 2 | ✅ Corrigé (max 20 valeurs) |
| F4 — Tests RateLimiterService / StartupSafetyCheck | 2 | ✅ Corrigé |
| M1 — Validation type de fichier uploadé | 1 | ⏳ Toujours ouvert |
| M2 — Mot de passe Keycloak trivial | 1 | ⏳ Toujours ouvert |
| M4 — Secrets committés en clair | 1 | ⏳ Toujours ouvert |
| F1 — `@Valid` sur `PersonalTokenController` | 1 | ⏳ Toujours ouvert |
| F3 — Alerte expiration imminente PAT | 1 | ⏳ Toujours ouvert |

---

## 3. Nouveaux risques identifiés

### 🔴 Critique

Aucun.

---

### 🟠 Haute

#### H1 — `TicketLinkService.deleteLink` : aucun contrôle d'autorisation

**Fichier** : `TicketLinkService.java`

```java
public void deleteLink(UUID linkId) {
    TicketLink link = linkRepository.findById(linkId)
            .orElseThrow(() -> new NotFoundException("Lien introuvable"));
    linkRepository.delete(link);   // ← pas de vérification du rôle ni du créateur
}
```

N'importe quel utilisateur authentifié (y compris rôle `CLIENT`) peut supprimer n'importe quel lien entre tickets en connaissant son UUID. Contrastant avec `createLink` qui enregistre le créateur (`createdBy`), mais ne l'exploite pas pour la suppression.

**Correctif** : vérifier que l'utilisateur courant est ADMIN, AGENT, ou le créateur du lien.

```java
public void deleteLink(UUID linkId) {
    TicketLink link = linkRepository.findById(linkId)
            .orElseThrow(() -> new NotFoundException("Lien introuvable"));
    User user = currentUserService.getCurrentUser();
    boolean isAdminOrAgent = user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.AGENT;
    boolean isCreator = link.getCreatedBy() != null && link.getCreatedBy().getId().equals(user.getId());
    if (!isAdminOrAgent && !isCreator)
        throw new ForbiddenException("Vous n'êtes pas autorisé à supprimer ce lien");
    linkRepository.delete(link);
}
```

---

### 🟡 Modérée

#### M1 — Type MIME client restitué tel quel dans le téléchargement

**Fichier** : `AttachmentController.java`

```java
String contentType = entity.getContentType() != null ? entity.getContentType() : "application/octet-stream";
return ResponseEntity.ok()
    .contentType(MediaType.parseMediaType(contentType))  // ← valeur fournie par le client
    .body(resource);
```

Le `Content-Type` enregistré à l'upload provient directement de l'en-tête HTTP du client, sans validation. Conséquences :

1. **XSS stocké (partiellement mitigé)** : un fichier uploadé avec `Content-Type: text/html` est servi avec ce type. Le `Content-Disposition: attachment` empêche le rendu inline dans les navigateurs modernes, mais des clients HTTP alternatifs (curl, fetch sans `Content-Disposition` check, applications mobiles) pourraient traiter le contenu comme HTML.
2. **Erreur 500 sur téléchargement** : un `Content-Type` malformé (`Content-Type: "invalid;;;"`) provoque une `InvalidMediaTypeException` au moment du `MediaType.parseMediaType()`, rendant le fichier non téléchargeable.

**Correctif** : ne jamais restituer le type MIME du client — le déduire de l'extension ou imposer `application/octet-stream`.

```java
// Option simple : toujours forcer le téléchargement
String contentType = "application/octet-stream";

// Option avancée : déduire depuis l'extension (java.nio.file.Files.probeContentType)
String probed = Files.probeContentType(path);
String contentType = (probed != null) ? probed : "application/octet-stream";
```

---

#### M2 — Taille de page (`Pageable`) non bornée explicitement

**Fichier** : `TicketController.java`, `application.yml`

L'endpoint `GET /api/projects/{id}/tickets` accepte un paramètre `?size=N` via Spring Data `Pageable`. Aucune configuration `spring.data.web.pageable.max-page-size` n'est définie. La valeur par défaut de Spring Boot est 2 000, ce qui permet à un client authentifié de déclencher une requête retournant 2 000 tickets avec tous leurs champs.

**Correctif** :

```yaml
# application.yml
spring:
  data:
    web:
      pageable:
        max-page-size: 100
        default-page-size: 20
```

---

#### M3 — `GET /api/users` expose l'annuaire complet à tout utilisateur authentifié

**Fichier** : `UserController.java`, `UserService.java`

```java
@GetMapping
public List<UserResponse> getUsers() {
    return userService.getActiveUsers();
}
```

`UserResponse` contient `id`, `email`, `firstName`, `lastName`, `role`, `active`. Un utilisateur de rôle `CLIENT` peut ainsi énumérer tous les comptes actifs, leurs emails et leurs rôles. Utilisé en production avec des dizaines d'employés, cela constitue une fuite d'informations personnelles.

**Correctif** : restreindre aux rôles ADMIN et AGENT, ou remplacer par un endpoint de recherche paginé.

```java
@GetMapping
public List<UserResponse> getUsers() {
    requireAdminOrAgent();
    return userService.getActiveUsers();
}
```

---

#### M4 — Absence de security headers HTTP

Le backend ne positionne aucun en-tête de sécurité HTTP :

| En-tête | Risque si absent |
|---|---|
| `X-Content-Type-Options: nosniff` | Le navigateur peut interpréter un fichier selon son contenu et non son `Content-Type` |
| `X-Frame-Options: DENY` | Clickjacking possible si l'API est accessible depuis un iframe |
| `Referrer-Policy: strict-origin-when-cross-origin` | L'URL complète (avec paramètres) peut fuiter dans les logs des serveurs tiers |

Ces en-têtes sont souvent gérés par le reverse proxy en production, mais il est plus robuste de les appliquer au niveau applicatif.

**Correctif** :

```java
// Dans SecurityConfig.filterChain()
http.headers(headers -> headers
    .contentTypeOptions(Customizer.withDefaults())
    .frameOptions(frame -> frame.deny())
    .referrerPolicy(referrer ->
        referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
);
```

---

### 🔵 Faible

#### F1 — `CreatePersonalTokenRequest` : pas de `@NotBlank` sur `name`, pas de `@Valid` sur le controller

**Fichiers** : `CreatePersonalTokenRequest.java`, `PersonalTokenController.java`

Un token peut être créé avec `name = null` ou `name = ""`. L'absence de `@Valid` sur le `@RequestBody` signifie que même si des contraintes étaient ajoutées, elles ne seraient pas appliquées. Noté depuis l'itération 1, toujours ouvert.

**Correctif** :
```java
// DTO
public record CreatePersonalTokenRequest(@NotBlank @Size(max = 100) String name, LocalDateTime expiresAt) {}

// Controller
public PersonalTokenCreated create(@Valid @RequestBody CreatePersonalTokenRequest req) { ... }
```

---

#### F2 — Champs texte libres sans limite de taille applicative

Les champs suivants acceptent des valeurs arbitrairement longues (seule la contrainte DB `TEXT` / `VARCHAR` limite) :

| Champ | DTO | Limite DB |
|---|---|---|
| `description` ticket | `CreateTicketRequest`, `UpdateTicketRequest` | `TEXT` (65 KB) |
| `body` commentaire | `CreateCommentRequest` | `TEXT` (65 KB) |
| `name` token PAT | `CreatePersonalTokenRequest` | `VARCHAR(255)` |
| `name` label | `LabelRequest` | `VARCHAR(100)` |

Un utilisateur peut soumettre 60 KB de `description` sur chaque ticket. Le stockage est limité par MariaDB, mais la charge réseau et la sérialisation JSON (sur `getTicket`) ne sont pas bornées côté applicatif.

**Correctif** : ajouter `@Size(max = ...)` cohérent avec les contraintes DB.

---

#### F3 — `TicketLinkService.search` : paramètre `q` sans limite de longueur

**Fichier** : `TicketLinkService.java`

```java
return ticketRepository.searchByQuery("%" + q.toUpperCase() + "%", Pageable.ofSize(10))
```

`q` peut être une chaîne arbitrairement longue envoyée depuis le frontend ou directement via l'API. Bien que la requête soit paramétrée (pas d'injection SQL), une longue chaîne inutile est transmise et traitée par MariaDB.

**Correctif** :
```java
if (q == null || q.length() < 2 || q.length() > 100) return List.of();
```

---

#### F4 — Rate limiting limité à la création de PAT

`RateLimiterService` ne couvre que `createToken`. Les endpoints suivants n'ont aucune limitation :

| Endpoint | Risque |
|---|---|
| `POST /api/tickets/{id}/attachments` | Upload de 10 MB répété en boucle — saturation disque |
| `POST /api/tickets/{projectId}/tickets` | Création massive de tickets |
| `GET /api/tickets/search?q=x` | Requête LIKE répétée |

Pour un outil interne, le risque est limité aux utilisateurs authentifiés, mais un compte compromis pourrait causer des dommages significatifs.

---

## 4. Points positifs (confirmés à l'itération 3)

| Pratique | Statut |
|---|---|
| Toutes les requêtes SQL via JPA paramétré | ✅ Pas d'injection SQL |
| PAT hashés SHA-256, jamais stockés en clair | ✅ |
| Autorisation vérifiée au niveau service (non seulement controller) | ✅ |
| CORS restreint à l'origine configurée | ✅ |
| Blocage démarrage `security.disabled` + profil `prod` | ✅ |
| Actuator HTTP restreint à `/health` uniquement | ✅ |
| phpMyAdmin absent du compose production | ✅ |
| Aucun `v-html` / `innerHTML` dans le frontend Vue | ✅ Pas de XSS frontend |
| Tokens Keycloak rafraîchis via PKCE S256 | ✅ |
| Tokens PAT jamais en `localStorage` / `sessionStorage` | ✅ En mémoire uniquement (Pinia) |

---

## 5. Synthèse des actions recommandées

| Priorité | Action | Fichier(s) |
|---|---|---|
| 🟠 Court terme | Ajouter contrôle d'autorisation sur `deleteLink` (H1) | `TicketLinkService.java` |
| 🟡 Moyen terme | Forcer `application/octet-stream` sur le download ou déduire du fichier (M1) | `AttachmentController.java` |
| 🟡 Moyen terme | Ajouter `spring.data.web.pageable.max-page-size: 100` (M2) | `application.yml` |
| 🟡 Moyen terme | Restreindre `GET /api/users` aux rôles ADMIN/AGENT (M3) | `UserController.java` |
| 🟡 Moyen terme | Ajouter security headers HTTP via `SecurityConfig` (M4) | `SecurityConfig.java` |
| 🔵 Long terme | `@NotBlank` + `@Valid` sur `PersonalTokenController` (F1) | DTO + Controller |
| 🔵 Long terme | `@Size(max=...)` sur les champs texte libres (F2) | DTOs request |
| 🔵 Long terme | Limiter longueur de `q` dans `TicketLinkService.search` (F3) | `TicketLinkService.java` |
| 🔵 Long terme | Valider type de fichier uploadé (M1 itération 1) | `AttachmentService.java` |
| 🔵 Long terme | Externaliser les secrets dans `.env` non-committé (M4 itération 1) | `docker-compose.yml` |
| 🔵 Long terme | Alerte expiration imminente des PAT (F3 itération 1) | frontend |
| 🔵 Long terme | Tests `@WebMvcTest` / `@SpringBootTest` sur les endpoints critiques | tests |
