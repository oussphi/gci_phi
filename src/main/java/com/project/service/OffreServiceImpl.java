package com.project.service;

import com.project.entity.Offre;
import com.project.entity.User;
import com.project.entity.OffreProduit;
import com.project.repository.OffreProduitRepository;
import com.project.repository.OffreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
// imports
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class OffreServiceImpl implements OffreService {

    @Autowired
    private OffreRepository offreRepository;

    @Autowired
    private OffreProduitRepository offreProduitRepository;

    @Override
    public List<Offre> getAllOffres() {
        return offreRepository.findAll();
    }

    @Override
    public Offre getOffreById(Long id) {
        return offreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offre introuvable avec l'ID : " + id));
    }

    @Override
    public List<OffreProduit> getProduitsByOffre(Long offreId) {
        return offreProduitRepository.findByOffreId(offreId).stream()
            .filter(p -> p.getProduit() != null)
            .toList();
    }

    @Override
    public double getRemisePourProduit(Long offreId, Long produitId) {
        return offreProduitRepository.findByOffreId(offreId).stream()
                .filter(p -> p.getProduit() != null && p.getProduit().getId().equals(produitId))
                .mapToDouble(OffreProduit::getRemisePourcent)
                .findFirst()
                .orElse(0.0);
    }

    @Override
    public double getPrixProduitDansOffre(Long offreId, Long produitId) {
        return offreProduitRepository.findByOffreId(offreId).stream()
                .filter(p -> p.getProduit() != null && p.getProduit().getId().equals(produitId))
                .mapToDouble(OffreProduit::getPrix)
                .findFirst()
                .orElse(0.0);
    }

    // ✅ MÉTHODE AJOUTÉE
    @Transactional
    @Override
    public void enregistrerOffre(Offre offre) {
        if (offre.getId() == null) {
            // === CREATION ===
            // Sécuriser l'information de créateur par username pour les contrôles d'accès fallback
            try {
                if (offre.getCreatedByUser() != null && offre.getCreatedByUser().getUsername() != null
                        && (offre.getCreatedByUsername() == null || offre.getCreatedByUsername().isBlank())) {
                    offre.setCreatedByUsername(offre.getCreatedByUser().getUsername());
                }
            } catch (Exception ignored) {}
            if (offre.getProduits() != null) {
                for (OffreProduit op : offre.getProduits()) {
                    op.setOffre(offre);
                }
            }
            offreRepository.save(offre);
            return;
        }

        // === MISE A JOUR ===
        // Récupérer l'entité MANAGEE par l'EntityManager
        Offre managed = offreRepository.findById(offre.getId())
                .orElseThrow(() -> new RuntimeException("Offre introuvable avec l'ID : " + offre.getId()));

        // Mettre à jour les champs simples
        managed.setNom(offre.getNom());
        managed.setDescription(offre.getDescription());
        managed.setDateDebut(offre.getDateDebut());
        managed.setDateFin(offre.getDateFin());
        managed.setActive(offre.isActive());
        managed.setGamme(offre.getGamme());

        // Mettre à jour les produits UNIQUEMENT si une nouvelle liste est fournie
        if (offre.getProduits() != null) {
            // IMPORTANT : garantir la suppression des anciennes lignes pour éviter tout résidu
            try {
                offreProduitRepository.deleteByOffreId(managed.getId());
            } catch (Exception ignored) {}
            // Puis reconstruire proprement la collection managée
            if (managed.getProduits() != null) {
                managed.getProduits().clear();
            }
            for (OffreProduit op : offre.getProduits()) {
                OffreProduit child = new OffreProduit();
                child.setOffre(managed);
                child.setProduit(op.getProduit());
                child.setPrix(op.getPrix());
                child.setRemisePourcent(op.getRemisePourcent());
                child.setQuantiteMin(op.getQuantiteMin());
                child.setQuantiteMax(op.getQuantiteMax());
                managed.getProduits().add(child);
            }
        }
        // Un save explicite n'est pas strictement nécessaire car managed est suivi, mais on le laisse pour clarté
        offreRepository.save(managed);
    }

    
    @Override
    public List<Offre> getOffresCreeesPar(User user) {
        return offreRepository.findByCreatedByUser(user);
    }
    
    // Nouvelles méthodes pour la pagination et séparation par date
    @Override
    public Page<Offre> getActiveOffresByUser(User user, Pageable pageable) {
        LocalDate today = LocalDate.now();
        String username = user != null ? user.getUsername() : null;
        return offreRepository.findActiveOffresByUser(user, username, today, pageable);
    }
    
    @Override
    public Page<Offre> getArchivedOffresByUser(User user, Pageable pageable) {
        LocalDate today = LocalDate.now();
        String username = user != null ? user.getUsername() : null;
        return offreRepository.findArchivedOffresByUser(user, username, today, pageable);
    }
    
    @Override
    public long getActiveOffresCount(User user) {
        LocalDate today = LocalDate.now();
        String username = user != null ? user.getUsername() : null;
        return offreRepository.countActiveOffresByUser(user, username, today);
    }
    
    @Override
    public long getArchivedOffresCount(User user) {
        LocalDate today = LocalDate.now();
        String username = user != null ? user.getUsername() : null;
        return offreRepository.countArchivedOffresByUser(user, username, today);
    }

    @Override
    public void toggleActive(Long offreId, User owner, boolean active) {
        Offre offre = getOffreById(offreId);
        boolean allowed = false;
        if (offre.getCreatedByUser() != null && owner != null &&
                offre.getCreatedByUser().getId() != null && offre.getCreatedByUser().getId().equals(owner.getId())) {
            allowed = true;
        }
        if (!allowed && owner != null && offre.getCreatedByUsername() != null && owner.getUsername() != null &&
                offre.getCreatedByUsername().equalsIgnoreCase(owner.getUsername())) {
            allowed = true;
        }
        if (!allowed) throw new RuntimeException("Accès refusé: cette offre n'appartient pas à l'utilisateur");
        offre.setActive(active);
        offreRepository.save(offre);
    }
    
    @Override
    @Transactional
    public void updateOffreDSM(Long offreId, String nom, String description,
                               LocalDate dateDebut, LocalDate dateFin, boolean active,
                               List<OffreProduit> nouveauxProduits) {
    
        Offre managed = offreRepository.findById(offreId)
            .orElseThrow(() -> new RuntimeException("Offre introuvable avec l'ID : " + offreId));
    
        // 1) Champs simples
        managed.setNom(nom);
        managed.setDescription(description);
        if (dateDebut != null) managed.setDateDebut(dateDebut);
        if (dateFin != null) managed.setDateFin(dateFin);
        managed.setActive(active);
    
        // Mettre à jour les enfants seulement si une nouvelle liste est fournie
        if (nouveauxProduits != null) {
            // 2) Index des enfants existants
            java.util.Map<Long, OffreProduit> existantsParId = managed.getProduits().stream()
                .filter(op -> op.getId() != null)
                .collect(java.util.stream.Collectors.toMap(OffreProduit::getId, java.util.function.Function.identity()));

            // 3) Mettre à jour/ajouter
            java.util.Set<Long> idsNouveaux = new java.util.HashSet<>();
            for (OffreProduit np : nouveauxProduits) {
                if (np.getId() != null && existantsParId.containsKey(np.getId())) {
                    // UPDATE sur l'enfant déjà managé
                    OffreProduit opManaged = existantsParId.get(np.getId());
                    copierChampsOffreProduit(np, opManaged);
                    idsNouveaux.add(np.getId());
                } else {
                    // NEW : créer un enfant managé et l'attacher proprement
                    OffreProduit nouveau = new OffreProduit();
                    copierChampsOffreProduit(np, nouveau);
                    // Ajout manuel à la collection et gestion de la relation bidirectionnelle
                    managed.getProduits().add(nouveau);
                    nouveau.setOffre(managed);
                    if (nouveau.getId() != null) idsNouveaux.add(nouveau.getId());
                }
            }

            // 4) Supprimer ceux qui ont disparu de la nouvelle liste
            managed.getProduits().removeIf(op -> op.getId() != null && !idsNouveaux.contains(op.getId()));
        }
    
        // 5) Pas besoin de save() explicite : le dirty checking s’en charge (transaction ouverte).
    }
    
    private void copierChampsOffreProduit(OffreProduit src, OffreProduit dst) {
        // Adapte cette liste à ton modèle :
        dst.setPrix(src.getPrix());
        dst.setRemisePourcent(src.getRemisePourcent());
        dst.setQuantiteMin(src.getQuantiteMin());
        dst.setQuantiteMax(src.getQuantiteMax());
        dst.setColisage(src.getColisage());
        dst.setProduit(src.getProduit()); // si tu as une ref vers Produit
        // etc.
    }

    @Override
    public boolean isOffreAccessibleForUser(Offre offre, User user, LocalDate today) {
        if (offre == null || user == null) return false;
        if (!offre.isActive()) return false;
        if (offre.getDateDebut() == null || offre.getDateFin() == null) return false;
        LocalDate t = (today != null) ? today : LocalDate.now();
        if (offre.getDateDebut().isAfter(t)) return false;
        if (offre.getDateFin().isBefore(t)) return false;
        if (offre.getGamme() == null || user.getGammes() == null) return false;
        return user.getGammes().contains(offre.getGamme());
    }

    @Override
    public List<Offre> findActiveOffresForUser(User user) {
        LocalDate today = LocalDate.now();
        if (user == null || user.getGammes() == null) return java.util.Collections.emptyList();
        return getAllOffres().stream()
                .filter(o -> o.getDateDebut() != null && o.getDateFin() != null)
                .filter(o -> !o.getDateDebut().isAfter(today))
                .filter(o -> !o.getDateFin().isBefore(today))
                .filter(Offre::isActive)
                .filter(o -> o.getGamme() != null && user.getGammes().contains(o.getGamme()))
                .toList();
    }

    @Override
    public void createOffre(Offre offre, java.util.List<com.project.service.offer.OffreProduitInput> produitsPayload, User owner) {
        // TODO implement creation using payload; fallback to enregistrerOffre for now
        if (offre != null) {
            offre.setCreatedByUser(owner);
            enregistrerOffre(offre);
        }
    }
}
