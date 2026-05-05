# Audit de sécurité #5

**Date** : 2026-04-27  
**Périmètre** : Backend Spring Boot (controllers, services, config, storage)  
**Tests** : 217 tests unitaires — 0 échec  
**Couverture** : 74,8 % lignes · 75,3 % branches

---

## Synthèse

| Sévérité | Nombre | Statut |
|---|---|---|
| Haute | 4 | ✅ H1, H2, H3 corrigés — H4 en attente |
| Moyenne | 5 | En attente |
| Faible | 4 | En attente |
| Informatif | 3 | — |

Aucune vulnérabilité **critique** confirmée. Les vérifications d'autorisation admin se font dans la couche service (pattern `requireAdmin()` cohérent) — ce n'est pas un bypass, mais un manque de défense en profondeur.

---

## H1 — Fuite d'information cross-projet dans la recherche de tickets ✅ Corrigé

**Fichiers** : `TicketLinkController.java:34`, `TicketRepository.java:41`  
**Sévérité** : Haute

La route `GET /api/tickets/search?q=…` recherche dans **tous les tickets de tous les projets** sans filtrer par organisation ni par droits d'accès :

```java
// TicketRepository
@Query("SELECT t FROM Ticket t WHERE UPPER(t.reference) LIKE :q OR UPPER(t.title) LIKE :q ORDER BY t.reference")
List<Ticket> searchByQuery(@Param("q") String q, Pageable pageable);
```

Un utilisateur CLIENT d'une organisation A peut retrouver les références et titres de tickets appartenant à des projets d'autres organisations.

**Correction** : filtrer la requête JPQL par les projets accessibles à l'utilisateur courant (via `organization_projects`), ou réutiliser la logique de `requireProjectAccess`.

---

## H2 — Personal tokens : aucune validation de la requête de création ✅ Corrigé

**Fichiers** : `PersonalTokenController.java:27`, `CreatePersonalTokenRequest.java`  
**Sévérité** : Haute

Le DTO de création ne déclare aucune contrainte et le contrôleur n'a pas `@Valid` :

```java
// Contrôleur
public PersonalTokenCreated create(@RequestBody CreatePersonalTokenRequest req)  // pas de @Valid

// DTO
public record CreatePersonalTokenRequest(String name, LocalDateTime expiresAt) {}
// → name peut être null ou ""
// → expiresAt peut être dans le passé ou null (token infini)
// → expiresAt peut être en 2150 (pas de borne max)
```

Un utilisateur peut créer un token sans nom, un token déjà expiré, ou un token qui n'expire jamais.

**Correction** :
```java
public record CreatePersonalTokenRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull @Future LocalDateTime expiresAt
) {}
```
Ajouter `@Valid` dans le contrôleur. Vérifier en service que `expiresAt` est inférieure à `now + 1 an`.

---

## H3 — Pagination non bornée ✅ Corrigé

**Fichier** : `TicketController.java:42`, `application.yml`  
**Sévérité** : Haute

`@PageableDefault(size = 20)` fixe la valeur par défaut mais ne plafonne pas les requêtes explicites. Un client peut envoyer `?size=1000000` et forcer le chargement de millions de tickets.

```bash
GET /api/projects/{id}/tickets?page=0&size=999999
```

**Correction** : ajouter dans `application.yml` :
```yaml
spring:
  data:
    web:
      pageable:
        max-page-size: 100
```

---

## H4 — Absence de headers HTTP de sécurité

**Fichier** : `SecurityConfig.java`  
**Sévérité** : Haute

Spring Security 6 désactive certains headers par défaut pour les API stateless. L'application ne configure aucun header de sécurité :

| Header manquant | Risque |
|---|---|
| `X-Content-Type-Options: nosniff` | MIME sniffing (aggrave M1 ci-dessous) |
| `X-Frame-Options: DENY` | Clickjacking |
| `Content-Security-Policy` | XSS si l'API est consommée depuis un contexte web |
| `Strict-Transport-Security` | Downgrade HTTP en prod |

**Correction** : dans `SecurityConfig.filterChain` :
```java
http.headers(h -> h
    .contentTypeOptions(Customizer.withDefaults())
    .frameOptions(f -> f.deny())
    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
);
```
Le CSP est à adapter selon le frontend (contrainte plus difficile avec une SPA).

