package ma.projet.rh_conges.api.dto;

import ma.projet.rh_conges.entity.DemandeConge;
import ma.projet.rh_conges.entity.StatutDemande;

import java.time.LocalDate;

/** Demande de congé exposée par l'API (jamais l'entité JPA directement). */
public record DemandeDto(
        Long id,
        Long employeId,
        String employe,
        String departement,
        Long typeCongeId,
        String typeConge,
        LocalDate dateDebut,
        LocalDate dateFin,
        int nombreJours,
        StatutDemande statut,
        String motif) {

    public static DemandeDto from(DemandeConge d) {
        return new DemandeDto(d.getId(),
                d.getEmploye().getId(), d.getEmploye().getNom(), d.getEmploye().getDepartement(),
                d.getTypeConge().getId(), d.getTypeConge().getLibelle(),
                d.getDateDebut(), d.getDateFin(), d.getNombreJours(), d.getStatut(), d.getMotif());
    }
}
