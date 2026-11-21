package ma.projet.rh_conges.exception;

/** La demande ferait dépasser le quota annuel du type de congé. */
public class QuotaDepasseException extends DemandeCongeException {

    private final int joursDemandes;
    private final int joursDisponibles;

    public QuotaDepasseException(String typeConge, int joursDemandes, int joursDisponibles) {
        super("Quota dépassé pour « " + typeConge + " » : " + joursDemandes
                + " jour(s) ouvré(s) demandé(s), " + joursDisponibles + " disponible(s).");
        this.joursDemandes = joursDemandes;
        this.joursDisponibles = joursDisponibles;
    }

    public int getJoursDemandes() {
        return joursDemandes;
    }

    public int getJoursDisponibles() {
        return joursDisponibles;
    }
}
