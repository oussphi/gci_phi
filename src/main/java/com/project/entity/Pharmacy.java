package com.project.entity;

import com.project.entity.base.AuditableEntity;
import jakarta.persistence.*;
import java.util.List;

@Entity
public class Pharmacy extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private String adresse;

    private String telephone;

    @ManyToOne
    @JoinColumn(name = "secteur_id")
    private Secteur secteur;
    

    @OneToMany(mappedBy = "pharmacy", cascade = CascadeType.ALL)
    private List<Commande> commandes;

    public Pharmacy() {}

    public Pharmacy(String nom, String adresse, String telephone, Secteur secteur) {
        this.nom = nom;
        this.adresse = adresse;
        this.telephone = telephone;
        this.secteur = secteur;
    }

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Secteur getSecteur() {
        return secteur;
    }

    public void setSecteur(Secteur secteur) {
        this.secteur = secteur;
    }

    public List<Commande> getCommandes() {
        return commandes;
    }

    public void setCommandes(List<Commande> commandes) {
        this.commandes = commandes;
    }

    @Column(nullable = false)
    private boolean payed = false;

    // Getter & Setter
    public boolean isPayed() {
        return payed;
    }

    public void setPayed(boolean payed) {
        this.payed = payed;
    }

    @Column(nullable = false, unique = true)
    private String ice;

    // Getter
    public String getIce() {
        return ice;
    }

    // Setter
    public void setIce(String ice) {
        this.ice = ice;
    }

}
