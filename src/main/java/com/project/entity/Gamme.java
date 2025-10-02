package com.project.entity;

import com.project.entity.base.AuditableEntity;
import jakarta.persistence.*;
import java.util.List;

@Entity
public class Gamme extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    @Column(length = 1000)
    private String description;

    @ManyToMany(mappedBy = "gammes")
    private List<User> users;

    @ManyToMany(mappedBy = "gammes")
    private List<Product> produits;

    @OneToMany(mappedBy = "gamme")
    private List<Offre> offres;

    // Getters and Setters
    // ...

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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<User> getUsers() {
        return users;
    }
    
    public void setUsers(List<User> users) {
        this.users = users;
    }
    
    public List<Product> getProduits() {
        return produits;
    }
    
    public void setProduits(List<Product> produits) {
        this.produits = produits;
    }
    
    public List<Offre> getOffres() {
        return offres;
    }
    
    public void setOffres(List<Offre> offres) {
        this.offres = offres;
    }
    
}
