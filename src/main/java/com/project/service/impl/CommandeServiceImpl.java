package com.project.service.impl;

import com.project.entity.Commande;
import com.project.entity.CommandeStatus;
import com.project.entity.User;
import com.project.repository.CommandeRepository;
import com.project.service.CommandeService;
import com.project.service.OffreService;
import com.project.service.PharmacyService;
import com.project.service.ProductService;
import com.project.service.dto.OrderLineInput;
import com.project.service.dto.OrderPreview;
import com.project.service.port.NotificationPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
public class CommandeServiceImpl implements CommandeService {

    private final CommandeRepository commandeRepository;
    private final OffreService offreService;
    private final NotificationPort notificationPort;
    private final PharmacyService pharmacyService;
    private final ProductService productService;

    @Autowired
    public CommandeServiceImpl(CommandeRepository commandeRepository,
                               OffreService offreService,
                               NotificationPort notificationPort,
                               PharmacyService pharmacyService,
                               ProductService productService) {
        this.commandeRepository = commandeRepository;
        this.offreService = offreService;
        this.notificationPort = notificationPort;
        this.pharmacyService = pharmacyService;
        this.productService = productService;
    }

    // Convenience constructor for tests that don't need all dependencies
    public CommandeServiceImpl(CommandeRepository commandeRepository,
                               OffreService offreService,
                               NotificationPort notificationPort) {
        this(commandeRepository, offreService, notificationPort, null, null);
    }

    @Override
    public OrderPreview buildPreview(User actor, Long offreId, Long pharmacyId, List<OrderLineInput> lines, String paymentCondition, String typeReglement, String consignes) {
        OrderPreview preview = new OrderPreview();
        if (lines == null || lines.isEmpty()) {
            preview.setMessage("Aucune ligne de commande");
            preview.setLines(Collections.emptyList());
            preview.setTotalAmount(BigDecimal.ZERO);
            preview.setTotalAfterDiscount(BigDecimal.ZERO);
            preview.setTotalQuantity(0);
            return preview;
        }

        java.util.ArrayList<OrderPreview.Line> outLines = new java.util.ArrayList<>();
        int totalQty = 0;
        BigDecimal total = BigDecimal.ZERO;

        for (OrderLineInput l : lines) {
            if (l == null || l.getQuantity() <= 0 || l.getProductId() == null) continue;
            var product = productService.getProduitById(l.getProductId());
            double basePu = offreService.getPrixProduitDansOffre(offreId, l.getProductId());
            double remise = offreService.getRemisePourProduit(offreId, l.getProductId());
            double puRemise = basePu - (basePu * remise / 100.0);

            BigDecimal unitPrice = BigDecimal.valueOf(basePu).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal discountedUnit = BigDecimal.valueOf(puRemise).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal lineTotal = discountedUnit.multiply(BigDecimal.valueOf(l.getQuantity()))
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            OrderPreview.Line pv = new OrderPreview.Line();
            pv.setProductId(l.getProductId());
            pv.setProductName(product.getNom());
            pv.setQuantity(l.getQuantity());
            pv.setUnitPrice(unitPrice);
            pv.setDiscountedUnitPrice(discountedUnit);
            pv.setLineTotal(lineTotal);
            outLines.add(pv);

            total = total.add(lineTotal);
            totalQty += l.getQuantity();
        }

        preview.setLines(outLines);
        preview.setTotalAmount(total);
        preview.setTotalAfterDiscount(applyPaymentDiscount(total, paymentCondition));
        preview.setTotalQuantity(totalQty);
        return preview;
    }

