-- Ajout du statut_produit sur ligne_commande
ALTER TABLE ligne_commande
    ADD COLUMN IF NOT EXISTS statut_produit VARCHAR(32) NOT NULL DEFAULT 'INCLUSE';

