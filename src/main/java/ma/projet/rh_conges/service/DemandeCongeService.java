package ma.projet.rh_conges.service;

import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.entity.DemandeConge;
import ma.projet.rh_conges.entity.Employe;
import ma.projet.rh_conges.entity.StatutDemande;
import ma.projet.rh_conges.entity.TypeConge;
import ma.projet.rh_conges.exception.DemandeCongeException;
import ma.projet.rh_conges.exception.QuotaDepasseException;
import ma.projet.rh_conges.repository.DemandeCongeRepository;
import ma.projet.rh_conges.repository.EmployeRepository;
import ma.projet.rh_conges.repository.TypeCongeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class DemandeCongeService {

    private final DemandeCongeRepository demandeCongeRepository;
    private final TypeCongeRepository typeCongeRepository;
    private final EmployeRepository employeRepository;
    private final JoursOuvresCalculator joursOuvres;

    public List<DemandeConge> findAll() {
        return demandeCongeRepository.findAllWithDetails();
    }

    public DemandeConge findById(Long id) {
        return demandeCongeRepository.findById(id)
                .orElseThrow(() -> new DemandeCongeException("Demande introuvable avec l'id : " + id));
    }

    /**
     * Soumet (ou modifie) une demande : contrôle les dates, le chevauchement avec les autres demandes
     * de l'employé et le quota annuel (jours acceptés + jours en attente), puis la met en attente.
     */
    public DemandeConge soumettreDemande(DemandeConge demande) {
        LocalDate debut = demande.getDateDebut();
        LocalDate fin = demande.getDateFin();
        if (debut == null || fin == null) {
            throw new DemandeCongeException("Les dates de début et de fin sont obligatoires.");
        }
        if (fin.isBefore(debut)) {
            throw new DemandeCongeException("La date de fin doit être après la date de début.");
        }
        if (debut.getYear() != fin.getYear()) {
            throw new DemandeCongeException("Une demande doit rester dans la même année civile : faites-en deux.");
        }

        Employe employe = chargerEmploye(demande.getEmploye());
        TypeConge type = chargerType(demande.getTypeConge());

        int jours = joursOuvres.compter(debut, fin);
        if (jours == 0) {
            throw new DemandeCongeException("La période ne contient aucun jour ouvré (week-end ou jour férié).");
        }

        List<DemandeConge> autres = demandeCongeRepository.findByEmployeId(employe.getId()).stream()
                .filter(d -> !Objects.equals(d.getId(), demande.getId()))
                .toList();

        boolean chevauche = autres.stream()
                .filter(d -> d.getStatut() != StatutDemande.REFUSE)
                .anyMatch(d -> !d.getDateDebut().isAfter(fin) && !d.getDateFin().isBefore(debut));
        if (chevauche) {
            throw new DemandeCongeException("Cette période chevauche une autre demande de l'employé.");
        }

        int dejaReserves = somme(autres, type.getId(), debut.getYear(),
                EnumSet.of(StatutDemande.ACCEPTE, StatutDemande.EN_ATTENTE));
        if (dejaReserves + jours > type.getQuotaAnnuel()) {
            throw new QuotaDepasseException(type.getLibelle(), jours,
                    Math.max(0, type.getQuotaAnnuel() - dejaReserves));
        }

        demande.setEmploye(employe);
        demande.setTypeConge(type);
        demande.setNombreJours(jours);
        demande.setStatut(StatutDemande.EN_ATTENTE);
        return demandeCongeRepository.save(demande);
    }

    /** Valide une demande en attente, après avoir revérifié le quota avec les seuls jours déjà acceptés. */
    public DemandeConge valider(Long id) {
        DemandeConge demande = enAttente(id);
        TypeConge type = demande.getTypeConge();
        int annee = demande.getDateDebut().getYear();
        List<DemandeConge> autres = demandeCongeRepository.findByEmployeId(demande.getEmploye().getId()).stream()
                .filter(d -> !Objects.equals(d.getId(), id))
                .toList();
        int acceptes = somme(autres, type.getId(), annee, EnumSet.of(StatutDemande.ACCEPTE));
        if (acceptes + demande.getNombreJours() > type.getQuotaAnnuel()) {
            throw new QuotaDepasseException(type.getLibelle(), demande.getNombreJours(),
                    Math.max(0, type.getQuotaAnnuel() - acceptes));
        }
        demande.setStatut(StatutDemande.ACCEPTE);
        return demandeCongeRepository.save(demande);
    }

    public DemandeConge rejeter(Long id) {
        DemandeConge demande = enAttente(id);
        demande.setStatut(StatutDemande.REFUSE);
        return demandeCongeRepository.save(demande);
    }

    public void deleteById(Long id) {
        demandeCongeRepository.deleteById(id);
    }

    public List<DemandeConge> findWithFilters(Long employeId, StatutDemande statut, String departement,
                                              Long typeCongeId, LocalDate dateDebut, LocalDate dateFin) {
        return demandeCongeRepository.findWithFilters(employeId, statut, departement, typeCongeId, dateDebut, dateFin);
    }

    /** Jours ouvrés acceptés pour un employé, un type de congé et une année. */
    public int getJoursConsommes(Long employeId, Long typeCongeId, int annee) {
        return somme(demandeCongeRepository.findByEmployeId(employeId), typeCongeId, annee,
                EnumSet.of(StatutDemande.ACCEPTE));
    }

    /** Solde de l'employé pour chaque type de congé de l'année. */
    public List<SoldeConge> getSoldes(Long employeId, int annee) {
        List<DemandeConge> demandes = demandeCongeRepository.findByEmployeId(employeId);
        List<SoldeConge> soldes = new ArrayList<>();
        for (TypeConge type : typeCongeRepository.findAll()) {
            int consommes = somme(demandes, type.getId(), annee, EnumSet.of(StatutDemande.ACCEPTE));
            int enAttente = somme(demandes, type.getId(), annee, EnumSet.of(StatutDemande.EN_ATTENTE));
            int restants = Math.max(0, type.getQuotaAnnuel() - consommes - enAttente);
            soldes.add(new SoldeConge(type, type.getQuotaAnnuel(), consommes, enAttente, restants));
        }
        return soldes;
    }

    // ---- Statistiques ----

    public Map<String, Long> getJoursParDepartement() {
        Map<String, Long> accum = new TreeMap<>();
        for (DemandeConge d : demandeCongeRepository.findAllWithDetails()) {
            if (d.getStatut() == StatutDemande.ACCEPTE && d.getEmploye() != null) {
                accum.merge(d.getEmploye().getDepartement(), (long) d.getNombreJours(), Long::sum);
            }
        }
        return new LinkedHashMap<>(accum);
    }

    public Map<String, Object> getStatistiquesGlobales() {
        List<DemandeConge> all = demandeCongeRepository.findAllWithDetails();
        long total = all.size();
        long acceptes = 0, refuses = 0, attente = 0;
        for (DemandeConge d : all) {
            switch (d.getStatut()) {
                case ACCEPTE -> acceptes++;
                case REFUSE -> refuses++;
                case EN_ATTENTE -> attente++;
            }
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("acceptes", acceptes);
        stats.put("refuses", refuses);
        stats.put("attente", attente);
        stats.put("tauxAcceptation", taux(acceptes, total));
        return stats;
    }

    public List<Map<String, Object>> getStatistiquesParType() {
        Map<String, long[]> tmp = new LinkedHashMap<>(); // libellé -> [total, acceptées]
        for (DemandeConge d : demandeCongeRepository.findAllWithDetails()) {
            if (d.getTypeConge() == null) continue;
            long[] arr = tmp.computeIfAbsent(d.getTypeConge().getLibelle(), k -> new long[2]);
            arr[0]++;
            if (d.getStatut() == StatutDemande.ACCEPTE) arr[1]++;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, long[]> e : tmp.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("libelle", e.getKey());
            m.put("total", e.getValue()[0]);
            m.put("acceptes", e.getValue()[1]);
            m.put("taux", taux(e.getValue()[1], e.getValue()[0]));
            result.add(m);
        }
        return result;
    }

    // ---- Outils ----

    private DemandeConge enAttente(Long id) {
        DemandeConge demande = findById(id);
        if (demande.getStatut() != StatutDemande.EN_ATTENTE) {
            throw new DemandeCongeException("Cette demande a déjà été traitée.");
        }
        return demande;
    }

    private Employe chargerEmploye(Employe employe) {
        if (employe == null || employe.getId() == null) {
            throw new DemandeCongeException("L'employé est obligatoire.");
        }
        return employeRepository.findById(employe.getId())
                .orElseThrow(() -> new DemandeCongeException("Employé introuvable."));
    }

    private TypeConge chargerType(TypeConge type) {
        if (type == null || type.getId() == null) {
            throw new DemandeCongeException("Le type de congé est obligatoire.");
        }
        return typeCongeRepository.findById(type.getId())
                .orElseThrow(() -> new DemandeCongeException("Type de congé introuvable."));
    }

    private static int somme(List<DemandeConge> demandes, Long typeCongeId, int annee, Set<StatutDemande> statuts) {
        int total = 0;
        for (DemandeConge d : demandes) {
            if (d.getTypeConge() == null || d.getDateDebut() == null) continue;
            if (typeCongeId != null && !typeCongeId.equals(d.getTypeConge().getId())) continue;
            if (!statuts.contains(d.getStatut())) continue;
            if (d.getDateDebut().getYear() != annee) continue;
            total += d.getNombreJours();
        }
        return total;
    }

    private static double taux(long part, long total) {
        return total > 0 ? Math.round((double) part / total * 1000) / 10.0 : 0.0;
    }
}
