package com.project.service;

import com.project.entity.Commande;
import com.project.entity.CommandeStatus;
import com.project.entity.User;
import com.project.service.dto.OrderLineInput;
import com.project.service.dto.OrderPreview;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service orchestrating all order (Commande) business logic.
 */
public interface CommandeService {

    /**
     * Builds a preview for an order without persisting it.
     */
    OrderPreview buildPreview(User actor,
                              Long offreId,
                              Long pharmacyId,
                              List<OrderLineInput> lines,
                              String paymentCondition,
                              String typeReglement,
                              String consignes);

    /**
     * Creates an order after validating business rules and persists it.
     */
    Commande createCommande(User actor,
                            Long offreId,
                            Long pharmacyId,
                            List<OrderLineInput> lines,
                            String paymentCondition,
                            String typeReglement,
                            String consignes);

    void updateStatus(Long id, CommandeStatus newStatus);

    Commande getById(Long id);

    List<Commande> findAllWithFilters(LocalDate startDate, LocalDate endDate, Integer month, Integer year);

    /** Advanced filters for a specific user (VM) with optional pharmacy name and secteur. */
    List<Commande> findByUserWithFiltersAdvanced(User user,
                                                 LocalDate startDate,
                                                 LocalDate endDate,
                                                 Integer month,
                                                 Integer year,
                                                 String pharmacyName,
                                                 Long secteurId);

    byte[] exportAll();

    /**
     * Exposes price logic per product within an offer (if needed by other services).
     */
    BigDecimal calculateLinePrice(Long offreId, Long productId);

    /**
     * Applies payment discount rules (e.g., 2% for cash payment).
     */
    BigDecimal applyPaymentDiscount(BigDecimal total, String condition);

    /** Update existing order lines with new quantities and recompute totals. */
    Commande updateCommandeLines(Long id, java.util.List<OrderLineInput> lines);

    /**
     * OKACHA selection update: keep only selected line IDs, recompute totals,
     * and persist totalAvantModification/totalApresModification.
     */
    Commande updateCommandeSelection(Long id, java.util.List<Long> selectedLineIds);
}
