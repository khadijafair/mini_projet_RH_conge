# RH Congés — gestion des demandes de congé

Application web Spring Boot pour déposer, valider et suivre les congés des employés, avec deux rôles :

| Rôle | Ce qu'il peut faire |
|---|---|
| **Employé** | déposer une demande pour lui-même, la modifier ou l'annuler tant qu'elle est en attente, consulter son solde |
| **RH** | valider ou rejeter les demandes, gérer les employés (et créer leur compte), les types de congé, voir les statistiques |

## Fonctionnalités

- **Jours ouvrés** : une demande compte les jours hors samedis, dimanches et jours fériés marocains
  (11 fêtes nationales à date fixe, dont la Fête de l'Unité depuis 2026 ; les fêtes religieuses,
  qui suivent le calendrier hégirien, se déclarent dans `rh.jours-feries-mobiles`).
- **Contrôle du quota à la soumission** : jours acceptés + jours en attente + nouvelle demande ≤ quota annuel du type ;
  le quota est revérifié au moment de la validation.
- **Autres règles** : pas de chevauchement avec une autre demande de l'employé, date de fin après la date de début,
  une demande reste dans la même année civile, une demande déjà traitée ne peut plus être validée ni rejetée.
- **Solde par type de congé** : quota, jours pris, en attente, restants.
- **Recherche multicritère** (statut, département, type, dates) en une seule requête JPQL avec `JOIN FETCH` (pas de N+1).
- **Tableau de bord RH** : taux d'acceptation, jours par département et par type (Chart.js).
- **Historique protégé** : un employé ou un type de congé utilisé par des demandes ne peut pas être supprimé.

## Sécurité

- Spring Security, connexion par formulaire, mots de passe hachés avec **BCrypt**.
- Autorisations par URL (`/employes`, `/types-conge`, `/stats`, validation et rejet réservés au rôle RH)
  et contrôle de propriété dans les contrôleurs (un employé n'accède qu'à ses propres demandes et à son solde).
- Toutes les actions qui modifient des données passent par des **POST protégés par jeton CSRF**.

## Stack

Java 21 · Spring Boot 4 · Spring Security · Spring Data JPA (Hibernate) · Bean Validation · Thymeleaf · Bootstrap 5 · Chart.js ·
H2 (dev) · MySQL 8 (prod) · JUnit 5 · Mockito · AssertJ · Spring Security Test · JaCoCo · Docker · GitHub Actions

## Lancer le projet

### En local (profil dev, H2 en mémoire et données de démo)

```bash
./mvnw spring-boot:run
```

Puis http://localhost:8080. Comptes de démonstration (profil dev uniquement) :

| Identifiant | Mot de passe | Rôle |
|---|---|---|
| `rh` | `rh123` | RH |
| `karim` | `karim123` | Employé (Informatique) |
| `fatima` | `fatima123` | Employé (RH) |

### Avec Docker (profil prod, MySQL)

```bash
RH_ADMIN_PASSWORD=<mot-de-passe-RH> docker compose up --build
```

Au premier démarrage, un compte `rh` est créé avec ce mot de passe ; le RH crée ensuite les employés et leurs comptes.

## Tests

```bash
./mvnw verify      # tests + rapport de couverture dans target/site/jacoco/index.html
```

- **Tests unitaires** (JUnit 5, Mockito) : calcul des jours ouvrés et jours fériés, règles de soumission
  (quota, chevauchement, dates), validation et rejet, soldes et statistiques.
- **Tests d'intégration** (`@SpringBootTest`, MockMvc, Spring Security Test) : connexion, droits par rôle,
  CSRF, refus des actions par GET, parcours de soumission et de validation, affichage de toutes les pages,
  recherche multicritère.

La CI GitHub Actions lance `mvn verify` à chaque push, publie le nombre de tests et la couverture dans le résumé
du workflow, puis construit l'image Docker.

## Structure

```
controller/   pages Thymeleaf (demandes, soldes, employés, types, statistiques)
service/      règles métier (DemandeCongeService, JoursOuvresCalculator)
security/     configuration Spring Security, chargement des comptes, utilisateur courant
entity/       Employe, TypeConge, DemandeConge, Utilisateur
repository/   Spring Data JPA
```
