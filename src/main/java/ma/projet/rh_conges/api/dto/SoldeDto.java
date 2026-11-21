package ma.projet.rh_conges.api.dto;

import ma.projet.rh_conges.service.SoldeConge;

public record SoldeDto(Long typeCongeId, String typeConge, boolean calculLegal,
                       int droits, int report, int pris, int enAttente, int restants) {

    public static SoldeDto from(SoldeConge s) {
        return new SoldeDto(s.type().getId(), s.type().getLibelle(), s.type().isCalculLegal(),
                s.quota(), s.report(), s.consommes(), s.enAttente(), s.restants());
    }
}
