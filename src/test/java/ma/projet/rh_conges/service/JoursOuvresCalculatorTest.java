package ma.projet.rh_conges.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class JoursOuvresCalculatorTest {

    private final JoursOuvresCalculator calc = new JoursOuvresCalculator("");

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    @Test
    void semaineComplete_compteCinqJours() {
        assertThat(calc.compter(d("2026-11-23"), d("2026-11-29"))).isEqualTo(5);
    }

    @Test
    void weekEndSeul_compteZero() {
        assertThat(calc.compter(d("2026-11-21"), d("2026-11-22"))).isZero();
    }

    @Test
    void feteDeLIndependance_exclue() {
        // du lundi 16 au vendredi 20 novembre 2026, le mercredi 18 est férié
        assertThat(calc.compter(d("2026-11-16"), d("2026-11-20"))).isEqualTo(4);
    }

    @Test
    void feteDuTravail_exclue() {
        assertThat(calc.compter(d("2026-04-27"), d("2026-05-01"))).isEqualTo(4);
    }

    @Test
    void feteDeLUnite_ferieeSeulementDepuis2026() {
        assertThat(calc.estFerie(d("2026-10-31"))).isTrue();
        assertThat(calc.estFerie(d("2025-10-31"))).isFalse();
        assertThat(calc.compter(d("2025-10-31"), d("2025-10-31"))).isEqualTo(1);
    }

    @Test
    void nouvelAnAmazigh_ferieDepuis2024() {
        assertThat(calc.estFerie(d("2025-01-14"))).isTrue();
        assertThat(calc.estFerie(d("2023-01-14"))).isFalse();
    }

    @Test
    void feteReligieuseDeclareeEnConfiguration_exclue() {
        JoursOuvresCalculator avecAid = new JoursOuvresCalculator("2026-03-20, 2026-03-21");
        assertThat(avecAid.compter(d("2026-03-20"), d("2026-03-20"))).isZero();
        assertThat(calc.compter(d("2026-03-20"), d("2026-03-20"))).isEqualTo(1);
    }

    @Test
    void periodeInverseeOuIncomplete_compteZero() {
        assertThat(calc.compter(d("2026-11-27"), d("2026-11-23"))).isZero();
        assertThat(calc.compter(null, d("2026-11-23"))).isZero();
    }

    @Test
    void enJoursOuvrables_leSamediCompte() {
        JoursOuvresCalculator ouvrables = new JoursOuvresCalculator("", true);
        assertThat(ouvrables.compter(d("2026-11-23"), d("2026-11-29"))).isEqualTo(6);
    }

    @Test
    void finDAnnee_nouvelAnExclu() {
        // lundi 28 décembre 2026 au vendredi 1er janvier 2027 : le 1er janvier est férié
        assertThat(calc.compter(d("2026-12-28"), d("2027-01-01"))).isEqualTo(4);
    }
}
