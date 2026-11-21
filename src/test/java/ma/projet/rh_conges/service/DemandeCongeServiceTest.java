package ma.projet.rh_conges.service;

import ma.projet.rh_conges.entity.DemandeConge;
import ma.projet.rh_conges.entity.Employe;
import ma.projet.rh_conges.entity.StatutDemande;
import ma.projet.rh_conges.entity.TypeConge;
import ma.projet.rh_conges.exception.DemandeCongeException;
import ma.projet.rh_conges.exception.QuotaDepasseException;
import ma.projet.rh_conges.repository.DemandeCongeRepository;
import ma.projet.rh_conges.repository.EmployeRepository;
import ma.projet.rh_conges.repository.TypeCongeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemandeCongeServiceTest {

    @Mock
    private DemandeCongeRepository demandeRepo;
    @Mock
    private TypeCongeRepository typeRepo;
    @Mock
    private EmployeRepository employeRepo;

    private DemandeCongeService service;

    private final Employe karim = Employe.builder().id(1L).nom("Karim").departement("Informatique")
            .dateEmbauche(LocalDate.of(2019, 3, 15)).build();
    private final TypeConge annuel = TypeConge.builder().id(10L).libelle("Congé Annuel").quotaAnnuel(10).build();
    private final List<DemandeConge> existantes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new DemandeCongeService(demandeRepo, typeRepo, employeRepo, new JoursOuvresCalculator(""));
        when(employeRepo.findById(1L)).thenReturn(Optional.of(karim));
        when(typeRepo.findById(10L)).thenReturn(Optional.of(annuel));
        when(typeRepo.findAll()).thenReturn(List.of(annuel));
        when(demandeRepo.findByEmployeId(1L)).thenReturn(existantes);
        when(demandeRepo.save(any(DemandeConge.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private DemandeConge nouvelle(String debut, String fin) {
        return DemandeConge.builder()
                .employe(Employe.builder().id(1L).build())
                .typeConge(TypeConge.builder().id(10L).build())
                .dateDebut(LocalDate.parse(debut)).dateFin(LocalDate.parse(fin))
                .build();
    }

    private DemandeConge existante(long id, String debut, String fin, int jours, StatutDemande statut) {
        DemandeConge d = DemandeConge.builder().id(id).employe(karim).typeConge(annuel)
                .dateDebut(LocalDate.parse(debut)).dateFin(LocalDate.parse(fin))
                .nombreJours(jours).statut(statut).build();
        existantes.add(d);
        return d;
    }

    // ---- Soumission ----

    @Test
    void soumission_calculeLesJoursOuvresEtMetEnAttente() {
        DemandeConge d = service.soumettreDemande(nouvelle("2026-11-16", "2026-11-22"));

        assertThat(d.getNombreJours()).isEqualTo(4); // 18 novembre férié, week-end exclu
        assertThat(d.getStatut()).isEqualTo(StatutDemande.EN_ATTENTE);
        assertThat(d.getEmploye()).isSameAs(karim);
        verify(demandeRepo).save(d);
    }

    @Test
    void soumission_refuseeSiQuotaDepasse() {
        existante(100, "2026-03-02", "2026-03-06", 5, StatutDemande.ACCEPTE);
        existante(101, "2026-04-06", "2026-04-08", 3, StatutDemande.EN_ATTENTE);

        // 5 acceptés + 3 en attente + 5 demandés > 10
        assertThatThrownBy(() -> service.soumettreDemande(nouvelle("2026-11-23", "2026-11-27")))
                .isInstanceOfSatisfying(QuotaDepasseException.class,
                        e -> assertThat(e.getJoursDisponibles()).isEqualTo(2));
        verify(demandeRepo, never()).save(any());
    }

    @Test
    void soumission_lesDemandesRefuseesNeComptentPas() {
        existante(100, "2026-03-02", "2026-03-13", 10, StatutDemande.REFUSE);

        DemandeConge d = service.soumettreDemande(nouvelle("2026-11-23", "2026-11-27"));
        assertThat(d.getNombreJours()).isEqualTo(5);
    }

    @Test
    void soumission_lesAutresAnneesNeComptentPas() {
        existante(100, "2025-03-03", "2025-03-14", 10, StatutDemande.ACCEPTE);

        assertThat(service.soumettreDemande(nouvelle("2026-11-23", "2026-11-27")).getNombreJours()).isEqualTo(5);
    }

    @Test
    void modification_neSeComptePasElleMeme() {
        existante(100, "2026-11-23", "2026-11-27", 5, StatutDemande.EN_ATTENTE);
        existante(101, "2026-03-02", "2026-03-06", 5, StatutDemande.ACCEPTE);

        DemandeConge modif = nouvelle("2026-11-23", "2026-11-27");
        modif.setId(100L);
        assertThat(service.soumettreDemande(modif).getNombreJours()).isEqualTo(5);
    }

    @Test
    void soumission_refuseeSiChevauchement() {
        existante(100, "2026-11-25", "2026-11-26", 2, StatutDemande.EN_ATTENTE);

        assertThatThrownBy(() -> service.soumettreDemande(nouvelle("2026-11-23", "2026-11-27")))
                .isInstanceOf(DemandeCongeException.class)
                .hasMessageContaining("chevauche");
    }

    @Test
    void soumission_refuseeSiFinAvantDebut() {
        assertThatThrownBy(() -> service.soumettreDemande(nouvelle("2026-11-27", "2026-11-23")))
                .isInstanceOf(DemandeCongeException.class)
                .hasMessageContaining("date de fin");
    }

    @Test
    void soumission_refuseeSiAucunJourOuvre() {
        assertThatThrownBy(() -> service.soumettreDemande(nouvelle("2026-11-21", "2026-11-22")))
                .isInstanceOf(DemandeCongeException.class)
                .hasMessageContaining("aucun jour ouvré");
    }

    @Test
    void soumission_refuseeSiSurDeuxAnnees() {
        assertThatThrownBy(() -> service.soumettreDemande(nouvelle("2026-12-28", "2027-01-08")))
                .isInstanceOf(DemandeCongeException.class)
                .hasMessageContaining("année civile");
    }

    @Test
    void soumission_refuseeSansTypeDeConge() {
        DemandeConge d = nouvelle("2026-11-23", "2026-11-27");
        d.setTypeConge(null);
        assertThatThrownBy(() -> service.soumettreDemande(d)).isInstanceOf(DemandeCongeException.class);
    }

    // ---- Validation / rejet ----

    @Test
    void validation_accepteUneDemandeEnAttente() {
        DemandeConge d = existante(100, "2026-11-23", "2026-11-27", 5, StatutDemande.EN_ATTENTE);
        when(demandeRepo.findById(100L)).thenReturn(Optional.of(d));

        assertThat(service.valider(100L).getStatut()).isEqualTo(StatutDemande.ACCEPTE);
    }

    @Test
    void validation_refuseeSiLeQuotaEstDejaAtteint() {
        existante(101, "2026-03-02", "2026-03-13", 10, StatutDemande.ACCEPTE);
        DemandeConge d = existante(100, "2026-11-23", "2026-11-27", 5, StatutDemande.EN_ATTENTE);
        when(demandeRepo.findById(100L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.valider(100L)).isInstanceOf(QuotaDepasseException.class);
        assertThat(d.getStatut()).isEqualTo(StatutDemande.EN_ATTENTE);
    }

    @Test
    void validation_impossibleSurUneDemandeDejaTraitee() {
        DemandeConge d = existante(100, "2026-11-23", "2026-11-27", 5, StatutDemande.REFUSE);
        when(demandeRepo.findById(100L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.valider(100L)).hasMessageContaining("déjà été traitée");
    }

    @Test
    void rejet_passeLaDemandeAuStatutRefuse() {
        DemandeConge d = existante(100, "2026-11-23", "2026-11-27", 5, StatutDemande.EN_ATTENTE);
        when(demandeRepo.findById(100L)).thenReturn(Optional.of(d));

        assertThat(service.rejeter(100L).getStatut()).isEqualTo(StatutDemande.REFUSE);
    }

    @Test
    void demandeIntrouvable_leveUneErreurMetier() {
        when(demandeRepo.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(999L)).isInstanceOf(DemandeCongeException.class);
    }

    // ---- Soldes et statistiques ----

    @Test
    void solde_separeJoursPrisEtEnAttente() {
        existante(100, "2026-03-02", "2026-03-04", 3, StatutDemande.ACCEPTE);
        existante(101, "2026-04-06", "2026-04-07", 2, StatutDemande.EN_ATTENTE);
        existante(102, "2026-05-04", "2026-05-08", 5, StatutDemande.REFUSE);

        SoldeConge solde = service.getSoldes(1L, 2026).get(0);
        assertThat(solde.consommes()).isEqualTo(3);
        assertThat(solde.enAttente()).isEqualTo(2);
        assertThat(solde.restants()).isEqualTo(5);
        assertThat(service.getJoursConsommes(1L, 10L, 2026)).isEqualTo(3);
    }

    @Test
    void statistiques_tauxDAcceptationEtJoursParDepartement() {
        existante(100, "2026-03-02", "2026-03-04", 3, StatutDemande.ACCEPTE);
        existante(101, "2026-04-06", "2026-04-07", 2, StatutDemande.EN_ATTENTE);
        existante(102, "2026-05-04", "2026-05-08", 5, StatutDemande.REFUSE);
        existante(103, "2026-06-01", "2026-06-02", 2, StatutDemande.REFUSE);
        when(demandeRepo.findAllWithDetails()).thenReturn(existantes);

        Map<String, Object> stats = service.getStatistiquesGlobales();
        assertThat(stats).containsEntry("total", 4L).containsEntry("acceptes", 1L)
                .containsEntry("tauxAcceptation", 25.0);
        assertThat(service.getJoursParDepartement()).containsEntry("Informatique", 3L);
        assertThat(service.getStatistiquesParType()).singleElement()
                .satisfies(m -> assertThat(m).containsEntry("taux", 25.0));
    }
}
