package ma.projet.rh_conges.service;

import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.entity.DemandeConge;
import ma.projet.rh_conges.entity.StatutDemande;
import ma.projet.rh_conges.repository.DemandeCongeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class DemandeCongeService {

    private final DemandeCongeRepository demandeCongeRepository;

    public List<DemandeConge> findAll() {
        return demandeCongeRepository.findAllWithDetails();
    }

    public DemandeConge findById(Long id) {
        return demandeCongeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande introuvable avec l'id : " + id));
    }

    public DemandeConge soumettreDemande(DemandeConge demande) {
        demande.setStatut(StatutDemande.EN_ATTENTE);
        return demandeCongeRepository.save(demande);
    }

    public DemandeConge valider(Long id) {
        DemandeConge demande = findById(id);
        demande.setStatut(StatutDemande.ACCEPTE);
        return demandeCongeRepository.save(demande);
    }

    public DemandeConge rejeter(Long id) {
        DemandeConge demande = findById(id);
        demande.setStatut(StatutDemande.REFUSE);
        return demandeCongeRepository.save(demande);
    }

    public void deleteById(Long id) {
        demandeCongeRepository.deleteById(id);
    }

    public List<DemandeConge> findWithFilters(StatutDemande statut, String departement,
                                              Long typeCongeId, LocalDate dateDebut, LocalDate dateFin) {
        return demandeCongeRepository.findWithFilters(statut, departement, typeCongeId, dateDebut, dateFin);
    }

    public int getJoursConsommes(Long employeId, Long typeCongeId, int annee) {
        List<DemandeConge> demandes = demandeCongeRepository.findByEmployeId(employeId);
        int sum = 0;
        for (DemandeConge d : demandes) {
            if (d.getTypeConge() == null) continue;
            if (typeCongeId != null && !typeCongeId.equals(d.getTypeConge().getId())) continue;
            if (d.getStatut() != StatutDemande.ACCEPTE) continue;
            if (d.getDateDebut() == null) continue;
            if (d.getDateDebut().getYear() != annee) continue;
            sum += (int) d.getNombreJours();
        }
        return sum;
    }

    // ---- Statistiques ----

    public Map<String, Long> getJoursParDepartement() {
        Map<String, Long> result = new LinkedHashMap<>();
        List<DemandeConge> all = demandeCongeRepository.findAllWithDetails();
        Map<String, Long> accum = new TreeMap<>();
        for (DemandeConge d : all) {
            if (d.getStatut() == StatutDemande.ACCEPTE && d.getEmploye() != null) {
                String dep = d.getEmploye().getDepartement();
                long days = d.getNombreJours();
                accum.put(dep, accum.getOrDefault(dep, 0L) + days);
            }
        }
        result.putAll(accum);
        return result;
    }

    public Map<String, Object> getStatistiquesGlobales() {
        List<DemandeConge> all = demandeCongeRepository.findAllWithDetails();
        long total = all.size();
        long acceptes = 0, refuses = 0, attente = 0;
        for (DemandeConge d : all) {
            if (d.getStatut() == StatutDemande.ACCEPTE) acceptes++;
            else if (d.getStatut() == StatutDemande.REFUSE) refuses++;
            else if (d.getStatut() == StatutDemande.EN_ATTENTE) attente++;
        }
        double taux = total > 0 ? Math.round((double) acceptes / total * 1000) / 10.0 : 0.0;
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("acceptes", acceptes);
        stats.put("refuses", refuses);
        stats.put("attente", attente);
        stats.put("tauxAcceptation", taux);
        return stats;
    }

    public List<Map<String, Object>> getStatistiquesParType() {
        List<DemandeConge> all = demandeCongeRepository.findAllWithDetails();
        Map<String, long[]> tmp = new LinkedHashMap<>(); // libelle -> [total, acceptes]
        for (DemandeConge d : all) {
            if (d.getTypeConge() == null) continue;
            String lib = d.getTypeConge().getLibelle();
            tmp.putIfAbsent(lib, new long[]{0L, 0L});
            long[] arr = tmp.get(lib);
            arr[0]++;
            if (d.getStatut() == StatutDemande.ACCEPTE) arr[1]++;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, long[]> e : tmp.entrySet()) {
            long total = e.getValue()[0];
            long acceptes = e.getValue()[1];
            double taux = total > 0 ? Math.round((double) acceptes / total * 1000) / 10.0 : 0.0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("libelle", e.getKey());
            m.put("total", total);
            m.put("acceptes", acceptes);
            m.put("taux", taux);
            result.add(m);
        }
        return result;
    }
}
