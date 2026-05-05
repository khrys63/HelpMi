# Rapport d'audit de sécurité — HelpMi
*Date : 2026-04-29 — Modèle : Claude Sonnet 4.6*

---

## ✅ Ce qui fonctionne bien

| Domaine | Détail |
|---|---|
| Authentification | OAuth2/JWT via Keycloak — architecture solide |
| PATs — génération | `SecureRandom` + 256 bits d'entropie, préfixe `hm_` |
| PATs — stockage | SHA-256 sans sel, acceptable pour les tokens haute entropie |
| PATs — lifecycle | Rate-limit (10/h), expiration, vérification propriétaire avant suppression |
| SQL Injection | 100% JPQL paramétré avec `@Param` — aucune concaténation |
| Validation | Bean Validation (`@NotBlank`, `@Size`, `@Future`) + `@Valid` aux controllers |
| Autorisation | Vérifications au service layer (`requireAdmin()`, `requireProjectAccess()`, `requireCanModify()`) |
| Upload | Noms stockés en UUID (pas de path traversal), `Content-Disposition: attachment` |
| Config prod | Secrets via variables d'env, `.env` dans `.gitignore`, health endpoint sans détails |
| Session | STATELESS, CSRF désactivé justifié pour une API REST sans cookie |

---

## 🔴 Critique

### C1 — Personal Access Tokens complètement non-fonctionnels

**Fichiers** : `CurrentUserService.java:27`, `PersonalTokenFilter.java:29`

`PersonalTokenFilter` authentifie via `UsernamePasswordAuthenticationToken(email, null, roles)`. Mais `CurrentUserService.getCurrentUser()` fait :

```java
if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
    throw new ForbiddenException("Authentification requise");  // ← toujours levée pour les PATs
}
```

**Résultat** : tout appel API avec un PAT retourne 403. Les utilisateurs peuvent créer des tokens mais ils ne fonctionnent sur aucun endpoint. Le filtre s'authentifie, puis chaque service rejette silencieusement.

**Correction** : `getCurrentUser()` doit aussi gérer `UsernamePasswordAuthenticationToken` en cherchant l'utilisateur par email (le principal).

```java
public User getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
        // ... logique JWT existante
    }
    if (auth instanceof UsernamePasswordAuthenticationToken) {
        String email = (String) auth.getPrincipal();
        return userRepository.findByEmail(email)
            .filter(User::isActive)
            .orElseThrow(() -> new ForbiddenException("Utilisateur introuvable"));
    }
    throw new ForbiddenException("Authentification requise");
}
```

> **✅ Corrigé le 2026-04-29** — `CurrentUserService.java` modifié : `getCurrentUser()` gère maintenant `JwtAuthenticationToken` (JWT) et `UsernamePasswordAuthenticationToken` (PAT) via deux branches `instanceof` distinctes. Import `UsernamePasswordAuthenticationToken` ajouté.

---

## 🟠 Élevé

### H1 — Broken Access Control sur les pièces jointes

**Fichier** : `AttachmentService.java:35-65`

`upload()` et `download()` ne vérifient pas l'accès de l'utilisateur au projet du ticket. Contrairement à `TicketService` qui appelle `projectService.requireProjectAccess(projectId)`, n'importe quel utilisateur authentifié peut :
- uploader une pièce jointe sur n'importe quel ticket (même dans un projet auquel il n'appartient pas)
- télécharger n'importe quelle pièce jointe en connaissant son UUID

**Correction** dans `AttachmentService` :

```java
public AttachmentResponse upload(UUID ticketId, MultipartFile file) {
    Ticket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new NotFoundException("Ticket introuvable"));
    projectService.requireProjectAccess(ticket.getProject().getId());  // ← ajouter
    // ...
}

public Resource download(UUID attachmentId) {
    Attachment attachment = attachmentRepository.findById(attachmentId)
        .orElseThrow(() -> new NotFoundException("Pièce jointe introuvable"));
    projectService.requireProjectAccess(attachment.getTicket().getProject().getId());  // ← ajouter
    // ...
}
```

