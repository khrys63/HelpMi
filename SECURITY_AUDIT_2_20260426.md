# Audit de sécurité & couverture de tests — HelpMi (itération 2)

Date : 2026-04-26

---

## 1. Couverture de tests

### Évolution depuis l'itération 1

| Périmètre | Avant (itération 1) | Après (itération 2) | Δ |
|---|---|---|---|
| Instructions | 60 % | **73 %** | +13 pts |
| Lignes | 59 % | **71 %** | +12 pts |
| Branches | 51 % | **64 %** | +13 pts |

### Par classe — couche service et sécurité

| Classe | Lignes | Branches |
|---|---|---|
| `LabelService` | **100 %** | 100 % |
| `TicketLinkService` | **100 %** | 86 % |
| `ClientService` | **100 %** | 100 % |
| `CommentService` | **100 %** | 100 % |
| `UserService` | **100 %** | — |
| `PersonalTokenFilter` | **100 %** ← nouveau | **100 %** |
| `PersonalTokenService` | **95 %** ← était 0 % | 100 % |
| `AttachmentService` | **95 %** ← était 0 % | 83 % |
| `AdminConfigService` | 96 % | 82 % |
| `ProjectService` | 97 % | 75 % |
| `TicketService` | 94 % | 77 % |
| `GlobalExceptionHandler` | **89 %** ← était 0 % | — |
| `RateLimiterService` | **0 %** ← nouveau, non testé | 0 % |
| `StartupSafetyCheck` | **0 %** ← nouveau, non testé | 0 % |
| `CurrentUserService` | **0 %** | 0 % |

### Périmètres restants à 0 %

| Périmètre | Raison |
|---|---|
| Tous les `Controller` | Pas de `@SpringBootTest` — architecture unit-only |
| `SecurityConfig` | Configuration Spring — difficile en unit test |
| `StartupSafetyCheck` | Nouveau composant de sécurité |
| `RateLimiterService` | Nouveau service |
| `CurrentUserService` | Dépend de `SecurityContextHolder` et JWT |
| `DevAuthFilter` | Filter dev, actif seulement en mode `disabled=true` |
| `StorageConfig` | `@PostConstruct` nécessite contexte Spring |
| `HelpMiApplication` | Point d'entrée Spring Boot |

### Recommandations couverture

1. **Priorité haute** : tester `RateLimiterService` (limite par utilisateur, fenêtre glissante) et `StartupSafetyCheck` (profil prod + sécurité désactivée)
2. **Priorité haute** : tester `CurrentUserService` (création utilisateur depuis JWT, fallback dev)
3. **Priorité moyenne** : atteindre 80 % de branches sur `TicketService` (lignes 77 → 90 %)
4. **Priorité basse** : ajouter des tests d'intégration `@SpringBootTest` sur les endpoints critiques (auth, tickets, tokens)

---

## 2. Statut des corrections — itération 1

| Risque | Statut |
|---|---|
| H1 — CORS trop permissif | ✅ Corrigé : `APP_CORS_ALLOWED_ORIGINS` injecté, obligatoire en prod |
| H2 — `security.disabled` en prod | ✅ Corrigé : `StartupSafetyCheck` bloque le démarrage |
| M3 — Pas de rate limiting | ✅ Corrigé (partiel) : 10 créations de PAT/heure/utilisateur |
| M1 — Validation type de fichier | ⏳ Toujours ouvert |
| M2 — Mot de passe Keycloak trivial | ⏳ Toujours ouvert |
| M4 — Secrets committés en clair | ⏳ Toujours ouvert |
| F1 — `@Valid` absent sur `PersonalTokenController` | ⏳ Toujours ouvert |
| F3 — Pas d'alerte expiration PAT | ⏳ Toujours ouvert |

---

## 3. Nouveaux risques identifiés

### 🔴 Critique

Aucun.

---

### 🟠 Haute

#### H1 — Autorisation manquante sur 5 méthodes de `TicketService`

**Fichier** : `TicketService.java`

Les méthodes `moveTicket`, `setDueDate`, `setClients`, `setLabels` et `cloneTicket` ne font aucun contrôle de rôle ni de `requireCanModify`. N'importe quel utilisateur authentifié (y compris rôle `CLIENT`) peut :

- Déplacer un ticket vers un autre projet
- Modifier la date d'échéance d'un ticket quelconque
- Réaffecter les clients associés à n'importe quel ticket
- Modifier les labels de n'importe quel ticket
- Cloner n'importe quel ticket

