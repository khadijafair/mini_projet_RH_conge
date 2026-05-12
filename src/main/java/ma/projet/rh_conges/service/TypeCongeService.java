package ma.projet.rh_conges.service;

import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.entity.TypeConge;
import ma.projet.rh_conges.repository.TypeCongeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TypeCongeService {

    private final TypeCongeRepository typeCongeRepository;

    public List<TypeConge> findAll() {
        return typeCongeRepository.findAll();
    }

    public TypeConge findById(Long id) {
        return typeCongeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Type de congé introuvable avec l'id : " + id));
    }

    public TypeConge save(TypeConge typeConge) {
        return typeCongeRepository.save(typeConge);
    }

    public void deleteById(Long id) {
        typeCongeRepository.deleteById(id);
    }
}
