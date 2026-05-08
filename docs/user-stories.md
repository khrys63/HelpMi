# HelpMi — User Stories

> Product backlog ordonné par priorité fonctionnelle.
> Version 1.0 — 2026-05-08

---

## 1. Gestion des tickets

### US-1 : Créer un ticket

**En tant que** membre d'un projet
**Je veux** créer un ticket avec un titre, une description, une priorité, un type et une date d'échéance
**Afin de** signaler un problème ou une demande

**Critères d'acceptation**
- Le champ titre est prépositionné au focus
- La référence est générée automatiquement (PROJ-N)
- Le reporter est l'utilisateur connecté
- Un message toast confirme la création

### US-2 : Modifier un ticket

**En tant que** membre d'un projet
**Je veux** modifier le titre, la description, la priorité, le type, la date d'échéance, les organisations concernées et les étiquettes
**Afin de** maintenir les informations du ticket à jour

**Critères d'acceptation**
- Les champs modifiables sont visibles sur l'écran du ticket
- Un message toast confirme la modification
- Les champs assigné et échéance sont en lecture seule pour les membres

### US-3 : Changer le statut d'un ticket (machine à états)

**En tant que** membre d'un projet
**Je veux** changer le statut d'un ticket via une interface graphique montrant les transitions possibles depuis le statut actuel
**Afin de** suivre l'avancement du ticket

**Critères d'acceptation**
- Les statuts sont : OPEN, IN_PROGRESS, STAND_BY, RESOLVED, CLOSED, CANCELLED
- Les transitions autorisées respectent la machine à états définie
- Les actions sont des verbes : démarrer, résoudre, fermer, réouvrir, annuler
- Les statuts CLOSED et CANCELLED figent le ticket (lecture seule)
- La réouverture est possible depuis CLOSED et CANCELLED

### US-4 : Résolution avec type et commentaire

**En tant que** gestionnaire d'un projet
**Je veux** en résolvant un ticket, saisir le type de résolution (obligatoire, défaut "Corrigé") et un commentaire optionnel
**Afin de** documenter comment le problème a été traité

**Types de résolution** : Corrigé, Contourné, Abandonné, Doublon

### US-5 : Réouverture avec commentaire

**En tant que** membre d'un projet
**Je veux** en réouvrant un ticket fermé ou annulé, saisir un commentaire obligatoire
**Afin de** justifier la réouverture

### US-6 : Cloner un ticket

**En tant que** membre d'un projet
**Je veux** dupliquer un ticket sur le même projet avec tous les champs copiés (sauf les tickets liés)
**Afin de** créer rapidement un ticket similaire

**Critères d'acceptation**
- Le clonage est interrompu si le ticket est fermé ou annulé
- Un message toast confirme le clonage et redirige vers le nouveau ticket

### US-7 : Déplacer un ticket entre projets

**En tant que** membre d'un projet
**Je veux** déplacer un ticket vers un autre projet
**Afin de** le réassigner au bon projet

**Critères d'acceptation**
- Le ticket obtient une nouvelle référence dans le projet cible
- L'ancienne référence devient invalide et non réutilisable
- Un message toast confirme le déplacement

### US-8 : Filtrer les tickets

**En tant que** membre d'un projet
**Je veux** filtrer la liste des tickets par statut, priorité, type et assigné (multi-sélection)
**Afin de** trouver rapidement les tickets qui m'intéressent

**Critères d'acceptation**
- Les choix "annulé" et "fermé" sont décochés par défaut
- Les filtres sont combinables
- Le nombre maximum de valeurs par filtre est de 20

### US-9 : Assigner un ticket

**En tant que** gestionnaire d'un projet
**Je veux** assigner un ticket à un utilisateur ayant un rôle dans le projet
**Afin de** désigner la personne responsable du ticket

**Critères d'acceptation**
- Un bouton "M'affecter" permet à l'utilisateur de s'assigner le ticket rapidement
- La liste des assignés est filtrée par les utilisateurs du projet
- Les membres ne peuvent pas modifier l'assigné

### US-10 : Ajouter des watchers (observateurs)

**En tant que** membre d'un projet
**Je veux** ajouter ou supprimer des utilisateurs comme observateurs d'un ticket
**Afin de** les tenir informés des évolutions du ticket

