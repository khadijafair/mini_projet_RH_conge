package ma.projet.rh_conges.entity;

public enum StatutDemande {
    EN_ATTENTE("En attente"),
    ACCEPTE("Accepté"),
    REFUSE("Refusé");

    private final String libelle;

    StatutDemande(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
