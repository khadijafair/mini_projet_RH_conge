package ma.projet.rh_conges.service;

import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.entity.Employe;
import ma.projet.rh_conges.entity.Role;
import ma.projet.rh_conges.entity.Utilisateur;
import ma.projet.rh_conges.exception.DemandeCongeException;
import ma.projet.rh_conges.exception.RessourceIntrouvableException;
import ma.projet.rh_conges.repository.DemandeCongeRepository;
import ma.projet.rh_conges.repository.EmployeRepository;
import ma.projet.rh_conges.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeService {

    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Employe> findAll() {
        return employeRepository.findAll();
    }

    public Employe findById(Long id) {
        return employeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Employé introuvable avec l'id : " + id));
    }

    public Employe save(Employe employe) {
        return employeRepository.save(employe);
    }

    /** Enregistre un nouvel employé et son compte dans la même transaction. */
    public Employe creerAvecCompte(Employe employe, String username, String motDePasse) {
        verifierCompte(username, motDePasse);
        Employe enregistre = employeRepository.save(employe);
        creerCompte(enregistre, username, motDePasse);
        return enregistre;
    }

    /** Crée le compte de connexion (rôle EMPLOYE) d'un employé. */
    public Utilisateur creerCompte(Employe employe, String username, String motDePasse) {
        verifierCompte(username, motDePasse);
        if (utilisateurRepository.existsByEmployeId(employe.getId())) {
            throw new DemandeCongeException("Cet employé a déjà un compte.");
        }
        return utilisateurRepository.save(Utilisateur.builder()
                .username(username.trim())
                .password(passwordEncoder.encode(motDePasse))
                .role(Role.EMPLOYE)
                .employe(employe)
                .build());
    }

    private void verifierCompte(String username, String motDePasse) {
        if (username == null || username.isBlank() || motDePasse == null || motDePasse.length() < 8) {
            throw new DemandeCongeException("Identifiant obligatoire et mot de passe d'au moins 8 caractères.");
        }
        if (utilisateurRepository.existsByUsername(username.trim())) {
            throw new DemandeCongeException("Cet identifiant est déjà utilisé.");
        }
    }

    public void deleteById(Long id) {
        if (demandeCongeRepository.existsByEmployeId(id)) {
            throw new DemandeCongeException("Cet employé a des demandes de congé : historique conservé.");
        }
        utilisateurRepository.findAll().stream()
                .filter(u -> u.getEmploye() != null && id.equals(u.getEmploye().getId()))
                .forEach(utilisateurRepository::delete);
        employeRepository.deleteById(id);
    }

    public List<String> findAllDepartements() {
        return employeRepository.findAllDepartements();
    }

    public List<Employe> findByDepartement(String departement) {
        return employeRepository.findByDepartement(departement);
    }
}
