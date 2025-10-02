package com.project.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Form DTO for order preview/confirmation.
 */
public class OrderSubmitInput {
    private Long pharmacyId;
    private Long offreId;
    private String conditionPaiement;
    private String typeReglement;
    private String consignes;
    private List<OrderLineInput> lines = new ArrayList<>();

    public Long getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(Long pharmacyId) { this.pharmacyId = pharmacyId; }
    public Long getOffreId() { return offreId; }
    public void setOffreId(Long offreId) { this.offreId = offreId; }
    public String getConditionPaiement() { return conditionPaiement; }
    public void setConditionPaiement(String conditionPaiement) { this.conditionPaiement = conditionPaiement; }
    public String getTypeReglement() { return typeReglement; }
    public void setTypeReglement(String typeReglement) { this.typeReglement = typeReglement; }
    public String getConsignes() { return consignes; }
    public void setConsignes(String consignes) { this.consignes = consignes; }
    public List<OrderLineInput> getLines() { return lines; }
    public void setLines(List<OrderLineInput> lines) { this.lines = lines; }
}

