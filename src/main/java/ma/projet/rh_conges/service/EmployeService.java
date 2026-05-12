package ma.projet.rh_conges.service;

import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.entity.Employe;
import ma.projet.rh_conges.repository.EmployeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeService {

    private final EmployeRepository employeRepository;

    public List<Employe> findAll() {
        return employeRepository.findAll();
    }

    public Employe findById(Long id) {
        return employeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employé introuvable avec l'id : " + id));
    }

    public Employe save(Employe employe) {
        return employeRepository.save(employe);
    }

    public void deleteById(Long id) {
        employeRepository.deleteById(id);
    }

    public List<String> findAllDepartements() {
        return employeRepository.findAllDepartements();
    }

    public List<Employe> findByDepartement(String departement) {
        return employeRepository.findByDepartement(departement);
    }
}
