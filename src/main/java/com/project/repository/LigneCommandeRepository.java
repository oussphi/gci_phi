package com.project.repository;

import com.project.entity.LigneCommande;
import com.project.entity.Commande;
import com.project.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneCommandeRepository extends JpaRepository<LigneCommande, Long> {

    /**
     * Récupère toutes les lignes d'une commande donnée.
     * @param commande la commande concernée
     * @return liste des lignes associées à cette commande
     */
    List<LigneCommande> findByCommande(Commande commande);

    /**
     * Récupère toutes les lignes d'une commande par son ID.
     * @param commandeId l'ID de la commande
     * @return liste des lignes associées à l'ID de commande
     */
    List<LigneCommande> findByCommandeId(Long commandeId);

    /**
     * Vérifie s'il existe une ligne pour un produit spécifique dans une commande.
     * @param commande la commande
     * @param produit le produit
     * @return true si une ligne correspondante existe
     */
    boolean existsByCommandeAndProduit(Commande commande, Product produit);
}
