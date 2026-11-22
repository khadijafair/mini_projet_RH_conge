package ma.projet.rh_conges.api;

import com.jayway.jsonpath.JsonPath;
import ma.projet.rh_conges.entity.DemandeConge;
import ma.projet.rh_conges.entity.Employe;
import ma.projet.rh_conges.entity.StatutDemande;
import ma.projet.rh_conges.entity.TypeConge;
import ma.projet.rh_conges.repository.DemandeCongeRepository;
import ma.projet.rh_conges.repository.TypeCongeRepository;
import ma.projet.rh_conges.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** API REST de bout en bout : jeton JWT, droits, règles métier, erreurs RFC 9457, pagination, OpenAPI. */
@SpringBootTest
@Transactional
class DemandeApiTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private DemandeCongeRepository demandeRepo;
    @Autowired
    private TypeCongeRepository typeRepo;
    @Autowired
    private UtilisateurRepository utilisateurRepo;

    private MockMvc mvc;
    private Employe karim;
    private TypeConge annuel;
    private TypeConge exceptionnel;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        karim = utilisateurRepo.findByUsername("karim").orElseThrow().getEmploye();
        annuel = typeRepo.findByLibelleIgnoreCase("Congé Annuel").orElseThrow();
        exceptionnel = typeRepo.findByLibelleIgnoreCase("Congé Exceptionnel").orElseThrow();
    }

    private String jeton(String utilisateur) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + utilisateur + "\",\"password\":\"" + utilisateur + "123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.accessToken");
    }

    private LocalDate lundiLibre() {
        return LocalDate.of(LocalDate.now().getYear() + 1, 2, 1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)).plusWeeks(1);
    }

    private String corps(TypeConge type, LocalDate debut, LocalDate fin) {
        return "{\"typeCongeId\":" + type.getId() + ",\"dateDebut\":\"" + debut + "\",\"dateFin\":\"" + fin + "\"}";
    }

    // ---- Authentification ----

    @Test
    void sansJeton_401() throws Exception {
        mvc.perform(get("/api/v1/demandes")).andExpect(status().isUnauthorized());
    }

    @Test
    void jetonFalsifie_401() throws Exception {
        mvc.perform(get("/api/v1/demandes").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mauvaisMotDePasse_401AuFormatProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/auth/token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"karim\",\"password\":\"faux\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Authentification refusée"));
    }

    // ---- Lecture ----

    @Test
    void employe_neVoitQueSesDemandes_avecPagination() throws Exception {
        mvc.perform(get("/api/v1/demandes").param("taille", "1").header("Authorization", jeton("karim")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu", hasSize(1)))
                .andExpect(jsonPath("$.taille").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.contenu[0].employeId").value(karim.getId().intValue()));
    }

    @Test
    void rh_filtreParStatutEtTrie() throws Exception {
        mvc.perform(get("/api/v1/demandes").param("statut", "ACCEPTE").param("tri", "dateDebut").param("ordre", "asc")
                        .header("Authorization", jeton("rh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.contenu[*].statut", everyItem(is("ACCEPTE"))));
    }

    @Test
    void triNonAutorise_400() throws Exception {
        mvc.perform(get("/api/v1/demandes").param("tri", "motif").header("Authorization", jeton("rh")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Tri non autorisé")));
    }

    @Test
    void demandeDUnCollegue_403() throws Exception {
        Employe houda = utilisateurRepo.findByUsername("houda").orElseThrow().getEmploye();
        DemandeConge d = demandeRepo.findByEmployeId(houda.getId()).get(0);
        mvc.perform(get("/api/v1/demandes/{id}", d.getId()).header("Authorization", jeton("karim")))
                .andExpect(status().isForbidden());
    }

    @Test
    void demandeInexistante_404() throws Exception {
        mvc.perform(get("/api/v1/demandes/999999").header("Authorization", jeton("rh")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void solde_et_typesDeConge() throws Exception {
        String auth = jeton("karim");
        mvc.perform(get("/api/v1/soldes/moi").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.typeConge == 'Congé Annuel')].calculLegal", contains(true)))
                .andExpect(jsonPath("$[?(@.typeConge == 'Congé Annuel')].droits", contains(greaterThanOrEqualTo(18))));
        mvc.perform(get("/api/v1/types-conge").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    // ---- Écriture ----

    @Test
    void employe_deposeUneDemande_201() throws Exception {
        LocalDate lundi = lundiLibre();
        mvc.perform(post("/api/v1/demandes").header("Authorization", jeton("karim"))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(annuel, lundi, lundi.plusDays(4))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/demandes/")))
                .andExpect(jsonPath("$.nombreJours").value(5))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.employeId").value(karim.getId().intValue()));
    }

    @Test
    void quotaDepasse_422AvecJoursDisponibles() throws Exception {
        LocalDate lundi = lundiLibre();
        // Congé exceptionnel : 5 jours par an ; 10 jours ouvrés demandés
        mvc.perform(post("/api/v1/demandes").header("Authorization", jeton("karim"))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(exceptionnel, lundi, lundi.plusDays(11))))
                .andExpect(status().is(422))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Quota dépassé"))
                .andExpect(jsonPath("$.joursDemandes").value(10))
                .andExpect(jsonPath("$.joursDisponibles").value(5));
    }

    @Test
    void champsManquants_400AvecDetailParChamp() throws Exception {
        mvc.perform(post("/api/v1/demandes").header("Authorization", jeton("karim"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"typeCongeId\":" + annuel.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.dateDebut").value("La date de début est obligatoire"))
                .andExpect(jsonPath("$.erreurs.dateFin").exists());
    }

    @Test
    void employe_nePeutPasValider_403() throws Exception {
        DemandeConge d = demandeEnAttente();
        mvc.perform(post("/api/v1/demandes/{id}/validation", d.getId()).header("Authorization", jeton("karim")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rh_valide_puisNePeutPlusValider() throws Exception {
        DemandeConge d = demandeEnAttente();
        String auth = jeton("rh");
        mvc.perform(post("/api/v1/demandes/{id}/validation", d.getId()).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACCEPTE"));
        mvc.perform(post("/api/v1/demandes/{id}/rejet", d.getId()).header("Authorization", auth))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail", containsString("déjà été traitée")));
    }

    @Test
    void employe_annuleSaDemandeEnAttente_204() throws Exception {
        DemandeConge d = demandeEnAttente();
        mvc.perform(delete("/api/v1/demandes/{id}", d.getId()).header("Authorization", jeton("karim")))
                .andExpect(status().isNoContent());
        assertThat(demandeRepo.existsById(d.getId())).isFalse();
    }

    // ---- Documentation ----

    @Test
    void documentationOpenApiPublique() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/demandes']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.jwt.scheme").value("bearer"));
    }

    private DemandeConge demandeEnAttente() {
        LocalDate lundi = lundiLibre();
        return demandeRepo.save(DemandeConge.builder().employe(karim).typeConge(annuel)
                .dateDebut(lundi).dateFin(lundi.plusDays(1)).nombreJours(2)
                .statut(StatutDemande.EN_ATTENTE).build());
    }
}
