package ma.projet.rh_conges.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ma.projet.rh_conges.entity.DemandeConge;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "employes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false)
    private String nom;

    @NotBlank(message = "Le département est obligatoire")
    @Column(nullable = false)
    private String departement;

    @NotNull(message = "La date d'embauche est obligatoire")
    @Column(nullable = false)
    private LocalDate dateEmbauche;

    @OneToMany(mappedBy = "employe", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<DemandeConge> demandes;
}
