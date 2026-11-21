package ma.projet.rh_conges.controller;

import ma.projet.rh_conges.exception.DemandeCongeException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Une règle métier non traitée par le contrôleur ramène à la liste avec un message clair. */
@ControllerAdvice
public class ErreurMetierAdvice {

    @ExceptionHandler(DemandeCongeException.class)
    public String erreurMetier(DemandeCongeException e, RedirectAttributes ra) {
        ra.addFlashAttribute("errorMsg", e.getMessage());
        return "redirect:/demandes";
    }
}
