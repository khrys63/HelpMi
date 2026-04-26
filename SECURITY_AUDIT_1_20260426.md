# Audit de sécurité & couverture de tests — HelpMi

Date : 2026-04-26

---

## 1. Couverture de tests

### Méthodologie
Tests unitaires Mockito uniquement (pas d'integration tests ni de tests de bout en bout).
Rapport généré via JaCoCo.

### Résultats globaux

| Périmètre | Couverture |
|---|---|
| Instructions | 60 % (2174 / 3613) |
| Lignes | 59 % (387 / 658) |
| Branches | 51 % (92 / 181) |

### Par service (couche critique)

| Service | Couverture lignes |
|---|---|
| `LabelService` | **100 %** |
| `ClientService` | **100 %** |
| `CommentService` | **100 %** |
| `UserService` | **100 %** |
| `AdminConfigService` | 97 % |
| `TicketLinkService` | 98 % |
| `ProjectService` | 95 % |
| `TicketService` | 85 % |
| `PersonalTokenService` | **0 %** ← nouveau, non testé |
| `AttachmentService` | **0 %** ← non testé |

### Périmètres à 0 % (non couverts)

| Périmètre | Raison / Risque |
|---|---|
| Tous les `Controller` | Architecture unit-only, pas d'integration tests |
| `PersonalTokenFilter` | Nouveau composant de sécurité, priorité haute |
| `PersonalTokenService` | Nouvelle fonctionnalité |
| `AttachmentService` | Gestion fichiers, logique de permissions |
| `GlobalExceptionHandler` | Peut exposer des infos sensibles si mal configuré |
| `SecurityConfig` | Difficile à tester unitairement |

### Recommandations couverture

1. **Priorité haute** : tester `PersonalTokenService` (création, validation, révocation, expiration) et `PersonalTokenFilter`
2. **Priorité haute** : tester `AttachmentService` (permissions upload/suppression)
3. **Priorité moyenne** : ajouter des tests d'intégration `@SpringBootTest` sur les endpoints critiques (auth, tickets, tokens)
4. Atteindre 80 % de couverture globale sur les services

---

## 2. Audit de sécurité

### 2.1 Dépendances frontend — vulnérabilités connues

| Sévérité | Paquet | CVE / Advisory | Description |
|---|---|---|---|
| **Modérée** | `esbuild ≤ 0.24.2` via `vite ≤ 6.4.1` | GHSA-67mh-4wv8-2f99 | Le serveur de dev Vite accepte des requêtes cross-origin sans restriction, permettant à n'importe quel site web d'y accéder. **Impact : dev uniquement, pas le build de production.** |

**Correctif disponible** : `vite 8.x` (breaking change — migration majeure requise).
**Mitigation immédiate** : ne pas exposer le serveur de dev (`localhost:5173`) sur un réseau partagé.

### 2.2 Dépendances frontend — mises à jour majeures disponibles

Ces versions ont des breaking changes et ne peuvent pas être mises à jour sans travail de migration :

| Paquet | Version actuelle | Dernière version |
|---|---|---|
| `vite` | 5.4.21 | 8.0.10 |
| `keycloak-js` | 24.0.5 | 26.2.4 |
| `@vitejs/plugin-vue` | 5.2.4 | 6.0.6 |
| `pinia` | 2.3.1 | 3.0.4 |
| `tailwindcss` | 3.4.19 | 4.2.4 |
| `vue-router` | 4.6.4 | 5.0.6 |

### 2.3 Dépendances backend

Aucune vulnérabilité identifiée sur les dépendances Maven actuelles. Versions utilisées :

| Composant | Version | Statut |
|---|---|---|
| Spring Boot | 3.3.5 | Stable, 3.4.x disponible |
| Spring Security | 6.3.4 | OK |
| Hibernate | 6.5.3 | OK |
| Flyway | 10.10.0 | OK |
| MariaDB connector | 3.3.3 | 3.5.x disponible |
| Tomcat | 10.1.31 | OK |

---

## 3. Vulnérabilités et risques identifiés dans le code

### 🔴 Critique

Aucune.

---

### 🟠 Haute

#### H1 — CORS trop permissif en production ✅ Corrigé

**Fichier** : `SecurityConfig.java`

`allowedOriginPatterns` est maintenant injecté depuis `${app.cors.allowed-origins}` (défaut : `http://localhost:5173` en dev). En prod, la variable d'environnement `APP_CORS_ALLOWED_ORIGINS` est **obligatoire** (pas de valeur par défaut dans `application-prod.yml`).

---

#### H2 — `app.security.disabled` : risque de déploiement accidentel en mode non sécurisé ✅ Corrigé

**Fichier** : `config/StartupSafetyCheck.java` (nouveau)

Le backend refuse désormais de démarrer si `app.security.disabled=true` et que le profil actif contient `prod`. Un `IllegalStateException` est lancé au `@PostConstruct`, avant toute requête HTTP.

---

### 🟡 Modérée

#### M1 — Pas de validation du type de fichier uploadé

**Fichier** : `AttachmentService.java`

Aucune restriction sur le `Content-Type` ou l'extension des fichiers. Un utilisateur peut uploader un `.sh`, `.php`, `.exe`, etc. Le fichier est stocké sur disque et téléchargeable.

**Risque** : Si le serveur web sert les fichiers uploadés avec exécution (PHP, CGI), code execution possible. Non exploitable sur la config actuelle (fichiers servis en download pur).

**Correctif recommandé** :
```java
private static final Set<String> ALLOWED_TYPES = Set.of(
    "image/jpeg", "image/png", "image/gif", "application/pdf",
    "text/plain", "application/zip",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
);

if (!ALLOWED_TYPES.contains(file.getContentType())) {
    throw new IllegalArgumentException("Type de fichier non autorisé");
}
```

---

#### M2 — Mot de passe Keycloak admin trivial committé

**Fichier** : `docker-compose.yml`
```yaml
KEYCLOAK_ADMIN_PASSWORD: admin
```
**Risque** : Si l'interface admin Keycloak (`localhost:8180/admin`) est accessible depuis l'extérieur, le compte admin est compromis immédiatement.

**Correctif** : Passer par une variable d'environnement ou un secret, changer la valeur par défaut.

---

#### M3 — Aucun rate limiting sur les endpoints ✅ Corrigé (création de tokens)

**Fichiers** : `service/RateLimiterService.java` (nouveau), `service/PersonalTokenService.java`

Un rate limiter sliding-window en mémoire limite la création de PATs à **10 par heure par utilisateur**. Au-delà, l'API répond `429 Too Many Requests`. Implémenté sans dépendance externe (single-node).

---

#### M4 — Identifiants de développement committés en clair

**Fichiers** : `application-dev.yml`, `docker-compose.yml`

Les mots de passe `helpmi_pass`, `root_pass` et `admin` sont dans le dépôt. Documenté dans le README, mais reste un risque si le dépôt devient public.

**Correctif** : Utiliser un fichier `.env` non-committé pour les valeurs sensibles, injecté via `docker-compose --env-file`.

---

### 🔵 Faible

#### F1 — `@Valid` absent sur `PersonalTokenController`

`CreatePersonalTokenRequest` n'a pas de contraintes de validation, et `@Valid` n'est pas présent. Si des contraintes sont ajoutées plus tard, elles ne seront pas appliquées automatiquement.

**Correctif** : Ajouter `@Valid` sur le `@RequestBody` par précaution.

#### F2 — `GlobalExceptionHandler` non testé

Si le handler laisse passer des stack traces dans les réponses d'erreur, des informations internes (noms de classes, chemins) sont exposées aux clients.

**Correctif** : Vérifier que le handler ne renvoie jamais de stack trace en production, ajouter des tests.

#### F3 — Token PAT : pas d'alerte sur expiration imminente

L'UI affiche la date d'expiration mais n'alerte pas l'utilisateur quand un token arrive à expiration, ce qui peut causer des coupures d'intégrations silencieuses.

---

## 4. Points positifs

| Pratique | Statut |
|---|---|
| Requêtes SQL via JPA paramétré | ✅ Pas d'injection SQL possible |
| Tokens PAT hashés SHA-256 en base | ✅ Token en clair jamais stocké |
| Token PAT affiché une seule fois en UI | ✅ |
| CSRF désactivé (API stateless JWT) | ✅ Correct pour cette architecture |
| Validation `@Valid` sur tous les controllers | ✅ (sauf PersonalTokenController) |
| Permissions vérifiées au niveau service | ✅ |
| Upload : nom de stockage basé sur UUID | ✅ Pas de path traversal possible |
| Expiration configurable sur les PAT | ✅ |
| Séparation dev / prod par profil Spring | ✅ |

---

## 5. Synthèse des actions recommandées

| Priorité | Action |
|---|---|
| ~~🔴 Immédiat~~ | ~~Tester `PersonalTokenService` et `PersonalTokenFilter`~~ ✅ |
| ~~🟠 Court terme~~ | ~~Restreindre CORS à l'origine du frontend en production (H1)~~ ✅ |
| 🟠 Court terme | Valider et restreindre les types de fichiers uploadés (M1) |
| 🟡 Moyen terme | Externaliser les secrets dans un `.env` non-committé (M4) |
| ~~🟡 Moyen terme~~ | ~~Ajouter du rate limiting sur les endpoints critiques (M3)~~ ✅ |
| 🟡 Moyen terme | Migrer `vite` vers 6.x (correctif CVE esbuild sans breaking change) |
| 🔵 Long terme | Ajouter des tests d'intégration `@SpringBootTest` |
| 🔵 Long terme | Alerte d'expiration imminente des tokens PAT (F3) |
