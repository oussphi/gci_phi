-- Adds total_avant_modification and total_apres_modification to commande
ALTER TABLE commande
    ADD COLUMN IF NOT EXISTS total_avant_modification DOUBLE NULL,
    ADD COLUMN IF NOT EXISTS total_apres_modification DOUBLE NULL;

