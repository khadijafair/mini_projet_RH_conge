package ma.projet.rh_conges.web;

import ma.projet.rh_conges.entity.DemandeConge;
import ma.projet.rh_conges.entity.Employe;
import ma.projet.rh_conges.entity.StatutDemande;
import ma.projet.rh_conges.entity.Role;
import ma.projet.rh_conges.entity.TypeConge;
import ma.projet.rh_conges.entity.Utilisateur;
import ma.projet.rh_conges.repository.DemandeCongeRepository;
import ma.projet.rh_conges.repository.EmployeRepository;
import ma.projet.rh_conges.repository.TypeCongeRepository;
import ma.projet.rh_conges.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.WebApplicationContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Parcours complets avec la vraie configuration de sécurité et les données de démo (profil dev, H2).
 * Chaque test est annulé en fin d'exécution (@Transactional).
 */
@SpringBootTest
@Transactional
class SecuriteEtParcoursTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private DemandeCongeRepository demandeRepo;
    @Autowired
    private EmployeRepository employeRepo;
    @Autowired
    private TypeCongeRepository typeRepo;
    @Autowired
    private UtilisateurRepository utilisateurRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mvc;
    private Employe karim;
    private TypeConge annuel;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        karim = utilisateurRepo.findByUsername("karim").orElseThrow().getEmploye();
        annuel = typeRepo.findByLibelleIgnoreCase("Congé Annuel").orElseThrow();
    }

    /** Lundi d'une semaine sans jour férié, l'an prochain (évite les demandes de démo). */
    private LocalDate lundiLibre() {
        return LocalDate.of(LocalDate.now().getYear() + 1, 2, 1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)).plusWeeks(1);
    }

    private DemandeConge demandeEnAttente(Employe employe) {
        LocalDate lundi = lundiLibre();
        return demandeRepo.save(DemandeConge.builder().employe(employe).typeConge(annuel)
                .dateDebut(lundi).dateFin(lundi.plusDays(1)).nombreJours(2)
                .statut(StatutDemande.EN_ATTENTE).build());
    }

    // ---- Authentification ----

    @Test
    void visiteurAnonyme_redirigeVersLaConnexion() throws Exception {
        mvc.perform(get("/demandes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void connexionAvecLeBonMotDePasse() throws Exception {
        mvc.perform(formLogin("/login").user("karim").password("karim123"))
                .andExpect(authenticated().withUsername("karim").withRoles("EMPLOYE"));
    }

    @Test
    void connexionAvecUnMauvaisMotDePasse_refusee() throws Exception {
        mvc.perform(formLogin("/login").user("karim").password("mauvais"))
                .andExpect(unauthenticated());
    }

    // ---- Autorisations ----

    @Test
    @WithMockUser(username = "karim", roles = "EMPLOYE")
    void employe_nAccedePasAuxStatistiques() throws Exception {
        mvc.perform(get("/stats")).andExpect(status().isForbidden());
        mvc.perform(get("/employes")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH")
    void rh_accedeAuTableauDeBord() throws Exception {
        mvc.perform(get("/stats"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("statsGlobales", "joursParDept", "statsParType"));
    }

    @Test
    @WithMockUser(username = "karim", roles = "EMPLOYE")
    void employe_nePeutPasValider() throws Exception {
        DemandeConge d = demandeEnAttente(karim);
        mvc.perform(post("/demandes/{id}/valider", d.getId()).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(demandeRepo.findById(d.getId()).orElseThrow().getStatut()).isEqualTo(StatutDemande.EN_ATTENTE);
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH")
    void validationSansJetonCsrf_refusee() throws Exception {
        DemandeConge d = demandeEnAttente(karim);
        mvc.perform(post("/demandes/{id}/valider", d.getId())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH")
    void validationParLienGet_nonAutorisee() throws Exception {
        DemandeConge d = demandeEnAttente(karim);
        mvc.perform(get("/demandes/{id}/valider", d.getId())).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH")
    void rh_valideUneDemande() throws Exception {
        DemandeConge d = demandeEnAttente(karim);
        mvc.perform(post("/demandes/{id}/valider", d.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMsg"));
        assertThat(demandeRepo.findById(d.getId()).orElseThrow().getStatut()).isEqualTo(StatutDemande.ACCEPTE);
    }

    @Test
    @WithMockUser(username = "karim", roles = "EMPLOYE")
    void employe_neVoitQueSesDemandes() throws Exception {
        MvcResult res = mvc.perform(get("/demandes")).andExpect(status().isOk()).andReturn();
        @SuppressWarnings("unchecked")
        List<DemandeConge> demandes = (List<DemandeConge>) res.getModelAndView().getModel().get("demandes");
        assertThat(demandes).isNotEmpty().allMatch(d -> d.getEmploye().getId().equals(karim.getId()));
    }

    @Test
    @WithMockUser(username = "karim", roles = "EMPLOYE")
    void employe_nePeutPasSupprimerLaDemandeDUnCollegue() throws Exception {
        Employe houda = utilisateurRepo.findByUsername("houda").orElseThrow().getEmploye();
        DemandeConge d = demandeEnAttente(houda);
        mvc.perform(post("/demandes/{id}/supprimer", d.getId()).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(demandeRepo.existsById(d.getId())).isTrue();
    }

    @Test
    @WithMockUser(username = "karim", roles = "EMPLOYE")
    void employe_nePeutPasVoirLeSoldeDUnCollegue() throws Exception {
        Employe houda = utilisateurRepo.findByUsername("houda").orElseThrow().getEmploye();
        mvc.perform(get("/soldes/{id}", houda.getId())).andExpect(status().isForbidden());
        mvc.perform(get("/soldes")).andExpect(status().isOk()).andExpect(model().attributeExists("soldes"));
    }

    // ---- Affichage des pages ----

    @Test
    @WithMockUser(username = "rh", roles = "RH")
    void rh_toutesLesPagesSAffichent() throws Exception {
        DemandeConge d = demandeEnAttente(karim);
        for (String url : List.of("/demandes", "/demandes/new", "/demandes/edit/" + d.getId(),
                "/employes", "/employes/new", "/employes/edit/" + karim.getId(),
                "/types-conge", "/types-conge/new", "/types-conge/edit/" + annuel.getId(),
                "/stats", "/soldes/" + karim.getId(), "/demandes?statut=ACCEPTE&departement=")) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(username = "karim", roles = "EMPLOYE")
    void employe_sesPagesSAffichentSansLiensRH() throws Exception {
        for (String url : List.of("/demandes", "/demandes/new", "/soldes")) {
            String html = mvc.perform(get(url)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(html).doesNotContain("href=\"/stats\"");
        }
    }

    // ---- Soumission ----

    @Test
    @WithMockUser(username = "karim", roles = "EMPLOYE")
    void employe_soumetUneDemandePourLuiMeme() throws Exception {
        LocalDate lundi = lundiLibre();
        Employe houda = utilisateurRepo.findByUsername("houda").orElseThrow().getEmploye();
        long avant = demandeRepo.count();

        // même s'il envoie l'id d'un collègue, la demande est enregistrée à son nom
        mvc.perform(post("/demandes/save").with(csrf())
                        .param("employe.id", houda.getId().toString())
                        .param("typeConge.id", annuel.getId().toString())
                        .param("dateDebut", lundi.toString())
                        .param("dateFin", lundi.plusDays(6).toString()))
                .andExpect(status().is3xxRedirection());

        assertThat(demandeRepo.count()).isEqualTo(avant + 1);
        DemandeConge creee = demandeRepo.findByEmployeId(karim.getId()).stream()
                .filter(d -> d.getDateDebut().equals(lundi)).findFirst().orElseThrow();
        assertThat(creee.getNombreJours()).isEqualTo(5);
        assertThat(creee.getStatut()).isEqualTo(StatutDemande.EN_ATTENTE);
    }

    @Test
    @WithMockUser(username = "karim", roles = "EMPLOYE")
    void demandeAuDelaDuQuota_refuseeAvecMessage() throws Exception {
        LocalDate lundi = lundiLibre();
        long avant = demandeRepo.count();

        // 8 semaines de congé annuel : bien au-delà des 30 jours
        MvcResult res = mvc.perform(post("/demandes/save").with(csrf())
                        .param("typeConge.id", annuel.getId().toString())
                        .param("dateDebut", lundi.toString())
                        .param("dateFin", lundi.plusWeeks(8).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("demande/form"))
                .andReturn();

        BindingResult erreurs = (BindingResult) res.getModelAndView().getModel()
                .get(BindingResult.MODEL_KEY_PREFIX + "demande");
        assertThat(erreurs.getGlobalErrors()).singleElement()
                .satisfies(e -> assertThat(e.getDefaultMessage()).startsWith("Quota dépassé"));
        assertThat(demandeRepo.count()).isEqualTo(avant);
        assertThat(res.getResponse().getContentAsString()).contains("Quota dépassé");
    }

    // ---- Administration (RH) ----

    @Test
    @WithMockUser(username = "rh", roles = "RH")
    void rh_creeUnEmployeAvecSonCompte() throws Exception {
        mvc.perform(post("/employes/save").with(csrf())
                        .param("nom", "Sara Idrissi").param("departement", "Finance")
                        .param("dateEmbauche", "2024-09-01")
                        .param("username", "sara").param("motDePasse", "motdepasse1"))
                .andExpect(status().is3xxRedirection());

        Utilisateur sara = utilisateurRepo.findByUsername("sara").orElseThrow();
        assertThat(sara.getRole()).isEqualTo(Role.EMPLOYE);
        assertThat(sara.getEmploye().getNom()).isEqualTo("Sara Idrissi");
        assertThat(sara.getPassword()).isNotEqualTo("motdepasse1");
        assertThat(passwordEncoder.matches("motdepasse1", sara.getPassword())).isTrue();
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH")
    void motDePasseTropCourt_niEmployeNiCompteCrees() throws Exception {
        long avant = employeRepo.count();
        mvc.perform(post("/employes/save").with(csrf())
                        .param("nom", "Sara Idrissi").param("departement", "Finance")
                        .param("dateEmbauche", "2024-09-01")
                        .param("username", "sara").param("motDePasse", "court"))
                .andExpect(status().isOk())
                .andExpect(view().name("employe/form"));
        assertThat(employeRepo.count()).isEqualTo(avant);
        assertThat(utilisateurRepo.existsByUsername("sara")).isFalse();
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH")
    void supprimerUnTypeUtilise_refuseSansPerteDeDonnees() throws Exception {
        long demandes = demandeRepo.count();
        mvc.perform(post("/types-conge/{id}/supprimer", annuel.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMsg"));
        assertThat(typeRepo.existsById(annuel.getId())).isTrue();
        assertThat(demandeRepo.count()).isEqualTo(demandes);
    }

    // ---- Recherche multicritère ----

    @Test
    void rechercheMulticritere_filtreParStatutEtDepartement() {
        List<DemandeConge> acceptees = demandeRepo.findWithFilters(null, StatutDemande.ACCEPTE, null, null, null, null);
        assertThat(acceptees).isNotEmpty().allMatch(d -> d.getStatut() == StatutDemande.ACCEPTE);

        List<DemandeConge> info = demandeRepo.findWithFilters(null, null, "Informatique", null, null, null);
        assertThat(info).isNotEmpty().allMatch(d -> "Informatique".equals(d.getEmploye().getDepartement()));

        List<DemandeConge> deKarim = demandeRepo.findWithFilters(karim.getId(), null, null, null, null, null);
        assertThat(deKarim).isNotEmpty().allMatch(d -> d.getEmploye().getId().equals(karim.getId()));
    }
}