Contrastant avec `updateTicket`, `changeStatus` (qui appellent `requireCanModify`) et `deleteTicket` (qui exige `ADMIN`).

**Correctif** : ajouter `requireCanModify(currentUserService.getCurrentUser(), ticket)` en début de chacune de ces méthodes, ou définir une règle homogène (ex. : AGENT ou ADMIN pour `move`, reporter/assigné pour le reste).

```java
public TicketResponse moveTicket(UUID projectId, UUID ticketId, UUID targetProjectId) {
    Ticket ticket = findTicket(projectId, ticketId);
    requireCanModify(currentUserService.getCurrentUser(), ticket); // ← manquant
    ...
}
```

---

#### H2 — `AdminLabelController` : endpoint `find-or-create` sans vérification de rôle

**Fichier** : `AdminLabelController.java`

```java
@PostMapping("/find-or-create")
public LabelResponse findOrCreate(@RequestBody Map<String, String> body) {
    return labelService.findOrCreate(body.getOrDefault("name", "")); // pas de requireAdmin()
}
```

L'endpoint est sur `/api/admin/labels/find-or-create` mais ne vérifie pas le rôle. N'importe quel utilisateur authentifié (CLIENT) peut créer des labels via cet endpoint, alors que les endpoints `POST /`, `PUT /{id}`, `DELETE /{id}` de la même ressource exigent tous le rôle ADMIN.

**Correctif** :
```java
@PostMapping("/find-or-create")
public LabelResponse findOrCreate(@RequestBody Map<String, String> body) {
    requireAdmin(); // ← à ajouter
    return labelService.findOrCreate(body.getOrDefault("name", ""));
}
```

---

### 🟡 Modérée

#### M1 — Injection dans l'en-tête `Content-Disposition` via le nom de fichier

**Fichier** : `AttachmentController.java`

```java
.header(HttpHeaders.CONTENT_DISPOSITION,
    "attachment; filename=\"" + entity.getFileName() + "\"")
```

Le nom de fichier provient de la saisie utilisateur au moment de l'upload et est stocké en base tel quel. Si le nom contient un `"` ou des séquences `\r\n`, cela produit un en-tête malformé. Tomcat 10.x rejette les `\r\n` dans les en-têtes (HTTP Response Splitting mitigé), mais le `"` casse la syntaxe `filename=` et peut amener le navigateur à interpréter un nom de fichier incorrect.

**Correctif** : assainir ou encoder le nom de fichier dans l'en-tête.

```java
String safeFilename = entity.getFileName()
    .replaceAll("[\"\\\\]", "_")   // retire les guillemets et antislashs
    .replaceAll("[\\r\\n]", "");   // retire les retours chariot
.header(HttpHeaders.CONTENT_DISPOSITION,
    "attachment; filename=\"" + safeFilename + "\"")
```

Pour les noms non-ASCII, utiliser le format RFC 5987 : `filename*=UTF-8''<encoded>`.

---

#### M2 — phpMyAdmin déployé en production

**Fichier** : `docker-compose.yml`

Le service `phpmyadmin` est déclaré dans le compose principal utilisé en prod (`SPRING_PROFILES_ACTIVE: prod`). Il expose une interface d'administration complète de la base MariaDB sur le port 8081, accessible à quiconque atteint ce port.

**Correctif** : déplacer `phpmyadmin` dans un fichier `docker-compose.dev.yml` chargé uniquement en développement.

```bash
# Dev seulement
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
```

---

#### M3 — Actuator : absence de whitelist explicite des endpoints exposés

**Fichier** : `application.yml`

