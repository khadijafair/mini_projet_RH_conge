package ma.projet.rh_conges.repository;

import ma.projet.rh_conges.entity.DemandeConge;
import ma.projet.rh_conges.entity.Employe;
import ma.projet.rh_conges.entity.StatutDemande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Long> {

    // Simple filters
    List<DemandeConge> findByStatut(StatutDemande statut);

    List<DemandeConge> findByEmploye(Employe employe);

    List<DemandeConge> findByEmployeId(Long employeId);

    // Combined filters with fetch to avoid N+1
    @Query("SELECT d FROM DemandeConge d JOIN FETCH d.employe e JOIN FETCH d.typeConge t WHERE (:statut IS NULL OR d.statut = :statut) AND (:departement IS NULL OR e.departement = :departement) AND (:typeCongeId IS NULL OR t.id = :typeCongeId) AND (:dateDebut IS NULL OR d.dateDebut >= :dateDebut) AND (:dateFin IS NULL OR d.dateFin <= :dateFin) ORDER BY d.dateDebut DESC")
    List<DemandeConge> findWithFilters(
            @Param("statut") StatutDemande statut,
            @Param("departement") String departement,
            @Param("typeCongeId") Long typeCongeId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    @Query("SELECT d FROM DemandeConge d JOIN FETCH d.employe JOIN FETCH d.typeConge ORDER BY d.dateDebut DESC")
    List<DemandeConge> findAllWithDetails();
}
