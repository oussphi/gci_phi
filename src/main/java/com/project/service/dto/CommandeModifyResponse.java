package com.project.service.dto;

public class CommandeModifyResponse {
    private Long commandeId;
    private double totalAvantModification;
    private double totalApresModification;
    private int totalQuantite;

    public Long getCommandeId() { return commandeId; }
    public void setCommandeId(Long commandeId) { this.commandeId = commandeId; }
    public double getTotalAvantModification() { return totalAvantModification; }
    public void setTotalAvantModification(double totalAvantModification) { this.totalAvantModification = totalAvantModification; }
    public double getTotalApresModification() { return totalApresModification; }
    public void setTotalApresModification(double totalApresModification) { this.totalApresModification = totalApresModification; }
    public int getTotalQuantite() { return totalQuantite; }
    public void setTotalQuantite(int totalQuantite) { this.totalQuantite = totalQuantite; }
}