    @Override
    @Transactional
    public Commande createCommande(User actor, Long offreId, Long pharmacyId, List<OrderLineInput> lines, String paymentCondition, String typeReglement, String consignes) {
        if (actor == null) throw new IllegalArgumentException("Utilisateur requis");
        if (offreId == null) throw new IllegalArgumentException("Offre requise");
        if (pharmacyId == null) throw new IllegalArgumentException("Pharmacie requise");
        if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("Aucune ligne");

        var offre = offreService.getOffreById(offreId);
        var pharmacy = pharmacyService.findById(pharmacyId);
        if (pharmacy == null) throw new IllegalArgumentException("Pharmacie introuvable");

        Commande cmd = new Commande();
        cmd.setUser(actor);
        cmd.setOffre(offre);
        cmd.setPharmacy(pharmacy);
        cmd.setConditionPaiement(paymentCondition);
        cmd.setTypeReglement(typeReglement);
        cmd.setConsignes(consignes);

        java.util.ArrayList<com.project.entity.LigneCommande> lcs = new java.util.ArrayList<>();
        int totalQty = 0;
        BigDecimal total = BigDecimal.ZERO;

        for (OrderLineInput l : lines) {
            if (l == null || l.getProductId() == null || l.getQuantity() <= 0) continue;
            var product = productService.getProduitById(l.getProductId());
            double basePu = offreService.getPrixProduitDansOffre(offreId, l.getProductId());
            double remise = offreService.getRemisePourProduit(offreId, l.getProductId());
            double puRemise = basePu - (basePu * remise / 100.0);
            BigDecimal discountedUnit = BigDecimal.valueOf(puRemise).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal lineTotal = discountedUnit.multiply(BigDecimal.valueOf(l.getQuantity()))
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            com.project.entity.LigneCommande lc = new com.project.entity.LigneCommande();
            lc.setCommande(cmd);
            lc.setProduit(product);
            lc.setQuantite(l.getQuantity());
            lc.setPrixUnitaire(discountedUnit.doubleValue());
            lcs.add(lc);

            total = total.add(lineTotal);
            totalQty += l.getQuantity();
        }

        // appliquer remise paiement au total
        BigDecimal totalAfterPay = applyPaymentDiscount(total, paymentCondition);
        cmd.setTotalQuantite(totalQty);
        cmd.setTotalPrix(totalAfterPay.doubleValue());
        cmd.setLignes(lcs);

        Commande saved = commandeRepository.save(cmd);
        // Notify the manager (DSM) of the actor if present
        try {
            if (actor != null && actor.getManager() != null) {
                String title = "Nouvelle commande";
                String message = String.format("%s a créé une commande #%d pour la pharmacie %s (total: %.2f)",
                        actor.getUsername(),
                        saved.getId(),
                        pharmacy.getNom(),
                        saved.getTotalPrix());
                notificationPort.notifyManager(actor.getManager(), saved, title, message);
            }
        } catch (Exception ex) {
            // Do not break the transaction if notification fails
        }
        return saved;
    }

    @Override
    @Transactional
    public void updateStatus(Long id, CommandeStatus newStatus) {
        // TODO implement status transition validation
        Commande cmd = commandeRepository.findById(id).orElseThrow(() -> new RuntimeException("Commande introuvable"));
        cmd.setStatus(newStatus);
        commandeRepository.save(cmd);
    }

    @Override
    public Commande getById(Long id) {
        return commandeRepository.findById(id).orElseThrow(() -> new RuntimeException("Commande introuvable"));
    }

    @Override
    public List<Commande> findAllWithFilters(LocalDate startDate, LocalDate endDate, Integer month, Integer year) {
        // delegate to repository @Query; pas de statut ici
        return commandeRepository.findAllWithFilters(startDate, endDate, month, year, null);
    }

    @Override
    public List<Commande> findByUserWithFiltersAdvanced(User user, LocalDate startDate, LocalDate endDate, Integer month, Integer year, String pharmacyName, Long secteurId) {
        return commandeRepository.findByUserWithFiltersAdvanced(user, startDate, endDate, month, year, pharmacyName, secteurId);
    }

    @Override
    public byte[] exportAll() {
        // TODO implement export to XLSX
        return new byte[0];
    }

    @Override
    public BigDecimal calculateLinePrice(Long offreId, Long productId) {
        double pu = offreService.getPrixProduitDansOffre(offreId, productId);
        double remise = offreService.getRemisePourProduit(offreId, productId);
        double prixRemise = pu - (pu * remise / 100.0);
        return BigDecimal.valueOf(prixRemise);
    }

