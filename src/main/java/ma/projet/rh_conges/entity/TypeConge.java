package ma.projet.rh_conges.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "types_conge")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le libellé est obligatoire")
    @Column(nullable = false, unique = true)
    private String libelle;

    @Min(value = 1, message = "Le quota doit être au moins 1 jour")
    @Column(nullable = false)
    private int quotaAnnuel;

    // Pas de cascade : un type utilisé par des demandes ne peut pas être supprimé.
    @OneToMany(mappedBy = "typeConge", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<DemandeConge> demandes;
}
