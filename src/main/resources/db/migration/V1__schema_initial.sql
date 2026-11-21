-- Schéma initial (compatible MySQL 8 et H2 en mode MySQL)

CREATE TABLE employes (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom           VARCHAR(255) NOT NULL,
    departement   VARCHAR(255) NOT NULL,
    date_embauche DATE         NOT NULL
);

CREATE TABLE types_conge (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    libelle      VARCHAR(255) NOT NULL,
    quota_annuel INT          NOT NULL,
    calcul_legal BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_types_conge_libelle UNIQUE (libelle)
);

CREATE TABLE demandes_conge (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    date_debut    DATE         NOT NULL,
    date_fin      DATE         NOT NULL,
    statut        VARCHAR(20)  NOT NULL,
    motif         VARCHAR(500),
    nombre_jours  INT          NOT NULL,
    employe_id    BIGINT       NOT NULL,
    type_conge_id BIGINT       NOT NULL,
    CONSTRAINT fk_demande_employe FOREIGN KEY (employe_id) REFERENCES employes (id),
    CONSTRAINT fk_demande_type FOREIGN KEY (type_conge_id) REFERENCES types_conge (id)
);

CREATE INDEX idx_demandes_employe_debut ON demandes_conge (employe_id, date_debut);

CREATE TABLE utilisateurs (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    employe_id BIGINT,
    CONSTRAINT uk_utilisateurs_username UNIQUE (username),
    CONSTRAINT uk_utilisateurs_employe UNIQUE (employe_id),
    CONSTRAINT fk_utilisateur_employe FOREIGN KEY (employe_id) REFERENCES employes (id)
);