**Critères d'acceptation**
- On ne peut ajouter que des utilisateurs ayant un rôle dans le projet ou dans les organisations concernées
- Les watchers reçoivent des notifications (commentaire, changement de statut)

### US-11 : Ajouter des étiquettes (labels)

**En tant que** membre d'un projet
**Je veux** attacher des étiquettes à un ticket
**Afin de** le catégoriser visuellement

**Critères d'acceptation**
- Les labels existants s'affichent dans une liste de sélection
- Saisir un nouveau label le crée à la volée
- Les labels sont gérés depuis l'administration

### US-12 : Associer des organisations concernées

**En tant que** membre d'un projet
**Je veux** attacher une ou plusieurs organisations à un ticket
**Afin de** indiquer qui est impacté par ce ticket

**Critères d'acceptation**
- La liste ne propose que les organisations pour lesquelles l'utilisateur est rattaché

### US-13 : Lier des tickets entre eux

**En tant que** membre d'un projet
**Je veux** créer un lien entre deux tickets (dépend de, doublon de, lié à, etc.)
**Afin de** documenter les relations entre tickets

**Critères d'acceptation**
- Les types de liens sont configurables en admin
- Les libellés sont bidirectionnels (direct + inverse)
- Les types de liens sont configurés par paires

### US-14 : Ajouter des commentaires

**En tant que** membre d'un projet
**Je veux** ajouter, modifier et supprimer des commentaires sur un ticket
**Afin de** discuter et documenter l'évolution

**Critères d'acceptation**
- L'historique des modifications est visible (qui, quand, quoi)
- L'horodatage est affiché pour chaque commentaire
- Admin peut modifier/supprimer n'importe quel commentaire
- Gestionnaire et membre seulement les leurs

### US-15 : Joindre des fichiers à un ticket

**En tant que** membre d'un projet
**Je veux** uploader des fichiers sur un ticket et les télécharger
**Afin de** partager des documents, captures d'écran, logs

**Critères d'acceptation**
- Taille max : 10 Mo par fichier, 50 Mo par requête
- Le type MIME est validé par analyse binaire (Apache Tika)
- Les fichiers sont stockés dans un bucket S3/MinIO

### US-16 : Tickets récurrents

**En tant que** utilisateur d'un projet
**Je veux** créer des tickets de type ANNUEL, MENSUEL ou TRIMESTRIEL avec une date d'échéance
**Afin de** générer automatiquement des tickets périodiques

**Critères d'acceptation**
- À la clôture (CLOSED), un ticket clone est créé avec la date décalée
- Le clonage ne se déclenche pas pour les statuts CANCELLED ou RESOLVED
- Un message toast informe de la recréation

### US-17 : Voir l'historique d'un ticket

**En tant que** membre d'un projet
**Je veux** voir toutes les modifications faites sur un ticket (qui, quand, champ modifié, valeur avant/après)
**Afin de** reconstituer l'histoire du ticket

---

## 2. Projets

### US-18 : Créer un projet

**En tant qu'**administrateur
**Je veux** créer un projet avec un nom, une clé unique et une description
**Afin de** structurer les tickets

**Critères d'acceptation**
- Le créateur est automatiquement ajouté comme membre du projet
- La clé est en majuscules, unique et limitée à 10 caractères

### US-19 : Modifier un projet

**En tant qu'**administrateur
**Je veux** modifier le nom et la description d'un projet
**Afin de** mettre à jour les informations du projet

**Critères d'acceptation**
- La clé du projet n'est pas modifiable
- Le curseur est prépositionné dans le premier champ

### US-20 : Désactiver un projet

**En tant qu'**administrateur
**Je veux** supprimer un projet de manière logique
**Afin de** l'empêcher d'apparaître dans les listes

**Critères d'acceptation**
- L'opération est irréversible depuis l'interface

### US-21 : Archiver un projet

**En tant qu'**administrateur
**Je veux** archiver un projet
**Afin de** le rendre invisible pour les non-admins et bloquer la création de nouveaux tickets

**Critères d'acceptation**
- Le projet reste visible pour les admins avec un badge
- L'archivage est réversible
- Les tickets du projet ne sont plus comptés dans les dashboards
- Les tickets existants ne sont pas supprimés

### US-22 : Voir la liste des projets

**En tant que** membre ou admin
**Je veux** voir la liste de mes projets avec le nombre de tickets actifs, les organisations associées et mon rôle
**Afin de** naviguer rapidement vers le projet souhaité

