package com.project.entity;

import com.project.entity.base.AuditableEntity;
import jakarta.persistence.*;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "user") // garde le nom exact de ta table existante
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = true;

    // Rôle chargé eagerly pour l’authentification
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    // Secteurs liés à l'utilisateur
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_secteurs",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "secteur_id")
    )
    private Set<Secteur> secteurs;

    // Gammes liées à l'utilisateur
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_gamme",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "gamme_id")
    )
    private List<Gamme> gammes;

    // =========================
    //   AJOUTS POUR LE DSM/VM
    // =========================

    /**
     * Le manager (DSM) de cet utilisateur (VM).
     * Null si l'utilisateur est lui-même un DSM ou n'est rattaché à personne.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id") // -> colonne FK dans la table user
    private User manager;

    /**
     * La liste des utilisateurs (VMs) rattachés à ce manager (DSM).
     * C’est l’inverse du champ "manager".
     */
    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY)
    private List<User> team;

    // ====== Getters / Setters ======

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Set<Secteur> getSecteurs() { return secteurs; }
    public void setSecteurs(Set<Secteur> secteurs) { this.secteurs = secteurs; }

    public List<Gamme> getGammes() { return gammes; }
    public void setGammes(List<Gamme> gammes) { this.gammes = gammes; }

    public User getManager() { return manager; }
    public void setManager(User manager) { this.manager = manager; }

    public List<User> getTeam() { return team; }
    public void setTeam(List<User> team) { this.team = team; }
}