> **✅ Corrigé le 2026-04-29** — `AttachmentService.java` modifié : `ProjectService` injecté via `@RequiredArgsConstructor`. `requireProjectAccess()` appelé dans `upload()` (ligne 39) après résolution du ticket, et dans `download()` (ligne 67) après résolution de la pièce jointe.

---

## 🟡 Moyen

### ~~M1 — Content-Type des fichiers non validé~~

**Fichier** : `AttachmentService.java:45`

```java
String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
```

Le MIME type était fourni par le client HTTP, pas détecté depuis le contenu réel. Un attaquant pouvait déclarer `text/html` pour un fichier malveillant.

> ✅ **Corrigé le 2026-04-29**
>
> - Ajout de `tika-core:2.9.2` dans `pom.xml` et d'un bean `Tika` dans `StorageConfig.java`
> - `AttachmentService.upload()` lit les bytes du fichier, puis appelle `tika.detect(bytes, filename)` — détection basée sur le contenu réel, indépendante du header `Content-Type` client
> - Validation contre une allowlist de 12 types autorisés (`ALLOWED_TYPES`) ; tout autre type lève `IllegalArgumentException`
> - Le type stocké en base et transmis au storage est celui détecté par Tika, pas celui déclaré par le client
> - Tests ajoutés : `upload_disallowedType_throwsIllegalArgument`, `upload_usesDetectedType_notClientProvided`

### M2 — Énumération des utilisateurs

**Fichier** : `UserService.java:37`

`getActiveUsers()` retourne **tous** les utilisateurs actifs du système à n'importe quel utilisateur authentifié, sans filtrage par organisation. Un utilisateur peut énumérer tous les comptes et leurs emails.

**Correction** : filtrer par organisation pour les non-admins.

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

### M3 — Headers de sécurité absents

**Fichier** : `SecurityConfig.java`

Aucun header de sécurité HTTP configuré. Ajouter a minima :

```java
http.headers(h -> h
    .frameOptions(f -> f.deny())
    .contentTypeOptions(Customizer.withDefaults())
    .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000))
);
```

---

## 🔵 Faible / Informatif

### L1 — Rate limiter in-memory non distribué

**Fichier** : `RateLimiterService.java`

Fonctionne uniquement en déploiement mono-nœud. En cas de scale horizontal, le rate limit de 10 tokens/heure serait contournable. Acceptable pour l'usage actuel, à migrer vers Redis si le déploiement évolue.

### L2 — CORS `allowedHeaders: *`

**Fichier** : `SecurityConfig.java:63`

`config.setAllowedHeaders(List.of("*"))` accepte tous les headers. Restreindre aux headers réellement nécessaires (`Authorization`, `Content-Type`, `Accept`) réduit la surface d'attaque.

### L3 — Property `app.security.disabled` orpheline

**Fichier** : `application.yml:41`

La propriété `app.security.disabled: false` est définie mais non référencée dans `SecurityConfig.java`. Si elle devait court-circuiter la sécurité (pattern courant en dev), elle est potentiellement dangereuse si réactivée. À supprimer ou documenter son usage.

---

## Synthèse / Priorités

| Priorité | Finding | Effort estimé |
|---|---|---|
| ~~🔴 P0~~ | ~~C1 — PATs non-fonctionnels~~ | ✅ Corrigé le 2026-04-29 |
| ~~🟠 P1~~ | ~~H1 — Access control pièces jointes~~ | ✅ Corrigé le 2026-04-29 |
| ~~🟡 P2~~ | ~~M1 — Validation MIME type uploads~~ | ✅ Corrigé le 2026-04-29 |
| 🟡 P2 | M2 — Énumération utilisateurs | Moyen |
| 🟡 P3 | M3 — Headers sécurité | Faible |
| 🔵 P4 | L1, L2, L3 | Informatif |
