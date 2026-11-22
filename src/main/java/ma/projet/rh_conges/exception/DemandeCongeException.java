package ma.projet.rh_conges.exception;

/** Règle métier non respectée par une demande de congé (dates, chevauchement, statut). */
public class DemandeCongeException extends RuntimeException {
    public DemandeCongeException(String message) {
        super(message);
    }
}