**Critères d'acceptation**
- Les projets archivés ne sont visibles que par les admins
- Le compteur exclut les tickets CANCELLED, CLOSED et RESOLVED

---

## 3. Administration

### US-23 : Gérer les utilisateurs

**En tant qu'**administrateur
**Je veux** voir la liste des utilisateurs, modifier leur rôle, leur activation, leurs organisations et leurs projets
**Afin de** gérer les accès à l'application

**Critères d'acceptation**
- Champ de recherche libre pour trouver rapidement un utilisateur
- Un utilisateur sans organisation voit un écran d'attente
- L'admin peut affecter un utilisateur à une ou plusieurs organisations et projets

### US-24 : Gérer les organisations

**En tant qu'**administrateur
**Je veux** créer, modifier et supprimer des organisations, et y attacher des projets
**Afin de** structurer la hiérarchie de l'application

**Critères d'acceptation**
- Les utilisateurs rattachés sont visibles en lecture seule (gestionnaires / membres)
- La gestion des utilisateurs directement dans l'organisation n'est pas disponible

### US-25 : Configurer les valeurs de l'application

**En tant qu'**administrateur
**Je veux** configurer les statuts, priorités, types de ticket, types de liens et rôles projet
**Afin de** adapter l'application aux besoins de l'organisation

**Critères d'acceptation**
- Les libellés sont configurables en fr, en, bg
- Les couleurs sont configurables via un color picker
- Les types de liens sont configurés par paires (direct + inverse)
- Les valeurs peuvent être activées/désactivées

### US-26 : Consulter le journal d'audit

**En tant qu'**administrateur
**Je veux** consulter toutes les actions sensibles faites sur l'application
**Afin de** assurer la traçabilité et la sécurité

**Critères d'acceptation**
- Le journal est filtrable par date, action, utilisateur
- 50 entrées par page
- Les actions auditées incluent : création de ticket, suppression, connexions PAT, changements de rôle, etc.

---

## 4. Dashboards

### US-27 : Voir mon dashboard personnel

**En tant que** membre
**Je veux** voir mes tickets ouverts, assignés, observés et à venir, ainsi que les statistiques de mes projets
**Afin de** suivre mon activité

**Critères d'acceptation**
- Exclut les tickets CLOSED, CANCELLED et RESOLVED
- Les projets affichent le nombre de tickets par statut (OPEN, IN_PROGRESS, STAND_BY)
- Les tickets sont triés par date d'échéance

### US-28 : Voir le suivi gestionnaire

**En tant que** gestionnaire d'un projet
**Je veux** voir l'avancement des tickets de mes projets par utilisateur assigné
**Afin de** suivre la charge de mon équipe

**Critères d'acceptation**
- Vue arborescente : Projet → Assigné → Tickets
- Graphiques : bar chart intro par projet + donut par utilisateur
- Liste des tickets non assignés
- Affichage des statuts en langue de l'utilisateur

---

## 5. Profil et préférences

### US-29 : Personnaliser mon profil

**En tant que** membre
**Je veux** configurer mon thème (clair/sombre), ma langue, et mes préférences de notifications email
**Afin de** personnaliser mon expérience

**Critères d'acceptation**
- Le thème est persisté par utilisateur
- La langue affecte tous les libellés de l'interface
- Les notifications peuvent être activées/désactivées par type
- Les notifications sont envoyées uniquement aux utilisateurs actifs

### US-30 : Voir les tickets de mes projets

**En tant que** membre
**Je veux** voir les tickets de mes projets, filtrés par mes organisations
**Afin de** voir uniquement ce qui me concerne

### US-31 : Gérer mes tokens d'accès personnel (PAT)

**En tant que** membre
**Je veux** créer, lister et révoquer des tokens d'accès personnel
**Afin de** m'authentifier à l'API depuis la ligne de commande

**Critères d'acceptation**
- Les tokens sont stockés sous forme de hash SHA-256
- Les tokens peuvent expirer (date optionnelle)
- La révocation est immédiate
- Le journal d'audit enregistre les actions PAT

### US-32 : Me déconnecter

**En tant que** membre
**Je veux** me déconnecter de l'application
**Afin de** quitter proprement ma session

