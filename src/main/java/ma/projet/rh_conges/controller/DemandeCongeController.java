package ma.projet.rh_conges.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.entity.*;
import ma.projet.rh_conges.exception.DemandeCongeException;
import ma.projet.rh_conges.security.UtilisateurCourant;
import ma.projet.rh_conges.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/demandes")
@RequiredArgsConstructor
public class DemandeCongeController {

    private final DemandeCongeService demandeCongeService;
    private final EmployeService employeService;
    private final TypeCongeService typeCongeService;
    private final UtilisateurCourant utilisateurCourant;

    @GetMapping
    public String list(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String departement,
            @RequestParam(required = false) Long typeCongeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            Authentication auth,
            Model model) {

        StatutDemande statutEnum = null;
        if (statut != null && !statut.isBlank()) {
            try { statutEnum = StatutDemande.valueOf(statut); } catch (IllegalArgumentException ignored) {}
        }
        String dept = (departement == null || departement.isBlank()) ? null : departement;

        // Un employé ne voit que ses propres demandes.
        Long employeId = null;
        if (!utilisateurCourant.estRH(auth)) {
            Employe moi = utilisateurCourant.employe(auth);
            employeId = moi != null ? moi.getId() : -1L;
        }

        model.addAttribute("demandes",
                demandeCongeService.findWithFilters(employeId, statutEnum, dept, typeCongeId, dateDebut, dateFin));
        model.addAttribute("statuts", StatutDemande.values());
        model.addAttribute("departements", employeService.findAllDepartements());
        model.addAttribute("types", typeCongeService.findAll());
        model.addAttribute("selectedStatut", statut);
        model.addAttribute("selectedDept", dept);
        model.addAttribute("selectedType", typeCongeId);
        model.addAttribute("selectedDebut", dateDebut);
        model.addAttribute("selectedFin", dateFin);
        return "demande/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Authentication auth, Model model) {
        DemandeConge demande = new DemandeConge();
        if (!utilisateurCourant.estRH(auth)) {
            demande.setEmploye(utilisateurCourant.employe(auth));
        }
        model.addAttribute("demande", demande);
        remplirListes(model);
        return "demande/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("demande") DemandeConge demande,
                       BindingResult result,
                       Authentication auth,
                       Model model,
                       RedirectAttributes ra) {
        boolean rh = utilisateurCourant.estRH(auth);
        if (demande.getId() != null) {
            verifierModifiable(auth, demandeCongeService.findById(demande.getId()));
        }
        if (!rh) {
            // L'employé ne peut déposer une demande que pour lui-même.
            Employe moi = utilisateurCourant.employe(auth);
            if (moi == null) throw new AccessDeniedException("Aucune fiche employé liée à ce compte");
            demande.setEmploye(moi);
        }
        // Pour un employé, le champ « employé » est imposé : ses éventuelles erreurs de saisie ne comptent pas.
        boolean erreurs = rh ? result.hasErrors()
                : result.hasGlobalErrors() || result.getFieldErrors().stream()
                        .anyMatch(f -> !f.getField().startsWith("employe"));
        if (erreurs) {
            remplirListes(model);
            return "demande/form";
        }
        try {
            demandeCongeService.soumettreDemande(demande);
        } catch (DemandeCongeException e) {
            result.reject("demande.invalide", e.getMessage());
            remplirListes(model);
            return "demande/form";
        }
        ra.addFlashAttribute("successMsg", "Demande soumise avec succès !");
        return "redirect:/demandes";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Authentication auth, Model model) {
        DemandeConge demande = demandeCongeService.findById(id);
        verifierModifiable(auth, demande);
        model.addAttribute("demande", demande);
        remplirListes(model);
        return "demande/form";
    }

    @PostMapping("/{id}/valider")
    public String valider(@PathVariable Long id, RedirectAttributes ra) {
        try {
            demandeCongeService.valider(id);
            ra.addFlashAttribute("successMsg", "Demande validée ✔");
        } catch (DemandeCongeException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/demandes";
    }

    @PostMapping("/{id}/rejeter")
    public String rejeter(@PathVariable Long id, RedirectAttributes ra) {
        try {
            demandeCongeService.rejeter(id);
            ra.addFlashAttribute("errorMsg", "Demande rejetée ✖");
        } catch (DemandeCongeException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/demandes";
    }

    @PostMapping("/{id}/supprimer")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        verifierModifiable(auth, demandeCongeService.findById(id));
        demandeCongeService.deleteById(id);
        ra.addFlashAttribute("successMsg", "Demande supprimée.");
        return "redirect:/demandes";
    }

    /** RH : toute demande. Employé : seulement ses demandes encore en attente. */
    private void verifierModifiable(Authentication auth, DemandeConge demande) {
        if (utilisateurCourant.estRH(auth)) return;
        utilisateurCourant.verifierAcces(auth, demande.getEmploye().getId());
        if (demande.getStatut() != StatutDemande.EN_ATTENTE) {
            throw new AccessDeniedException("Une demande déjà traitée ne peut plus être modifiée");
        }
    }

    private void remplirListes(Model model) {
        model.addAttribute("employes", employeService.findAll());
        model.addAttribute("types", typeCongeService.findAll());
    }
}
