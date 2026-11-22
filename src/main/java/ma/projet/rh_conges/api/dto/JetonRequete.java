package ma.projet.rh_conges.api.dto;

import jakarta.validation.constraints.NotBlank;

public record JetonRequete(@NotBlank String username, @NotBlank String password) {
}
