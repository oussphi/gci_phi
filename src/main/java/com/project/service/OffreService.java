package com.project.service;

import com.project.entity.Offre;

import com.project.entity.User;
import com.project.entity.OffreProduit;

import java.util.List;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OffreService {
    List<Offre> getAllOffres();
    Offre getOffreById(Long id);
    List<OffreProduit> getProduitsByOffre(Long offreId);
    double getRemisePourProduit(Long offreId, Long produitId);
    double getPrixProduitDansOffre(Long offreId, Long produitId);

    // AJOUTÉ :
    void enregistrerOffre(Offre offre);
    List<Offre> getOffresCreeesPar(User user);
    
    // Nouvelles méthodes pour la pagination et séparation par date
    Page<Offre> getActiveOffresByUser(User user, Pageable pageable);
    Page<Offre> getArchivedOffresByUser(User user, Pageable pageable);
    long getActiveOffresCount(User user);
    long getArchivedOffresCount(User user);

    // Activation / Désactivation
    void toggleActive(Long offreId, User owner, boolean active);
    
    // Méthode spécialisée pour l'édition DSM
    void updateOffreDSM(Long offreId, String nom, String description,
                        LocalDate dateDebut, LocalDate dateFin, boolean active,
                        List<OffreProduit> nouveauxProduits);

    // === Added for architecture alignment ===
    /** Accessibility rule for offers */
    boolean isOffreAccessibleForUser(Offre offre, User user, LocalDate today);

    /** Active offers filtered for a given user */
    List<Offre> findActiveOffresForUser(User user);

    /** Create offer with products, owned by given user */
    void createOffre(Offre offre, java.util.List<com.project.service.offer.OffreProduitInput> produitsPayload, User owner);
}