`spring-boot-starter-actuator` est présent mais aucune configuration n'explicite quels endpoints sont exposés via HTTP. Le comportement par défaut de Spring Boot 3 n'expose que `/actuator/health` via le web, mais ce comportement implicite peut changer lors d'une mise à jour mineure. Un endpoint comme `/actuator/env` accessible à un utilisateur authentifié révèle toutes les variables d'environnement (incluant les valeurs masquées côté Spring mais potentiellement lisibles si l'auto-masquage est désactivé).

**Correctif** :

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
```

---

### 🔵 Faible

#### F1 — `CurrentUserService.createUserFromJwt` : email potentiellement null

**Fichier** : `CurrentUserService.java`

```java
.email(jwt.getClaimAsString("email"))
```

Le claim `email` n'est pas garanti dans tous les flux OAuth2. S'il est absent ou null dans le JWT, un utilisateur sera créé avec `email = null`, ce qui viole la contrainte `NOT NULL` en base et lèvera une exception non contrôlée à la connexion.

**Correctif** :
```java
String email = jwt.getClaimAsString("email");
if (email == null || email.isBlank()) {
    throw new IllegalStateException("JWT sans claim 'email' : connexion refusée");
}
```

---

#### F2 — `@Valid` absent sur plusieurs endpoints de `TicketController`

Les endpoints suivants acceptent un corps de requête sans annotation `@Valid` :

| Endpoint | Corps |
|---|---|
| `PATCH /{ticketId}/due-date` | `DueDateRequest` |
| `POST /{ticketId}/move` | `MoveTicketRequest` |
| `PUT /{ticketId}/clients` | `Map<String, List<UUID>>` |
| `PUT /{ticketId}/labels` | `Map<String, List<UUID>>` |

Si des contraintes sont ajoutées aux DTOs correspondants, elles ne seront pas appliquées automatiquement.

---

#### F3 — `parseFilter` n'invalide pas les valeurs par rapport aux enums connus

**Fichier** : `TicketService.java`

Les paramètres `status`, `priority` et `type` sont passés librement à la requête JPQL (paramétrisée — pas d'injection SQL). Une valeur inconnue (ex. : `status=FOO`) n'est jamais rejetée : la requête s'exécute avec un `IN ('FOO')` qui ne retourne rien, sans erreur pour l'appelant. Ce comportement silencieux peut masquer des bugs côté client.

**Correctif** : valider les valeurs contre les codes de la table `config_values`, ou documenter explicitement ce comportement.

---

#### F4 — `RateLimiterService` et `StartupSafetyCheck` non couverts par les tests

Deux composants de sécurité ajoutés à l'itération 1 n'ont aucun test unitaire :
- `RateLimiterService` : logique de fenêtre glissante, gestion de la concurrence
- `StartupSafetyCheck` : blocage du démarrage avec profil prod

---

## 4. Points positifs (nouveaux depuis itération 1)

| Pratique | Statut |
|---|---|
| CORS restreint à l'origine configurée | ✅ Corrigé (H1) |
| Démarrage bloqué si `security.disabled=true` + profil `prod` | ✅ Corrigé (H2) |
| Rate limiting sur création de PAT (10/heure/utilisateur) | ✅ Corrigé (M3) |
| `PersonalTokenFilter` à 100 % de couverture | ✅ |
| `PersonalTokenService` à 95 % de couverture | ✅ |
| `AttachmentService` à 95 % de couverture | ✅ |
| `GlobalExceptionHandler` testé — pas de stack trace dans les réponses | ✅ |
| JaCoCo intégré dans `pom.xml` (`mvn verify`) | ✅ |

---

## 5. Synthèse des actions recommandées

| Priorité | Action | Fichier(s) |
|---|---|---|
| 🟠 Court terme | Ajouter `requireCanModify` sur `moveTicket`, `setDueDate`, `setClients`, `setLabels`, `cloneTicket` (H1) | `TicketService.java` |
| 🟠 Court terme | Ajouter `requireAdmin()` sur `/find-or-create` (H2) | `AdminLabelController.java` |
| 🟡 Moyen terme | Assainir le nom de fichier dans `Content-Disposition` (M1) | `AttachmentController.java` |
| 🟡 Moyen terme | Déplacer phpMyAdmin dans un compose dev uniquement (M2) | `docker-compose.yml` |
| 🟡 Moyen terme | Expliciter la whitelist actuator (M3) | `application.yml` |
| 🟡 Moyen terme | Valider l'email avant création utilisateur depuis JWT (F1) | `CurrentUserService.java` |
| 🟡 Moyen terme | Tester `RateLimiterService` et `StartupSafetyCheck` (F4) | tests |
| 🔵 Long terme | Valider type de fichier uploadé (M1 itération 1, toujours ouvert) | `AttachmentService.java` |
| 🔵 Long terme | Externaliser les secrets dans `.env` non-committé (M4 itération 1) | `docker-compose.yml` |
| 🔵 Long terme | Ajouter des tests d'intégration `@SpringBootTest` | — |
| 🔵 Long terme | Alerte expiration imminente des PAT (F3 itération 1) | frontend |