---

## M1 — Content-Type de téléchargement issu de la saisie utilisateur

**Fichier** : `AttachmentController.java:34`, `AttachmentService.java:42`  
**Sévérité** : Moyenne

Le `Content-Type` retourné au téléchargement est celui déclaré **par l'uploadeur** lors du dépôt du fichier :

```java
String contentType = entity.getContentType() != null
    ? entity.getContentType() : "application/octet-stream";
return ResponseEntity.ok()
    .contentType(MediaType.parseMediaType(contentType))  // ← valeur utilisateur
    .body(resource);
```

Un attaquant peut uploader un fichier HTML/JS en déclarant `text/html`, puis envoyer le lien à une victime : le navigateur rend le contenu au lieu de le télécharger.

**Correction** :
1. Ne pas stocker le `Content-Type` fourni par le client
2. Forcer `Content-Type: application/octet-stream` au téléchargement avec `Content-Disposition: attachment`
3. Ou dériver le MIME depuis l'extension stockée via une table de correspondance interne

---

## M2 — Aucune validation du type de fichier à l'upload

**Fichier** : `AttachmentService.java:38-61`  
**Sévérité** : Moyenne

N'importe quel type de fichier est accepté (`.exe`, `.sh`, `.jsp`, `.docm`…). La seule logique de nommage préserve l'extension originale transmise par le client :

```java
String originalName = file.getOriginalFilename();   // valeur cliente, non fiable
if (originalName.contains("."))
    extension = originalName.substring(originalName.lastIndexOf("."));
String storedName = UUID.randomUUID() + extension;   // extension préservée
```

Si le bucket MinIO est mal configuré (accès public) ou si un bug autorisait l'exécution, des fichiers malveillants pourraient être servis.

**Correction** : déclarer une liste blanche d'extensions et de MIME types autorisés ; stocker le fichier sans extension si celle-ci n'est pas dans la liste.

---

## M3 — `@Valid` manquant dans `AdminUserController`

**Fichier** : `AdminUserController.java:26,32`  
**Sévérité** : Moyenne

Les deux endpoints d'écriture n'ont pas `@Valid` :

```java
public UserResponse update(@PathVariable UUID id, @RequestBody UpdateUserRequest req)
public UserResponse assignOrganization(@PathVariable UUID id, @RequestBody AssignOrganizationRequest req)
```

Si des contraintes sont ajoutées aux DTOs, elles ne seront jamais vérifiées.

**Correction** : ajouter `@Valid` devant chaque `@RequestBody`.

---

## M4 — Champs texte sans contrainte de taille

**Fichiers** : `CreateTicketRequest.java`, `UpdateTicketRequest.java`, `CreatePersonalTokenRequest.java`  
**Sévérité** : Moyenne

| Champ | DTO | Contrainte actuelle |
|---|---|---|
| `description` | `CreateTicketRequest` | aucune |
| `description` | `UpdateTicketRequest` | aucune |
| `name` | `CreatePersonalTokenRequest` | aucune |

Des chaînes de plusieurs Mo peuvent être insérées en base.

**Correction** :
```java
@Size(max = 20000) String description   // CreateTicketRequest, UpdateTicketRequest
@Size(max = 255)   String name          // CreatePersonalTokenRequest
```

---

## M5 — Paramètre `q` de recherche sans borne de longueur

**Fichier** : `TicketLinkService.java:67`  
**Sévérité** : Moyenne

Le paramètre `?q=` est directement injecté (via `LIKE`) dans une requête JPQL sans vérification de longueur en amont. Une valeur de 100 000 caractères consomme de la mémoire et du CPU inutilement.

**Correction** : ajouter `@Size(max = 100)` sur le paramètre dans le contrôleur, ou tronquer la valeur dans le service.

---

## L1 — Credentials S3 de développement dans le code source

**Fichier** : `application-dev.yml:17-19`, `application.yml:32-33`  
**Sévérité** : Faible

Des identifiants MinIO par défaut apparaissent en clair dans les fichiers de configuration versionnés (`helpmi` / `helpmi_minio_pass`). S'ils sont réutilisés en production, une fuite du dépôt suffit à compromettre le stockage.

