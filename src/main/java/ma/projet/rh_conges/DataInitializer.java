package ma.projet.rh_conges;

import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.entity.*;
import ma.projet.rh_conges.repository.*;
import ma.projet.rh_conges.service.JoursOuvresCalculator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

/** Données de démonstration, chargées uniquement avec le profil dev (H2 en mémoire). */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final EmployeRepository employeRepo;
    private final TypeCongeRepository typeCongeRepo;
    private final DemandeCongeRepository demandeRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final JoursOuvresCalculator joursOuvres;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // ---- Types de congé ----
        TypeConge ca  = typeCongeRepo.save(TypeConge.builder().libelle("Congé Annuel").quotaAnnuel(30).build());
        TypeConge mal = typeCongeRepo.save(TypeConge.builder().libelle("Congé Maladie").quotaAnnuel(15).build());
        TypeConge mat = typeCongeRepo.save(TypeConge.builder().libelle("Congé Maternité").quotaAnnuel(98).build());
        TypeConge exc = typeCongeRepo.save(TypeConge.builder().libelle("Congé Exceptionnel").quotaAnnuel(5).build());

        // ---- Employés ----
        Employe e1 = employeRepo.save(Employe.builder()
                .nom("Karim Benali").departement("Informatique")
                .dateEmbauche(LocalDate.of(2019, 3, 15)).build());

        Employe e2 = employeRepo.save(Employe.builder()
                .nom("Fatima Zahra El Amrani").departement("RH")
                .dateEmbauche(LocalDate.of(2021, 6, 1)).build());

        Employe e3 = employeRepo.save(Employe.builder()
                .nom("Youssef Tazi").departement("Informatique")
                .dateEmbauche(LocalDate.of(2020, 9, 10)).build());

        Employe e4 = employeRepo.save(Employe.builder()
                .nom("Houda Chraibi").departement("Finance")
                .dateEmbauche(LocalDate.of(2018, 1, 20)).build());

        Employe e5 = employeRepo.save(Employe.builder()
                .nom("Mohamed Alaoui").departement("Marketing")
                .dateEmbauche(LocalDate.of(2022, 4, 5)).build());

        // ---- Demandes de congé ----
        demandeRepo.save(avecJours(DemandeConge.builder()
                .employe(e1).typeConge(ca)
                .dateDebut(LocalDate.of(2025, 7, 1)).dateFin(LocalDate.of(2025, 7, 15))
                .motif("Vacances d'été en famille").statut(StatutDemande.ACCEPTE).build()));

        demandeRepo.save(avecJours(DemandeConge.builder()
                .employe(e1).typeConge(exc)
                .dateDebut(LocalDate.of(2025, 10, 2)).dateFin(LocalDate.of(2025, 10, 4))
                .motif("Mariage d'un proche").statut(StatutDemande.ACCEPTE).build()));

        demandeRepo.save(avecJours(DemandeConge.builder()
                .employe(e2).typeConge(mal)
                .dateDebut(LocalDate.of(2025, 3, 10)).dateFin(LocalDate.of(2025, 3, 17))
                .motif("Grippe").statut(StatutDemande.ACCEPTE).build()));

        demandeRepo.save(avecJours(DemandeConge.builder()
                .employe(e3).typeConge(ca)
                .dateDebut(LocalDate.of(2025, 8, 5)).dateFin(LocalDate.of(2025, 8, 20))
                .motif("Voyage Marrakech").statut(StatutDemande.REFUSE).build()));

        demandeRepo.save(avecJours(DemandeConge.builder()
                .employe(e4).typeConge(ca)
                .dateDebut(LocalDate.now().plusDays(5)).dateFin(LocalDate.now().plusDays(15))
                .motif("Repos").statut(StatutDemande.EN_ATTENTE).build()));

        demandeRepo.save(avecJours(DemandeConge.builder()
                .employe(e5).typeConge(mal)
                .dateDebut(LocalDate.now().plusDays(1)).dateFin(LocalDate.now().plusDays(3))
                .motif("Consultation médicale").statut(StatutDemande.EN_ATTENTE).build()));

        demandeRepo.save(avecJours(DemandeConge.builder()
                .employe(e2).typeConge(ca)
                .dateDebut(LocalDate.of(2025, 12, 23)).dateFin(LocalDate.of(2026, 1, 3))
                .motif("Fêtes de fin d'année").statut(StatutDemande.ACCEPTE).build()));

        demandeRepo.save(avecJours(DemandeConge.builder()
                .employe(e3).typeConge(exc)
                .dateDebut(LocalDate.now().plusDays(10)).dateFin(LocalDate.now().plusDays(12))
                .motif("Décès d'un parent").statut(StatutDemande.EN_ATTENTE).build()));

        // ---- Comptes de démonstration (mot de passe : identifiant + "123") ----
        compte("rh", Role.RH, null);
        compte("karim", Role.EMPLOYE, e1);
        compte("fatima", Role.EMPLOYE, e2);
        compte("youssef", Role.EMPLOYE, e3);
        compte("houda", Role.EMPLOYE, e4);
        compte("mohamed", Role.EMPLOYE, e5);

        System.out.println("✅ Données de test chargées avec succès !");
    }

    private DemandeConge avecJours(DemandeConge d) {
        d.setNombreJours(joursOuvres.compter(d.getDateDebut(), d.getDateFin()));
        return d;
    }

    private void compte(String username, Role role, Employe employe) {
        utilisateurRepo.save(Utilisateur.builder()
                .username(username)
                .password(passwordEncoder.encode(username + "123"))
                .role(role)
                .employe(employe)
                .build());
    }
}