    @Override
    public BigDecimal applyPaymentDiscount(BigDecimal total, String condition) {
        if (total == null) return BigDecimal.ZERO;
        if (condition == null) return total;
        String c = condition.trim().toUpperCase();
        if ("AU COMPTANT".equals(c) || "AU COMPTANT SE".equals(c)) {
            return total.multiply(BigDecimal.valueOf(0.98)).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return total;
    }

    @Override
    @Transactional
    public Commande updateCommandeLines(Long id, java.util.List<OrderLineInput> lines) {
        if (id == null) throw new IllegalArgumentException("ID commande requis");
        if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("Aucune ligne");

        Commande cmd = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        Long offreId = cmd.getOffre() != null ? cmd.getOffre().getId() : null;
        if (offreId == null) throw new IllegalStateException("Commande sans offre");

        java.util.ArrayList<com.project.entity.LigneCommande> lcs = new java.util.ArrayList<>();
        int totalQty = 0;
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;

        for (OrderLineInput l : lines) {
            if (l == null || l.getProductId() == null || l.getQuantity() <= 0) continue;
            var product = productService.getProduitById(l.getProductId());
            double basePu = offreService.getPrixProduitDansOffre(offreId, l.getProductId());
            double remise = offreService.getRemisePourProduit(offreId, l.getProductId());
            double puRemise = basePu - (basePu * remise / 100.0);
            java.math.BigDecimal discountedUnit = java.math.BigDecimal.valueOf(puRemise)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal lineTotal = discountedUnit.multiply(java.math.BigDecimal.valueOf(l.getQuantity()))
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            com.project.entity.LigneCommande lc = new com.project.entity.LigneCommande();
            lc.setCommande(cmd);
            lc.setProduit(product);
            lc.setQuantite(l.getQuantity());
            lc.setPrixUnitaire(discountedUnit.doubleValue());
            lcs.add(lc);

            total = total.add(lineTotal);
            totalQty += l.getQuantity();
        }

        if (lcs.isEmpty()) throw new IllegalArgumentException("Aucune ligne valide");

        java.math.BigDecimal totalAfterPay = applyPaymentDiscount(total, cmd.getConditionPaiement());
        cmd.setTotalQuantite(totalQty);
        cmd.setTotalPrix(totalAfterPay.doubleValue());
        // Important: avec orphanRemoval=true, modifier la collection existante au lieu de la remplacer
        java.util.List<com.project.entity.LigneCommande> existing = cmd.getLignes();
        if (existing == null) {
            cmd.setLignes(lcs);
        } else {
            existing.clear();
            existing.addAll(lcs);
        }

        return commandeRepository.save(cmd);
    }

    @Override
    @Transactional
    public Commande updateCommandeSelection(Long id, java.util.List<Long> selectedLineIds) {
        if (id == null) throw new IllegalArgumentException("ID commande requis");
        if (selectedLineIds == null) throw new IllegalArgumentException("Sélection requise");

        Commande cmd = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        // Totaux avant
        Double before = cmd.getTotalPrix();

        // Marquer les lignes sélectionnées comme INCLUSE, les autres RUPTURE_STOCK
        List<com.project.entity.LigneCommande> existing = cmd.getLignes();
        if (existing == null || existing.isEmpty()) {
            throw new IllegalArgumentException("Commande sans lignes");
        }
        int totalQty = 0;
        java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
        java.util.Set<Long> selected = new java.util.HashSet<>(selectedLineIds);
        for (com.project.entity.LigneCommande lc : existing) {
            boolean include = lc.getId() != null && selected.contains(lc.getId());
            lc.setStatutProduit(include ? com.project.entity.LigneCommandeStatus.INCLUSE : com.project.entity.LigneCommandeStatus.RUPTURE_STOCK);
            if (include) {
                totalQty += lc.getQuantite();
                sum = sum.add(java.math.BigDecimal.valueOf(lc.getPrixUnitaire())
                        .multiply(java.math.BigDecimal.valueOf(lc.getQuantite())));
            }
        }
        if (totalQty == 0) throw new IllegalArgumentException("Aucune ligne sélectionnée");

        java.math.BigDecimal totalAfterPay = applyPaymentDiscount(sum, cmd.getConditionPaiement());

        // Appliquer les changements et persister
        cmd.setTotalAvantModification(before);
        cmd.setTotalApresModification(totalAfterPay.doubleValue());
        cmd.setTotalQuantite(totalQty);
        cmd.setTotalPrix(totalAfterPay.doubleValue());

        Commande saved = commandeRepository.save(cmd);
        // La notification détaillée est envoyée par le contrôleur (connaît l'acteur et le détail)
        return saved;
    }
}
