package com.project.entity;

import jakarta.persistence.*;

@Entity
public class OffreProduit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_id", nullable = false)
    private Offre offre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product produit;

    private int quantiteMin;
    private int quantiteMax;
    private double prix;
    private int remisePourcent;

    // Colisage (nombre d'unités par colis) au niveau ligne produit
    private Integer colisage;

    public OffreProduit() {}

    public OffreProduit(Offre offre, Product produit, int min, int max, double prix, int remise) {
        this.offre = offre;
        this.produit = produit;
        this.quantiteMin = min;
        this.quantiteMax = max;
        this.prix = prix;
        this.remisePourcent = remise;
    }

    // ====== GETTERS ======
    public Long getId() {
        return id;
    }

    public Offre getOffre() {
        return offre;
    }

    public Product getProduit() {
        return produit;
    }

    public int getQuantiteMin() {
        return quantiteMin;
    }

    public int getQuantiteMax() {
        return quantiteMax;
    }

    public double getPrix() {
        return prix;
    }

    public int getRemisePourcent() {
        return remisePourcent;
    }

    public Integer getColisage() {
        return colisage;
    }

    // ✅ Pour usage DSM (conversion automatique)
    public Double getRemisePourcentAsDouble() {
        return (double) remisePourcent;
    }

    // ====== SETTERS ======
    public void setId(Long id) {
        this.id = id;
    }

    public void setOffre(Offre offre) {
        this.offre = offre;
    }

    public void setProduit(Product produit) {
        this.produit = produit;
    }

    public void setQuantiteMin(int quantiteMin) {
        this.quantiteMin = quantiteMin;
    }

    public void setQuantiteMax(int quantiteMax) {
        this.quantiteMax = quantiteMax;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public void setRemisePourcent(int remisePourcent) {
        this.remisePourcent = remisePourcent;
    }

    public void setColisage(Integer colisage) {
        this.colisage = colisage;
    }

    // ✅ Pour usage DSM (permet d’appeler setRemisePourcent avec un Double)
    public void setRemisePourcentFromDouble(Double remisePourcent) {
        this.remisePourcent = (remisePourcent != null) ? remisePourcent.intValue() : 0;
    }

    // equals/hashCode : basé UNIQUEMENT sur id quand non-null
@Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OffreProduit)) return false;
    OffreProduit other = (OffreProduit) o;
    return id != null && id.equals(other.id);
}
@Override public int hashCode() { return 31; }
}
