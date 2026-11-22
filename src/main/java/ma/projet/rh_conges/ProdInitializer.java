package ma.projet.rh_conges;

import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.entity.Role;
import ma.projet.rh_conges.entity.Utilisateur;
import ma.projet.rh_conges.repository.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** En production, crée le premier compte RH à partir de la variable RH_ADMIN_PASSWORD. */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProdInitializer.class);

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${RH_ADMIN_PASSWORD:}")
    private String motDePasseAdmin;

    @Override
    public void run(String... args) {
        if (utilisateurRepository.count() > 0) return;
        if (motDePasseAdmin == null || motDePasseAdmin.length() < 8) {
            log.warn("Aucun compte et RH_ADMIN_PASSWORD absent ou trop court (8 caractères minimum) : compte RH non créé.");
            return;
        }
        utilisateurRepository.save(Utilisateur.builder()
                .username("rh")
                .password(passwordEncoder.encode(motDePasseAdmin))
                .role(Role.RH)
                .build());
        log.info("Compte RH initial « rh » créé.");
    }
}
