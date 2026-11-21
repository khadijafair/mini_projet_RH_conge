package ma.projet.rh_conges.controller;

import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.entity.Employe;
import ma.projet.rh_conges.security.UtilisateurCourant;
import ma.projet.rh_conges.service.DemandeCongeService;
import ma.projet.rh_conges.service.EmployeService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/** Solde de congés par type : quota, jours pris, en attente, restants. */
@Controller
@RequestMapping("/soldes")
@RequiredArgsConstructor
public class SoldeController {

    private final DemandeCongeService demandeCongeService;
    private final EmployeService employeService;
    private final UtilisateurCourant utilisateurCourant;

    @GetMapping
    public String monSolde(@RequestParam(required = false) Integer annee, Authentication auth, Model model) {
        Employe moi = utilisateurCourant.employe(auth);
        if (moi == null) {
            if (utilisateurCourant.estRH(auth)) return "redirect:/employes";
            throw new AccessDeniedException("Aucune fiche employé liée à ce compte");
        }
        return afficher(moi, annee, model);
    }

    @GetMapping("/{employeId}")
    public String soldeEmploye(@PathVariable Long employeId, @RequestParam(required = false) Integer annee,
                               Authentication auth, Model model) {
        utilisateurCourant.verifierAcces(auth, employeId);
        return afficher(employeService.findById(employeId), annee, model);
    }

    private String afficher(Employe employe, Integer annee, Model model) {
        int an = annee != null ? annee : LocalDate.now().getYear();
        model.addAttribute("employe", employe);
        model.addAttribute("annee", an);
        model.addAttribute("soldes", demandeCongeService.getSoldes(employe.getId(), an));
        return "demande/solde";
    }
}
