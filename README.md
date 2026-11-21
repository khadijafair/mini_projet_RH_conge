# RH Congés — gestion des demandes de congé

Application web Spring Boot pour déposer, valider et suivre les congés des employés, avec deux rôles :

| Rôle | Ce qu'il peut faire |
|---|---|
| **Employé** | déposer une demande pour lui-même, la modifier ou l'annuler tant qu'elle est en attente, consulter son solde |
| **RH** | valider ou rejeter les demandes, gérer les employés (et créer leur compte), les types de congé, voir les statistiques |

## Fonctionnalités

- **Droits au congé annuel selon le Code du travail marocain** : 1,5 jour par mois de service (art. 231),
  +1,5 jour par tranche de 5 ans d'ancienneté dans la limite de 30 jours (art. 232), droit ouvert après
  6 mois de service, report des jours non pris de l'année précédente (art. 240). Calcul au prorata de
  l'embauche, à partir de la date d'embauche de l'employé.
- **API REST `/api/v1`** documentée avec OpenAPI (Swagger UI sur `/swagger-ui.html`) : recherche paginée et triée,
  dépôt, validation, rejet, annulation, soldes ; authentification par **jeton JWT** ; erreurs au format
  **RFC 9457** (`application/problem+json`, 422 avec les jours disponibles en cas de quota dépassé).

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

## API REST (exemple)

```bash
# 1. Obtenir un jeton
curl -s -X POST localhost:8080/api/v1/auth/token -H 'Content-Type: application/json' \
     -d '{"username":"karim","password":"karim123"}'
# 2. Déposer une demande
curl -s -X POST localhost:8080/api/v1/demandes -H "Authorization: Bearer $JETON" \
     -H 'Content-Type: application/json' -d '{"typeCongeId":1,"dateDebut":"2027-02-08","dateFin":"2027-02-12"}'
# 3. Mon solde
curl -s localhost:8080/api/v1/soldes/moi -H "Authorization: Bearer $JETON"
```

| Méthode | Route | Rôle |
|---|---|---|
| POST | `/api/v1/auth/token` | public |
| GET | `/api/v1/demandes?statut=&departement=&page=&taille=&tri=&ordre=` | employé (ses demandes) / RH |
| GET | `/api/v1/demandes/{id}` | propriétaire / RH |
| POST | `/api/v1/demandes` | employé / RH |
| POST | `/api/v1/demandes/{id}/validation`, `/rejet` | RH |
| DELETE | `/api/v1/demandes/{id}` | propriétaire (en attente) / RH |
| GET | `/api/v1/soldes/moi`, `/api/v1/soldes/{employeId}` | employé / RH |
| GET | `/api/v1/types-conge` | connecté |

## Sécurité

- Spring Security, connexion par formulaire, mots de passe hachés avec **BCrypt**.
- Autorisations par URL (`/employes`, `/types-conge`, `/stats`, validation et rejet réservés au rôle RH)
  et contrôle de propriété dans les contrôleurs (un employé n'accède qu'à ses propres demandes et à son solde).
- Toutes les actions qui modifient des données passent par des **POST protégés par jeton CSRF**.
- Deux chaînes de sécurité : interface web avec session et CSRF, API sans état avec jetons JWT signés HMAC-SHA256.

## Stack

Java 21 · Spring Boot 4 · Spring Security (OAuth2 Resource Server, JWT) · Spring Data JPA (Hibernate) · Bean Validation ·
springdoc-openapi · Flyway · Thymeleaf · Bootstrap 5 · Chart.js · H2 (dev) · MySQL 8 (prod) ·
JUnit 5 · Mockito · AssertJ · Spring Security Test · Testcontainers · JaCoCo · Docker · GitHub Actions

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
RH_ADMIN_PASSWORD=<mot-de-passe-RH> JWT_SECRET=<32-caracteres-minimum> docker compose up --build
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
  recherche multicritère ; API REST (JWT, codes 201/204/400/401/403/404/422, pagination, OpenAPI).
- **MySQL réel** (Testcontainers, profil prod) : migrations Flyway, requêtes paginées, compte RH initial.
  Ce test est ignoré si Docker n'est pas disponible.

Résultat en CI : **77 tests, 89,5 % des lignes couvertes**.

La CI GitHub Actions lance `mvn verify` à chaque push, publie le nombre de tests et la couverture dans le résumé
du workflow, puis construit l'image Docker.

## Structure

```
api/          API REST v1 (contrôleurs, DTO, gestion des erreurs RFC 9457)
controller/   pages Thymeleaf (demandes, soldes, employés, types, statistiques)
service/      règles métier (DemandeCongeService, JoursOuvresCalculator, DroitsCongeCalculator)
resources/db/migration/   migrations Flyway
security/     configuration Spring Security, chargement des comptes, utilisateur courant
entity/       Employe, TypeConge, DemandeConge, Utilisateur
repository/   Spring Data JPA
```
