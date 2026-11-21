package ma.projet.rh_conges.api.dto;

import ma.projet.rh_conges.entity.TypeConge;

public record TypeCongeDto(Long id, String libelle, int quotaAnnuel, boolean calculLegal) {

    public static TypeCongeDto from(TypeConge t) {
        return new TypeCongeDto(t.getId(), t.getLibelle(), t.getQuotaAnnuel(), t.isCalculLegal());
    }
}
