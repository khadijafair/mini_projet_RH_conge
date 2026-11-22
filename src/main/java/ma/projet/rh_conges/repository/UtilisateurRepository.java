package ma.projet.rh_conges.repository;

import ma.projet.rh_conges.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmployeId(Long employeId);
}
