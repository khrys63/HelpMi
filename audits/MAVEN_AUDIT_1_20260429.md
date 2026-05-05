# Rapport d'audit des dépendances Maven — JiraLike
*Date : 2026-04-29 — Spring Boot 3.3.5 / Java 21*

---

## 📦 Dépendances directes obsolètes

### Mises à jour majeures (planification requise)

| Dépendance | Actuel | Dernière stable | Type | Risque |
|---|---|---|---|---|
| `spring-boot-starter-*` (parent BOM) | **3.3.5** | 3.4.x / 3.5.x | Majeur 3→3 | Moyen |
| `org.flywaydb:flyway-core` + `flyway-mysql` | **10.21.0** | 12.5.0 | Majeur 10→12 | Élevé |

> Spring Boot affiche `4.1.0-RC1` mais c'est une RC — ne pas utiliser en production. Monter sur la dernière **3.4.x ou 3.5.x** stable est la voie recommandée. Spring Boot 3.3.x a atteint son EOL.

> Flyway : saut de deux versions majeures (10→12). Des changements de comportement et de configuration sont documentés dans les notes de migration. À tester en staging.

### Mises à jour mineures / patch (sûres, faciles à bundler)

| Dépendance | Actuel | Dernière | Impact |
|---|---|---|---|
| `software.amazon.awssdk:s3` | 2.26.12 | **2.43.0** | API compatible, correctifs inclus |
| `org.mariadb.jdbc:mariadb-java-client` | 3.3.3 | **3.5.8** | Correctifs de performance et TLS |
| `org.projectlombok:lombok` | 1.18.38 | **1.18.46** | Correctifs générés Java 21+ |

---

## 🔍 Dépendances utilisées mais non déclarées

`dependency:analyze` remonte des dépendances transitives utilisées directement dans le code. Elles fonctionnent via les starters Spring Boot, mais une future réorganisation de starters pourrait les rompre.

**À surveiller (AWS SDK)** — à déclarer explicitement si utilisées directement :
```xml
<!-- actuellement tirées via software.amazon.awssdk:s3 -->
software.amazon.awssdk:auth:2.26.12
software.amazon.awssdk:regions:2.26.12
software.amazon.awssdk:sdk-core:2.26.12
```

Les dépendances Spring, Mockito, AssertJ, JUnit tirées via `spring-boot-starter-test` sont normales — pas besoin de les déclarer explicitement dans un projet Spring Boot.

**Bonne nouvelle** : aucune dépendance déclarée inutilisée.

---

## 🛡️ Scan de vulnérabilités

Le plugin OWASP Dependency-Check n'est **pas configuré** dans le pom.xml. Il n'y a donc pas de rapport CVE automatisé. Recommandation :

```xml
<!-- À ajouter dans <build><plugins> -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>10.0.3</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
    </configuration>
</plugin>
```

Puis lancer : `mvn dependency-check:check`

---

## ✅ Ce qui est à jour / bien configuré

- Java 21 LTS ✅
- Spring Security 6.3.x (géré par BOM) ✅
- Flyway activé avec `ddl-auto: validate` (sécurité schéma) ✅
- JaCoCo configuré pour la couverture ✅
- Aucune dépendance déclarée inutilisée ✅

---

## 🎯 Actions prioritaires

| Priorité | Action | Effort | Notes |
|---|---|---|---|
| **P1** | Spring Boot 3.3.5 → 3.4.x/3.5.x stable | Moyen | Tire logback, Jackson, Hibernate à jour automatiquement |
| **P2** | Flyway 10 → 12 | Élevé | Lire migration guides v11 et v12 — tester en staging |
| **P3** | AWS SDK 2.26.12 → 2.43.0 + MariaDB + Lombok | Faible | Bundler dans un seul PR |
| **P4** | Ajouter OWASP plugin en CI | Faible | Scan CVE automatique avant chaque release |

Pour Spring Boot, vérifier la version stable exacte sur [spring.io/projects/spring-boot](https://spring.io/projects/spring-boot) et mettre à jour la ligne `<version>` du parent dans `pom.xml`.
