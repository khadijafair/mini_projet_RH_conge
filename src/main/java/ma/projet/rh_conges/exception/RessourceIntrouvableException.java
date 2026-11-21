package ma.projet.rh_conges.exception;

/** Ressource demandée inexistante (404 dans l'API). */
public class RessourceIntrouvableException extends DemandeCongeException {
    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
