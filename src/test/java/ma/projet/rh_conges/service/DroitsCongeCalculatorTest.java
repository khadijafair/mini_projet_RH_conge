package ma.projet.rh_conges.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DroitsCongeCalculatorTest {

    private final DroitsCongeCalculator calc = new DroitsCongeCalculator();

    @Test
    void anneeComplete_dixHuitJours() {
        assertThat(calc.droitsAnnuels(LocalDate.of(2024, 1, 1), 2025)).isEqualTo(18);
    }

    @Test
    void embaucheEnCoursDAnnee_auProrata() {
        // embauché le 1er juillet : 6 mois -> 9 jours
        assertThat(calc.moisDeService(LocalDate.of(2026, 7, 1), 2026)).isEqualTo(6);
        assertThat(calc.droitsAnnuels(LocalDate.of(2026, 7, 1), 2026)).isEqualTo(9);
        // le 15 juillet : 5 mois entiers -> 7,5 arrondi à 7
        assertThat(calc.droitsAnnuels(LocalDate.of(2026, 7, 15), 2026)).isEqualTo(7);
    }

    @Test
    void pasEncoreEmbauche_aucunDroit() {
        assertThat(calc.droitsAnnuels(LocalDate.of(2027, 2, 1), 2026)).isZero();
        assertThat(calc.droitsAnnuels(null, 2026)).isZero();
    }

    @Test
    void ancienneteCinqAns_unJourEtDemiDePlus() {
        // 5 ans au 31/12/2026 -> 18 + 1,5 = 19,5 arrondi à 19
        assertThat(calc.droitsAnnuels(LocalDate.of(2021, 6, 1), 2026)).isEqualTo(19);
        // 4 ans seulement -> 18
        assertThat(calc.droitsAnnuels(LocalDate.of(2022, 6, 1), 2026)).isEqualTo(18);
    }

    @Test
    void ancienneteDixAns_troisJoursDePlus() {
        assertThat(calc.droitsAnnuels(LocalDate.of(2016, 1, 1), 2026)).isEqualTo(21);
    }

    @Test
    void plafondDeTrenteJours() {
        // 40 ans de service : 18 + 8 x 1,5 = 30 ; 50 ans : plafonné à 30
        assertThat(calc.droitsAnnuels(LocalDate.of(1986, 1, 1), 2026)).isEqualTo(30);
        assertThat(calc.droitsAnnuels(LocalDate.of(1976, 1, 1), 2026)).isEqualTo(30);
    }

    @Test
    void droitOuvertApresSixMois() {
        LocalDate embauche = LocalDate.of(2026, 3, 10);
        assertThat(calc.droitOuvert(embauche, LocalDate.of(2026, 9, 9))).isFalse();
        assertThat(calc.droitOuvert(embauche, LocalDate.of(2026, 9, 10))).isTrue();
        assertThat(calc.droitOuvert(null, LocalDate.of(2026, 9, 10))).isFalse();
    }

    @Test
    void anneesDeService() {
        assertThat(calc.anneesDeService(LocalDate.of(2019, 3, 15), LocalDate.of(2026, 3, 14))).isEqualTo(6);
        assertThat(calc.anneesDeService(LocalDate.of(2019, 3, 15), LocalDate.of(2026, 3, 15))).isEqualTo(7);
        assertThat(calc.anneesDeService(LocalDate.of(2027, 1, 1), LocalDate.of(2026, 1, 1))).isZero();
    }
}
