package ma.projet.rh_conges.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.api.dto.SoldeDto;
import ma.projet.rh_conges.api.dto.TypeCongeDto;
import ma.projet.rh_conges.entity.Employe;
import ma.projet.rh_conges.security.UtilisateurCourant;
import ma.projet.rh_conges.service.DemandeCongeService;
import ma.projet.rh_conges.service.TypeCongeService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Soldes et types de congé")
public class SoldeApiController {

    private final DemandeCongeService demandeService;
    private final TypeCongeService typeCongeService;
    private final UtilisateurCourant utilisateurCourant;

    @GetMapping("/soldes/moi")
    @Operation(summary = "Mon solde par type de congé")
    public List<SoldeDto> monSolde(@RequestParam(required = false) Integer annee, Authentication auth) {
        Employe moi = utilisateurCourant.employe(auth);
        if (moi == null) throw new AccessDeniedException("Aucune fiche employé liée à ce compte");
        return soldes(moi.getId(), annee);
    }

    @GetMapping("/soldes/{employeId}")
    @Operation(summary = "Solde d'un employé", description = "RH, ou l'employé lui-même.")
    public List<SoldeDto> solde(@PathVariable Long employeId, @RequestParam(required = false) Integer annee,
                                Authentication auth) {
        utilisateurCourant.verifierAcces(auth, employeId);
        return soldes(employeId, annee);
    }

    @GetMapping("/types-conge")
    @Operation(summary = "Types de congé")
    public List<TypeCongeDto> types() {
        return typeCongeService.findAll().stream().map(TypeCongeDto::from).toList();
    }

    private List<SoldeDto> soldes(Long employeId, Integer annee) {
        int an = annee != null ? annee : LocalDate.now().getYear();
        return demandeService.getSoldes(employeId, an).stream().map(SoldeDto::from).toList();
    }
}
