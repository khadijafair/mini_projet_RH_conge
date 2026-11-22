package ma.projet.rh_conges.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compte les jours ouvrés d'une période : hors samedi, dimanche et jours fériés marocains.
 * <p>
 * Les fêtes nationales à date fixe sont codées ici. Les fêtes religieuses (Aïd al-Fitr, Aïd al-Adha,
 * 1er Moharram, Aïd al-Mawlid) dépendent du calendrier hégirien : leurs dates se déclarent dans
 * la propriété {@code rh.jours-feries-mobiles} (liste de dates ISO séparées par des virgules).
 */
@Component
public class JoursOuvresCalculator {

    /** Fêtes nationales à date fixe (décret n° 2.77.169 modifié). */
    private static final Set<MonthDay> FERIES_FIXES = Set.of(
            MonthDay.of(1, 1),    // Nouvel An
            MonthDay.of(1, 11),   // Manifeste de l'Indépendance
            MonthDay.of(5, 1),    // Fête du Travail
            MonthDay.of(7, 30),   // Fête du Trône
            MonthDay.of(8, 14),   // Journée de Oued Ed-Dahab
            MonthDay.of(8, 20),   // Révolution du Roi et du Peuple
            MonthDay.of(8, 21),   // Fête de la Jeunesse
            MonthDay.of(11, 6),   // Marche Verte
            MonthDay.of(11, 18)   // Fête de l'Indépendance
    );

    private static final MonthDay NOUVEL_AN_AMAZIGH = MonthDay.of(1, 14); // férié depuis 2024
    private static final MonthDay FETE_DE_L_UNITE = MonthDay.of(10, 31);  // fériée depuis 2026

    private final Set<LocalDate> feriesMobiles;
    private final boolean samediOuvrable;

    public JoursOuvresCalculator(String feriesMobiles) {
        this(feriesMobiles, false);
    }

    /**
     * @param samediOuvrable vrai pour compter en jours ouvrables (lundi au samedi), comme le Code du travail ;
     *                       faux (défaut) pour compter en jours ouvrés (lundi au vendredi).
     */
    @Autowired
    public JoursOuvresCalculator(@Value("${rh.jours-feries-mobiles:}") String feriesMobiles,
                                 @Value("${rh.samedi-ouvrable:false}") boolean samediOuvrable) {
        this.samediOuvrable = samediOuvrable;
        if (feriesMobiles == null || feriesMobiles.isBlank()) {
            this.feriesMobiles = Collections.emptySet();
        } else {
            this.feriesMobiles = Arrays.stream(feriesMobiles.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(LocalDate::parse)
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    public boolean estFerie(LocalDate date) {
        MonthDay jour = MonthDay.from(date);
        if (FERIES_FIXES.contains(jour)) return true;
        if (jour.equals(NOUVEL_AN_AMAZIGH) && date.getYear() >= 2024) return true;
        if (jour.equals(FETE_DE_L_UNITE) && date.getYear() >= 2026) return true;
        return feriesMobiles.contains(date);
    }

    public boolean estOuvre(LocalDate date) {
        DayOfWeek jour = date.getDayOfWeek();
        if (jour == DayOfWeek.SUNDAY) return false;
        if (jour == DayOfWeek.SATURDAY && !samediOuvrable) return false;
        return !estFerie(date);
    }

    /** Nombre de jours ouvrés entre deux dates incluses ; 0 si la période est vide ou inversée. */
    public int compter(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null || fin.isBefore(debut)) return 0;
        int jours = 0;
        for (LocalDate d = debut; !d.isAfter(fin); d = d.plusDays(1)) {
            if (estOuvre(d)) jours++;
        }
        return jours;
    }
}
