package com.project.entity;

import com.project.entity.base.AuditableEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Offre extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String description;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    // createdBy handled via auditor (username); keep legacy relation if needed
    @Column(name = "created_by_username")
    private String createdByUsername;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @OneToMany(mappedBy = "offre", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OffreProduit> produits = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "gamme_id")
    private Gamme gamme;



    // ====== GETTERS ======

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getDateCreation() { return dateCreation; }

    public User getCreatedByUser() { return createdByUser; }
    public String getCreatedByUsername() { return createdByUsername; }

    public List<OffreProduit> getProduits() {
        return produits;
    }


    // ====== SETTERS ======

    public void setId(Long id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public void setCreatedByUser(User createdByUser) { this.createdByUser = createdByUser; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }

/** Remplace la liste en ré-attachant chaque enfant à 'this' */
public void setProduits(List<OffreProduit> produits) {
    this.produits.clear();
    if (produits != null) {
        for (OffreProduit op : produits) {
            op.setOffre(this); // IMPORTANT pour la FK
            this.produits.add(op);
        }
    }
}

    public Gamme getGamme() {
        return gamme;
    }
    
    public void setGamme(Gamme gamme) {
        this.gamme = gamme;
    }

    public void addProduit(OffreProduit op) {
        this.produits.add(op);
        op.setOffre(this);
    }
    public void removeProduit(OffreProduit op) {
        this.produits.remove(op);
        op.setOffre(null);
    }
}
