# Rapport d'audit de sécurité — HelpMi

**Date :** 6 mai 2026
**Périmètre :** Backend Spring Boot 3.5 / Frontend Vue 3
**Auditeur :** Claude (audit statique automatisé)
**Statut :** Pré-production

---

## Résumé exécutif

L'application présente une base de sécurité solide : authentification déléguée à Keycloak (OAuth2/JWT), absence d'injection SQL, gestion des erreurs sans fuite de stack trace, upload de fichiers avec validation MIME stricte. Huit points correctifs ont été identifiés, dont cinq à traiter avant tout déploiement en production.

---

## Méthodologie

Analyse statique du code source :
- Fichiers Java (controllers, services, DTOs, config)
- Fichiers de configuration YAML
- `pom.xml`

Référentiel : OWASP Top 10 2021.

---

## Résultats

### CRITIQUE — À corriger avant la production

---

#### [SEC-01] Credentials hardcodés dans `application-dev.yml`

**Catégorie OWASP :** A02 — Cryptographic Failures / A05 — Security Misconfiguration
**Fichier :** `backend/src/main/resources/application-dev.yml`
**Statut : ⚪ RISQUE ACCEPTÉ**

**Description :**
Les credentials de la base de données MariaDB et du stockage objet MinIO sont écrits en clair dans le fichier de configuration. Si ce fichier est versionné (Git), toute personne ayant accès au dépôt dispose de ces mots de passe.

**Preuve :**
```yaml
datasource:
  password: helpmi_pass
s3:
  access-key: ${APP_S3_ACCESS_KEY:helpmi}
  secret-key: ${APP_S3_SECRET_KEY:helpmi_minio_pass}
```

**Décision :**
Ce fichier est exclusivement utilisé en environnement de développement local. Les credentials qu'il contient donnent accès à des services Docker locaux sans exposition réseau. Le fichier de production (`application-prod.yml`) utilise des variables d'environnement sans valeurs par défaut. Risque accepté — aucune action requise.

---

#### [SEC-02] En-têtes HTTP de sécurité absents

**Catégorie OWASP :** A05 — Security Misconfiguration
**Fichier :** `SecurityConfig.java`
**Statut : ✅ CORRIGÉ — 6 mai 2026**

**Description :**
Aucun en-tête de protection HTTP n'est configuré. En l'absence de ces en-têtes, le navigateur n'est pas instruit de se protéger contre le clickjacking, le MIME sniffing, ou les attaques XSS par injection de ressources.

**En-têtes manquants :**

| En-tête | Risque associé |
|---|---|
| `X-Content-Type-Options: nosniff` | MIME sniffing |
| `X-Frame-Options: DENY` | Clickjacking |
| `Strict-Transport-Security` | Downgrade HTTPS→HTTP |
| `Content-Security-Policy` | XSS via ressources externes |

**Correction appliquée dans `SecurityConfig.filterChain()` :**
```java
.headers(h -> h
    .contentTypeOptions(Customizer.withDefaults())
    .frameOptions(f -> f.deny())
    .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true)));
```
Note : `Content-Security-Policy` n'est pas configuré côté Spring (géré côté frontend Vue/Vite).

---

#### [SEC-03] Configuration CORS trop permissive

**Catégorie OWASP :** A05 — Security Misconfiguration
**Fichier :** `SecurityConfig.java`
**Statut : ✅ CORRIGÉ — 6 mai 2026**

**Description :**
La politique CORS autorise l'ensemble des en-têtes HTTP via un wildcard. Combiné à `allowCredentials(true)`, cela expose l'API à des requêtes cross-origin transmettant des en-têtes arbitraires.

**Preuve :**
```java
config.setAllowedHeaders(List.of("*"));
config.setAllowCredentials(true);
```

**Correction appliquée dans `SecurityConfig.corsConfigurationSource()` :**
```java
config.setAllowedHeaders(List.of("Content-Type", "Authorization", "Accept"));
```

---

#### [SEC-04] Validation absente sur `SetAssigneeRequest`

**Catégorie OWASP :** A03 — Injection / A04 — Insecure Design
**Fichier :** `TicketController.java`, ligne ~75
**Statut : ✅ CORRIGÉ — 6 mai 2026**

**Description :**
L'endpoint `PATCH /tickets/{id}/assignee` accepte le corps de la requête sans annotation `@Valid`. La contrainte Bean Validation déclarée sur le DTO n'est donc jamais évaluée, permettant d'envoyer un `assigneeId` arbitraire.

**Preuve :**
```java
// Manquant : @Valid
public ResponseEntity<?> setAssignee(@RequestBody SetAssigneeRequest req) { ... }
```

**Correction appliquée dans `TicketController.setAssignee()` :**
```java
public TicketResponse setAssignee(..., @Valid @RequestBody SetAssigneeRequest req) { ... }
```

---

#### [SEC-05] Données non typées acceptées sans validation (`Map<String, List<UUID>>`)

