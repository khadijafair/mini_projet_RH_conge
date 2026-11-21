package ma.projet.rh_conges.controller;

import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.security.UtilisateurCourant;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Expose l'utilisateur connecté et son rôle à toutes les vues. */
@ControllerAdvice
@RequiredArgsConstructor
public class UtilisateurCourantAdvice {

    private final UtilisateurCourant utilisateurCourant;

    @ModelAttribute
    public void ajouterUtilisateur(Model model, Authentication auth) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken) return;
        model.addAttribute("nomUtilisateur", auth.getName());
        model.addAttribute("estRH", utilisateurCourant.estRH(auth));
        model.addAttribute("employeConnecte", utilisateurCourant.employe(auth));
    }
}
