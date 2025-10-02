package com.project.entity;

import com.project.entity.base.AuditableEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Commande extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dateCommande;

    private int totalQuantite;

    private double totalPrix;

    // Nouveau: traçabilité des modifications par OKACHA
    @Column(name = "total_avant_modification")
    private Double totalAvantModification;

    @Column(name = "total_apres_modification")
    private Double totalApresModification;

    @ManyToOne
    @JoinColumn(name = "pharmacy_id")
    private Pharmacy pharmacy;

    @ManyToOne
    @JoinColumn(name = "offre_id")
    private Offre offre;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneCommande> lignes;

    @Column(name = "condition_paiement")
    private String conditionPaiement;

    @Column(name = "type_reglement")
    private String typeReglement;

    @Lob
    @Column(name = "consignes")
    private String consignes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CommandeStatus status = CommandeStatus.CREATED;

    // === Constructeur par défaut ===
    public Commande() {
        this.dateCommande = LocalDateTime.now();
        this.status = CommandeStatus.CREATED;
    }

    // === Getters ===

    public Long getId() {
        return id;
    }

    public LocalDateTime getDateCommande() {
        return dateCommande;
    }

    public int getTotalQuantite() {
        return totalQuantite;
    }

    public double getTotalPrix() {
        return totalPrix;
    }

    public Double getTotalAvantModification() {
        return totalAvantModification;
    }

    public Double getTotalApresModification() {
        return totalApresModification;
    }

    public Pharmacy getPharmacy() {
        return pharmacy;
    }

    public Offre getOffre() {
        return offre;
    }

    public User getUser() {
        return user;
    }

    public List<LigneCommande> getLignes() {
        return lignes;
    }

    public String getConditionPaiement() {
        return conditionPaiement;
    }

    public String getTypeReglement() {
        return typeReglement;
    }

    public String getConsignes() {
        return consignes;
    }

    public CommandeStatus getStatus() {
        return status;
    }

    // === Setters ===

    public void setId(Long id) {
        this.id = id;
    }

    public void setDateCommande(LocalDateTime dateCommande) {
        this.dateCommande = dateCommande;
    }

    public void setTotalQuantite(int totalQuantite) {
        this.totalQuantite = totalQuantite;
    }

    public void setTotalPrix(double totalPrix) {
        this.totalPrix = totalPrix;
    }

    public void setTotalAvantModification(Double totalAvantModification) {
        this.totalAvantModification = totalAvantModification;
    }

    public void setTotalApresModification(Double totalApresModification) {
        this.totalApresModification = totalApresModification;
    }

    public void setPharmacy(Pharmacy pharmacy) {
        this.pharmacy = pharmacy;
    }

    public void setOffre(Offre offre) {
        this.offre = offre;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setLignes(List<LigneCommande> lignes) {
        this.lignes = lignes;
    }

    public void setConditionPaiement(String conditionPaiement) {
        this.conditionPaiement = conditionPaiement;
    }

    public void setTypeReglement(String typeReglement) {
        this.typeReglement = typeReglement;
    }

    public void setConsignes(String consignes) {
        this.consignes = consignes;
    }

    public void setStatus(CommandeStatus status) {
        this.status = status;
    }
}