**Correction** : ne configurer aucune valeur par défaut pour les secrets, ou utiliser un fichier `application-dev.yml.local` non commité.

---

## L2 — Absence de journalisation des actions sensibles

**Fichier** : ensemble de l'application  
**Sévérité** : Faible

Aucun audit log n'enregistre : modifications de rôle utilisateur, affectation d'organisation, suppression de pièce jointe, échecs d'autorisation. En cas d'incident, il est impossible de savoir qui a fait quoi.

**Correction** : logger les actions admin (`logger.info("User {} updated role of user {}", actorId, targetId)`) et les `ForbiddenException`.

---

## L3 — `PersonalToken.lastUsedAt` potentiellement non persisté

**Fichier** : `PersonalTokenService.java:68-79`  
**Sévérité** : Faible

Dans `validateToken()`, `t.setLastUsedAt(LocalDateTime.now())` modifie l'entité mais ne fait pas appel à `save()` explicitement. La persistance dépend du dirty-checking Hibernate dans la transaction courante ; selon le contexte d'appel (filtre hors transaction), elle peut ne pas avoir lieu.

**Correction** : appeler `personalTokenRepository.save(t)` explicitement.

---

## L4 — Checks d'autorisation uniquement dans la couche service

**Fichier** : ensemble des controllers admin  
**Sévérité** : Faible

Les vérifications `requireAdmin()` sont correctement implémentées dans les services, mais absentes des contrôleurs (`AdminOrganizationController`, `AdminUserController`, etc.). En cas d'injection directe du service (tests, refactoring), il n'y a pas de deuxième ligne de défense.

**Correction** : ajouter `@PreAuthorize("hasRole('ADMIN')")` sur les méthodes des controllers, en activant `@EnableMethodSecurity` dans `SecurityConfig`.

---

## Informatif

### I1 — CSRF désactivé

`csrf.disable()` est justifié pour une API stateless JWT. Aucune action requise.

### I2 — Session stateless

`SessionCreationPolicy.STATELESS` est correct pour cette architecture. Aucune action requise.

### I3 — Mode dev irréversible en production

`StartupSafetyCheck` bloque explicitement le démarrage si `security.disabled=true` et profil `prod` sont combinés. Ce garde-fou est en place et suffisant.

---

## Priorités recommandées

| Priorité | Finding | Effort | Statut |
|---|---|---|---|
| 1 | **H1** — Filtrer la recherche par organisation | Moyen | ✅ Corrigé |
| 2 | **H2** — Validation DTOs PersonalToken + @Valid + limite 1 an | Faible | ✅ Corrigé |
| 3 | **H3** — Plafonner la pagination (`max-page-size: 100`) | Faible | ✅ Corrigé |
| 4 | **H4** — Headers HTTP de sécurité | Faible | En attente |
| 5 | **M1** — Content-Type forcé en `application/octet-stream` | Faible | En attente |
| 6 | **M2** — Liste blanche types de fichiers | Moyen | En attente |
| 7 | **M5** — Borne sur `q` de recherche | Faible | En attente |
| 8 | **L3** — Persistance explicite de `lastUsedAt` | Faible | En attente |

---

## Couverture de tests par service

**217 tests** — 0 échec

| Service / Composant | Lignes | Branches |
|---|---|---|
| `LabelService`, `ClientService`, `CommentService` | 100 % | 100 % |
| `RateLimiterService`, `PersonalTokenFilter`, `StartupSafetyCheck` | 100 % | 100 % |
| `TicketLinkService` | 100 % | 91 % |
| `OrganizationService` | 97 % | 88 % |
| `ProjectService`, `AdminConfigService` | 96 % | 82–89 % |
| `TicketService` | 96 % | 84 % |
| `PersonalTokenService` | 95 % | 100 % |
| `UserService`, `AttachmentService` | 94 % | 75–100 % |
| `GlobalExceptionHandler` | 89 % | n/a |
| **Global** | **74,8 %** | **75,3 %** |

*Controllers, SecurityConfig, StorageConfig, CurrentUserService, DevAuthFilter, S3StorageService : 0 % (aucun test d'intégration)*
