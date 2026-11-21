package ma.projet.rh_conges.service;

import ma.projet.rh_conges.entity.TypeConge;

/** Solde d'un employé pour un type de congé et une année, en jours ouvrés. */
public record SoldeConge(TypeConge type, int quota, int consommes, int enAttente, int restants) {
}
