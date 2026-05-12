package ma.projet.rh_conges.repository;

import ma.projet.rh_conges.entity.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Long> {

    List<Employe> findByDepartement(String departement);

    @Query("SELECT DISTINCT e.departement FROM Employe e ORDER BY e.departement")
    List<String> findAllDepartements();

    List<Employe> findByNomContainingIgnoreCase(String nom);
}
