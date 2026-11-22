package ma.projet.rh_conges.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "utilisateurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Mot de passe haché avec BCrypt. */
    @Column(nullable = false)
    @ToString.Exclude
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Employé correspondant (absent pour un compte RH sans fiche employé). */
    @OneToOne
    @JoinColumn(name = "employe_id", unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Employe employe;
}
