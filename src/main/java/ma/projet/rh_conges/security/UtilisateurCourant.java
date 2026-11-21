package ma.projet.rh_conges.security;

import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.entity.Employe;
import ma.projet.rh_conges.entity.Utilisateur;
import ma.projet.rh_conges.repository.UtilisateurRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Accès à l'utilisateur connecté et à ses droits. */
@Component
@RequiredArgsConstructor
public class UtilisateurCourant {

    private final UtilisateurRepository utilisateurRepository;

    public boolean estRH(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_RH".equals(a.getAuthority()));
    }

    public Utilisateur utilisateur(Authentication auth) {
        if (auth == null) throw new AccessDeniedException("Non connecté");
        return utilisateurRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Compte inconnu"));
    }

    /** Fiche employé de l'utilisateur connecté, ou null (compte RH sans fiche). */
    public Employe employe(Authentication auth) {
        if (auth == null) return null;
        return utilisateurRepository.findByUsername(auth.getName())
                .map(Utilisateur::getEmploye)
                .orElse(null);
    }

    /** Un employé n'accède qu'à ses propres données ; le RH accède à tout. */
    public void verifierAcces(Authentication auth, Long employeId) {
        if (estRH(auth)) return;
        Employe moi = employe(auth);
        if (moi == null || employeId == null || !moi.getId().equals(employeId)) {
            throw new AccessDeniedException("Accès réservé à vos propres demandes");
        }
    }
}
