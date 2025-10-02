package com.project.entity;

import com.project.entity.base.AuditableEntity;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "produits")
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, length = 100)
    private String code;

    @Column(name = "designation")
    private String nom; // ✅ renommé de name ➜ nom

    @Column(name = "prix")
    private double prix;

    @Column(name = "qte")
    private int quantite;

    // Deprecated: now inherited from AuditableEntity (Instant)
    // Keeping fields removed to avoid conflicts

    @ManyToMany
    @JoinTable(
        name = "product_gamme",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "gamme_id")
    )
    private List<Gamme> gammes;


    // === CONSTRUCTEURS ===
    public Product() {}

    public Product(String nom, double prix, int quantite) {
        this.nom = nom;
        this.prix = prix;
        this.quantite = quantite;
    }

    // === GETTERS ===
    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public double getPrix() {
        return prix;
    }

    public int getQuantite() {
        return quantite;
    }

    // createdAt/updatedAt provided by AuditableEntity

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    // === SETTERS ===
    public void setId(Long id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    // createdAt/updatedAt provided by AuditableEntity

    public List<Gamme> getGammes() {
        return gammes;
    }
    
    public void setGammes(List<Gamme> gammes) {
        this.gammes = gammes;
    }
    
}
