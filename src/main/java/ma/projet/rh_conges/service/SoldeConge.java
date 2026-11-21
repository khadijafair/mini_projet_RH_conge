package ma.projet.rh_conges.service;

import ma.projet.rh_conges.entity.TypeConge;

/**
 * Solde d'un employé pour un type de congé et une année, en jours ouvrés.
 * {@code quota} inclut le {@code report} des jours non pris l'année précédente (congé annuel légal).
 */
public record SoldeConge(TypeConge type, int quota, int report, int consommes, int enAttente, int restants) {
}
