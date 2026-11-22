package ma.projet.rh_conges.mysql;

import ma.projet.rh_conges.entity.*;
import ma.projet.rh_conges.exception.DemandeCongeException;
import ma.projet.rh_conges.repository.DemandeCongeRepository;
import ma.projet.rh_conges.repository.EmployeRepository;
import ma.projet.rh_conges.repository.TypeCongeRepository;
import ma.projet.rh_conges.repository.UtilisateurRepository;
import ma.projet.rh_conges.service.DemandeCongeService;
import ma.projet.rh_conges.service.TypeCongeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Profil prod sur un vrai MySQL 8 (Testcontainers) : migrations Flyway, requêtes JPQL paginées,
 * enum stocké en texte, compte RH initial. Ignoré si Docker n'est pas disponible.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.profiles.active=prod",
        "RH_ADMIN_PASSWORD=motdepasse-de-test",
        "JWT_SECRET=cle-de-test-suffisamment-longue-pour-hmac-256"
})
@Transactional
class MySqlIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private UtilisateurRepository utilisateurRepo;
    @Autowired private EmployeRepository employeRepo;
    @Autowired private TypeCongeRepository typeRepo;
    @Autowired private DemandeCongeRepository demandeRepo;
    @Autowired private DemandeCongeService demandeService;
    @Autowired private TypeCongeService typeService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void migrationsFlywayAppliquees() {
        Integer reussies = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);
        assertThat(reussies).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT VERSION()", String.class)).startsWith("8.");
    }

    @Test
    void compteRhInitialCree() {
        Utilisateur rh = utilisateurRepo.findByUsername("rh").orElseThrow();
        assertThat(rh.getRole()).isEqualTo(Role.RH);
        assertThat(passwordEncoder.matches("motdepasse-de-test", rh.getPassword())).isTrue();
    }

    @Test
    void parcoursCompletSurMySql() {
        Employe sara = employeRepo.save(Employe.builder().nom("Sara Idrissi").departement("Finance")
                .dateEmbauche(LocalDate.of(2020, 1, 6)).build());
        TypeConge annuel = typeRepo.save(TypeConge.builder().libelle("Congé Annuel").quotaAnnuel(30)
                .calculLegal(true).build());

        DemandeConge d = demandeService.soumettreDemande(DemandeConge.builder()
                .employe(Employe.builder().id(sara.getId()).build())
                .typeConge(TypeConge.builder().id(annuel.getId()).build())
                .dateDebut(LocalDate.of(2026, 11, 16)).dateFin(LocalDate.of(2026, 11, 20)).build());
        assertThat(d.getNombreJours()).isEqualTo(4); // 18 novembre férié
        demandeRepo.flush();

        assertThat(jdbc.queryForObject("SELECT statut FROM demandes_conge WHERE id = ?", String.class, d.getId()))
                .isEqualTo("EN_ATTENTE");
        assertThat(demandeService.rechercher(sara.getId(), StatutDemande.EN_ATTENTE, "Finance", annuel.getId(),
                null, null, PageRequest.of(0, 10, Sort.by("dateDebut"))).getTotalElements()).isEqualTo(1);

        demandeService.valider(d.getId());
        assertThat(demandeService.getJoursConsommes(sara.getId(), annuel.getId(), 2026)).isEqualTo(4);

        assertThatThrownBy(() -> typeService.deleteById(annuel.getId()))
                .isInstanceOf(DemandeCongeException.class);
    }
}
