package com.project.service.dto;

import com.project.entity.LigneCommandeStatus;

public class LigneCommandeView {
    private Long id;
    private String produitNom;
    private int quantite;
    private double prixUnitaire;
    private LigneCommandeStatus statutProduit;
    private boolean rupture;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }
    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
    public double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    public LigneCommandeStatus getStatutProduit() { return statutProduit; }
    public void setStatutProduit(LigneCommandeStatus statutProduit) { this.statutProduit = statutProduit; }
    public boolean isRupture() { return rupture; }
    public void setRupture(boolean rupture) { this.rupture = rupture; }
}