**Catégorie OWASP :** A03 — Injection / A04 — Insecure Design
**Fichiers :** `TicketController.java` (endpoints organizations, labels)

**Description :**
Deux endpoints reçoivent une `Map<String, List<UUID>>` brute au lieu d'un DTO structuré. Aucune validation Bean Validation ne peut s'appliquer à une `Map` générique : la taille, les clés et les valeurs ne sont pas contraintes.

**Remédiation :**
Remplacer par des records dédiés :
```java
public record SetOrganizationsRequest(@NotNull List<@NotNull UUID> organizationIds) {}
public record SetLabelsRequest(@NotNull List<@NotNull UUID> labelIds) {}
```

---

### IMPORTANT — Fortement recommandé

---

#### [SEC-06] PAT tokens hashés avec SHA-256 sans sel dédié par token

**Catégorie OWASP :** A02 — Cryptographic Failures
**Fichier :** `PersonalTokenService.java`

**Description :**
Les Personal Access Tokens sont hashés avec SHA-256 sans sel individuel stocké en base. La valeur aléatoire du token (32 octets via `SecureRandom`) remplit partiellement ce rôle, mais SHA-256 reste un algorithme de hachage rapide inadapté au stockage de secrets à long terme. Une attaque par tables pré-calculées ciblant des tokens faibles resterait viable.

**Remédiation :**
Remplacer SHA-256 par PBKDF2 avec sel aléatoire par token, ou déléguer à `PasswordEncoder` (BCrypt/Argon2) qui gère le sel automatiquement :
```java
passwordEncoder.encode(rawToken);
passwordEncoder.matches(rawToken, storedHash);
```

---

#### [SEC-07] Champs texte longs sans limite de taille

**Catégorie OWASP :** A04 — Insecure Design
**Fichiers :** `UpdateTicketRequest.java`, `UpdateProjectRequest.java`

**Description :**
Les champs `description` (ticket, projet) n'ont pas de contrainte `@Size`. Un client malveillant peut envoyer des payloads de plusieurs mégaoctets, saturant la mémoire JVM et la base de données.

**Remédiation :**
```java
@Size(max = 50_000)
private String description;
```

---

#### [SEC-08] Rate limiting en mémoire uniquement

**Catégorie OWASP :** A04 — Insecure Design
**Fichier :** `RateLimiterService.java`

**Description :**
Le service de limitation de débit stocke ses compteurs dans une `ConcurrentHashMap` en mémoire JVM. Cette implémentation est correcte pour un déploiement single-node, mais devient inopérante en cas de déploiement multi-instance (load balancer) : chaque nœud maintient ses propres compteurs, et un attaquant peut contourner la limite en distribuant ses requêtes entre les instances.

**Remédiation :**
Pour un déploiement en cluster, externaliser les compteurs dans Redis avec une structure clé/TTL :
```java
// Exemple Lettuce/Redis
redisTemplate.opsForValue().increment("rate:" + ip);
redisTemplate.expire("rate:" + ip, Duration.ofMinutes(1));
```
Pour un déploiement single-node actuel, l'implémentation existante est acceptable.

---

## Tableau de synthèse

| ID | Titre | Sévérité | Effort |
|---|---|---|---|
| SEC-01 | Credentials hardcodés `application-dev.yml` | Critique | — | ⚪ Risque accepté (dev only) |
| SEC-02 | En-têtes HTTP de sécurité absents | Critique | Faible | ✅ Corrigé 06/05/2026 |
| SEC-03 | CORS trop permissif (`allowedHeaders: *`) | Critique | Faible | ✅ Corrigé 06/05/2026 |
| SEC-04 | `@Valid` manquant sur `SetAssigneeRequest` | Critique | Faible | ✅ Corrigé 06/05/2026 |
| SEC-05 | `Map` brute sans DTO ni validation | Critique | Moyen |
| SEC-06 | PAT tokens : SHA-256 sans sel dédié | Important | Moyen |
| SEC-07 | `description` sans limite de taille | Important | Faible |
| SEC-08 | Rate limiting en mémoire (non distribué) | Important | Élevé |

---

## Points conformes — Pas d'action requise

| Domaine | Statut |
|---|---|
| Injection SQL (JPQL paramétré) | ✅ Conforme |
| Authentification OAuth2/JWT (Keycloak) | ✅ Conforme |
| Gestion des erreurs (pas de stack trace exposée) | ✅ Conforme |
| Upload fichiers (whitelist MIME via Tika, UUID, taille limitée) | ✅ Conforme |
| Logging (aucun secret loggé) | ✅ Conforme |
| Contrôle d'accès au niveau service | ✅ Conforme |
| Secrets production (variables d'environnement) | ✅ Conforme |
| Versions des dépendances (Spring Boot 3.5, Java 21) | ✅ Conforme |

---

*Rapport généré le 6 mai 2026 — Prochaine révision recommandée avant mise en production*