**Critères d'acceptation**
- Le bouton est dans le profil
- Le token Keycloak est invalidé

---

## 6. Notifications

### US-33 : Être notifié par email

**En tant que** membre
**Je veux** recevoir des emails pour les événements qui m'intéressent
**Afin de** suivre les tickets sans avoir à vérifier l'application

**Événements**
| Événement | Destinataires |
|---|---|
| Ticket assigné à moi | Moi uniquement |
| Commentaire ajouté | Reporter + assigné + watchers (excluant l'auteur) |
| Statut modifié | Reporter + assigné + watchers (excluant l'acteur) |
| Ajouté comme watcher | Moi uniquement |
| Ticket créé | Tous les gestionnaires du projet |

**Critères d'acceptation**
- Je ne reçois jamais d'email pour mes propres actions
- Je peux désactiver chaque type de notification
- Un envoi raté ne bloque pas l'action métier

---

## 7. Sécurité et conformité

### US-34 : Accéder avec mon compte entreprise

**En tant qu'**utilisateur
**Je veux** me connecter avec mon compte Keycloak (Single Sign-On)
**Afin de** ne pas avoir de mot de passe spécifique

**Critères d'acceptation**
- SSO via OIDC avec PKCE S256
- Le rôle ADMIN est détecté dans le token JWT

### US-35 : Utiliser l'application en mobile et en dark mode

**En tant que** membre
**Je veux** une interface qui s'adapte à l'écran et à mes préférences de thème
**Afin de** travailler confortablement sur n'importe quel appareil

> ⚠️ **Non implémenté** — Le dark mode est implémenté (préférence par utilisateur), mais l'interface n'est pas responsive/mobile (pas de media queries, pas de menu hamburger, pas de touch targets dimensionnés). Application conçue pour desktop.

### US-36 : Utiliser l'application dans ma langue

**En tant que** membre
**Je veux** l'application dans ma langue préférée
**Afin de** ne pas avoir de barrière linguistique

**Langues supportées** : Français, English, Български

---

## 8. Opérationnel

### US-37 : Sauvegarder et restaurer les données

**En tant qu'**opérateur
**Je veux** pouvoir sauvegarder la base de données et les fichiers
**Afin de** protéger les données contre la perte

> ⚠️ **Non implémenté** — Le runbook (`ops/runbook.md`) décrit les commandes manuelles (`mysqldump`, `tar`), mais il n'y a aucune automatisation (pas de cron, pas de script de déploiement, pas de vérification d'intégrité, pas de test de restauration).

### US-38 : Déployer une nouvelle version

**En tant qu'**opérateur
**Je veux** déployer une nouvelle version en une commande
**Afin de** minimiser les temps d'arrêt

> ⚠️ **Non implémenté** — Aucun pipeline CI/CD. Le runbook décrit une procédure manuelle (`git pull` → `docker compose build` → `docker compose up`). Pas de rollback automatique, pas de pré-production, pas de notifications de déploiement.

### US-39 : Diagnostiquer les incidents

**En tant qu'**opérateur
**Je veux** un runbook détaillé pour diagnostiquer et résoudre les incidents
**Afin de** intervenir rapidement, même sans connaître l'application

---

## Priorisation par phase

### Phase 1 — MVP
- US-1 à US-17 (gestion tickets)
- US-18 à US-22 (gestion projets)
- US-23 (gestion utilisateurs)
- US-34 (authentification)

### Phase 2 — Organisation
- US-24 (gestion organisations)
- US-25 (configuration application)
- US-27 (dashboard personnel)

### Phase 3 — Collaboration
- US-28 (suivi gestionnaire)
- US-33 (notifications email)
- US-15 (pièces jointes)

### Phase 4 — Administration
- US-26 (journal d'audit)
- US-29 (profil et préférences)
- US-31 (PAT)

### Phase 5 — Optimisation
- US-36 (i18n)
- US-39 (runbook)
- ~~US-38~~ (déploiement automatisé) — non implémenté
- ~~US-35~~ (mobile) — partiellement implémenté (thème uniquement)

---

## Résumé

| US | Statut |
|---|---|
| US-1 à US-34 | ✅ Implémenté |
| US-35 | ⚠️ Partiel (thème clair/sombre seulement) |
| US-36 à US-39 | ❌ Non implémenté (US-39 documenté dans le runbook, non automatisé) |

