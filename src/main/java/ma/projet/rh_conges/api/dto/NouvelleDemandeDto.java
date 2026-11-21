package ma.projet.rh_conges.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Création d'une demande. {@code employeId} n'est pris en compte que pour le RH ;
 * un employé dépose toujours pour lui-même.
 */
public record NouvelleDemandeDto(
        @NotNull(message = "Le type de congé est obligatoire") Long typeCongeId,
        @NotNull(message = "La date de début est obligatoire") LocalDate dateDebut,
        @NotNull(message = "La date de fin est obligatoire") LocalDate dateFin,
        @Size(max = 500, message = "500 caractères maximum") String motif,
        Long employeId) {
}
