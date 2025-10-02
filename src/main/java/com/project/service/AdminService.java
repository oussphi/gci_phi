package com.project.service;

import com.project.entity.Admin;
import com.project.entity.User;
import com.project.entity.Role;
import com.project.entity.Pharmacy;
import com.project.entity.Product;
import com.project.entity.Offre;
import com.project.entity.Commande;
import com.project.entity.Gamme;
import com.project.entity.Secteur;

import java.util.List;
import java.util.Map;

public interface AdminService {
    
    // Gestion des administrateurs
    Admin createAdmin(Admin admin);
    Admin updateAdmin(Long id, Admin admin);
    void deleteAdmin(Long id);
    Admin getAdminById(Long id);
    Admin getAdminByUsername(String username);
    List<Admin> getAllAdmins();
    List<Admin> searchAdmins(String searchTerm);
    void toggleAdminStatus(Long id);
    
    // Gestion des utilisateurs
    List<User> getAllUsers();
    User getUserById(Long id);
    User createUser(User user);
    User createUser(User user, java.util.List<Long> secteurIds, java.util.List<Long> gammeIds);
    User createUser(User user, java.util.List<Long> secteurIds, java.util.List<Long> gammeIds, Long roleId, Long managerId);
    User updateUser(Long id, User user);
    User updateUser(Long id, User user, java.util.List<Long> secteurIds, java.util.List<Long> gammeIds, Long roleId, Long managerId);
    void deleteUser(Long id);
    void toggleUserStatus(Long id);
    List<User> getUsersByRole(String roleName);
    Map<String, Long> getUserCountByRole();
    
    // Gestion des rôles
    List<Role> getAllRoles();
    Role createRole(Role role);
    Role updateRole(Long id, Role role);
    void deleteRole(Long id);
    
    // Gestion des pharmacies
    List<Pharmacy> getAllPharmacies();
    Pharmacy getPharmacyById(Long id);
    Pharmacy updatePharmacy(Long id, Pharmacy pharmacy);
    void deletePharmacy(Long id);
    Map<String, Long> getPharmacyCountBySecteur();
    
    // Gestion des produits
    List<Product> getAllProducts();
    Product getProductById(Long id);
    Product createProduct(Product product);
    Product updateProduct(Long id, Product product);
    Product createProduct(Product product, java.util.List<Long> gammeIds);
    Product updateProduct(Long id, Product product, java.util.List<Long> gammeIds);
    void deleteProduct(Long id);
    
    // Gestion des offres
    List<Offre> getAllOffres();
    Offre getOffreById(Long id);
    void deleteOffre(Long id);
    
    // Gestion des commandes
    List<Commande> getAllCommandes();
    Commande getCommandeById(Long id);
    void deleteCommande(Long id);
    Map<String, Long> getCommandeCountByStatus();
    
    // Gestion des gammes
    List<Gamme> getAllGammes();
    Gamme getGammeById(Long id);
    Gamme createGamme(Gamme gamme);
    Gamme updateGamme(Long id, Gamme gamme);
    void deleteGamme(Long id);
    
    // Gestion des secteurs
    List<Secteur> getAllSecteurs();
    Secteur getSecteurById(Long id);
    Secteur createSecteur(Secteur secteur);
    Secteur updateSecteur(Long id, Secteur secteur);
    void deleteSecteur(Long id);
    
    // Statistiques globales
    Map<String, Object> getGlobalStatistics();
    Map<String, Long> getSystemOverview();
}
