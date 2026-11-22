package ma.projet.rh_conges.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

/**
 * Droits au congé annuel payé selon le Code du travail marocain (loi 65-99).
 * <ul>
 *   <li>art. 231 : 1,5 jour par mois de service, ouvert après 6 mois de service continu ;</li>
 *   <li>art. 232 : +1,5 jour par période entière de 5 ans de service, total plafonné à 30 jours ;</li>
 *   <li>art. 240 : les jours non pris peuvent être reportés sur l'année suivante (deux années consécutives).</li>
 * </ul>
 * Les droits d'une année sont calculés au prorata des mois de service dans l'année civile,
 * puis arrondis au jour inférieur.
 */
@Component
public class DroitsCongeCalculator {

    static final double JOURS_PAR_MOIS = 1.5;
    static final double BONUS_PAR_TRANCHE_DE_5_ANS = 1.5;
    static final int PLAFOND = 30;
    static final int MOIS_AVANT_OUVERTURE = 6;

    /** Mois entiers de service accomplis dans l'année civile (0 à 12). */
    public int moisDeService(LocalDate embauche, int annee) {
        LocalDate debutAnnee = LocalDate.of(annee, 1, 1);
        LocalDate finAnnee = LocalDate.of(annee + 1, 1, 1);
        if (embauche == null || !embauche.isBefore(finAnnee)) return 0;
        LocalDate debut = embauche.isAfter(debutAnnee) ? embauche : debutAnnee;
        return (int) Math.min(12, ChronoUnit.MONTHS.between(debut, finAnnee));
    }

    /** Années entières de service à une date donnée. */
    public int anneesDeService(LocalDate embauche, LocalDate date) {
        if (embauche == null || date.isBefore(embauche)) return 0;
        return Period.between(embauche, date).getYears();
    }

    /** Droits de l'année (jours), ancienneté appréciée au 31 décembre. */
    public int droitsAnnuels(LocalDate embauche, int annee) {
        int mois = moisDeService(embauche, annee);
        if (mois == 0) return 0;
        double base = JOURS_PAR_MOIS * mois;
        int tranches = anneesDeService(embauche, LocalDate.of(annee, 12, 31)) / 5;
        double total = Math.min(base + BONUS_PAR_TRANCHE_DE_5_ANS * tranches, Math.max(base, PLAFOND));
        return (int) Math.floor(total);
    }

    /** Le droit au congé annuel est ouvert après 6 mois de service continu (art. 231). */
    public boolean droitOuvert(LocalDate embauche, LocalDate date) {
        return embauche != null && !date.isBefore(embauche.plusMonths(MOIS_AVANT_OUVERTURE));
    }
}
