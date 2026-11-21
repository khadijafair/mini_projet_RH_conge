package ma.projet.rh_conges.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.api.dto.DemandeDto;
import ma.projet.rh_conges.api.dto.NouvelleDemandeDto;
import ma.projet.rh_conges.api.dto.PageDto;
import ma.projet.rh_conges.entity.DemandeConge;
import ma.projet.rh_conges.entity.Employe;
import ma.projet.rh_conges.entity.StatutDemande;
import ma.projet.rh_conges.entity.TypeConge;
import ma.projet.rh_conges.security.UtilisateurCourant;
import ma.projet.rh_conges.service.DemandeCongeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/demandes")
@RequiredArgsConstructor
@Tag(name = "Demandes", description = "Dépôt, recherche, validation et annulation des demandes de congé")
public class DemandeApiController {

    private static final Set<String> TRIS_AUTORISES = Set.of("dateDebut", "dateFin", "nombreJours", "statut");

    private final DemandeCongeService service;
    private final UtilisateurCourant utilisateurCourant;

    @GetMapping
    @Operation(summary = "Rechercher des demandes (paginé)",
            description = "Un employé ne voit que ses demandes ; le RH voit tout et peut filtrer.")
    public PageDto<DemandeDto> rechercher(
            @RequestParam(required = false) StatutDemande statut,
            @RequestParam(required = false) String departement,
            @RequestParam(required = false) Long typeCongeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "dateDebut") String tri,
            @RequestParam(defaultValue = "desc") String ordre,
            Authentication auth) {
        if (!TRIS_AUTORISES.contains(tri)) {
            throw new IllegalArgumentException("Tri non autorisé : " + tri + " (valeurs : " + TRIS_AUTORISES + ")");
        }
        Sort sort = Sort.by("asc".equalsIgnoreCase(ordre) ? Sort.Direction.ASC : Sort.Direction.DESC, tri);
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, taille), 100), sort);
        String dept = (departement == null || departement.isBlank()) ? null : departement;
        return PageDto.of(service.rechercher(perimetre(auth), statut, dept, typeCongeId, dateDebut, dateFin,
                pageRequest), DemandeDto::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une demande")
    public DemandeDto detail(@PathVariable Long id, Authentication auth) {
        DemandeConge demande = service.findById(id);
        utilisateurCourant.verifierAcces(auth, demande.getEmploye().getId());
        return DemandeDto.from(demande);
    }

    @PostMapping
    @Operation(summary = "Déposer une demande",
            description = "Contrôle les jours ouvrés, le chevauchement et le quota ; renvoie 422 si une règle n'est pas respectée.")
    public ResponseEntity<DemandeDto> deposer(@Valid @RequestBody NouvelleDemandeDto requete, Authentication auth) {
        Employe employe;
        if (utilisateurCourant.estRH(auth) && requete.employeId() != null) {
            employe = Employe.builder().id(requete.employeId()).build();
        } else {
            employe = utilisateurCourant.employe(auth);
            if (employe == null) throw new AccessDeniedException("Aucune fiche employé liée à ce compte");
        }
        DemandeConge demande = DemandeConge.builder()
                .employe(employe)
                .typeConge(TypeConge.builder().id(requete.typeCongeId()).build())
                .dateDebut(requete.dateDebut())
                .dateFin(requete.dateFin())
                .motif(requete.motif())
                .build();
        DemandeDto cree = DemandeDto.from(service.soumettreDemande(demande));
        return ResponseEntity.created(URI.create("/api/v1/demandes/" + cree.id())).body(cree);
    }

    @PostMapping("/{id}/validation")
    @Operation(summary = "Valider une demande (RH)")
    public DemandeDto valider(@PathVariable Long id) {
        return DemandeDto.from(service.valider(id));
    }

    @PostMapping("/{id}/rejet")
    @Operation(summary = "Rejeter une demande (RH)")
    public DemandeDto rejeter(@PathVariable Long id) {
        return DemandeDto.from(service.rejeter(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Annuler une demande", description = "Employé : seulement ses demandes en attente.")
    public ResponseEntity<Void> annuler(@PathVariable Long id, Authentication auth) {
        utilisateurCourant.verifierModifiable(auth, service.findById(id));
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Employé : ses demandes uniquement ; RH : toutes. */
    private Long perimetre(Authentication auth) {
        if (utilisateurCourant.estRH(auth)) return null;
        Employe moi = utilisateurCourant.employe(auth);
        return moi != null ? moi.getId() : -1L;
    }
}
