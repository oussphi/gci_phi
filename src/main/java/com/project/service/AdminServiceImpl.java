package com.project.service;

import com.project.entity.*;
import com.project.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PharmacyRepository pharmacyRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private OffreRepository offreRepository;
    
    @Autowired
    private CommandeRepository commandeRepository;
    
    @Autowired
    private GammeRepository gammeRepository;
    
    @Autowired
    private SecteurRepository secteurRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // === GESTION DES ADMINISTRATEURS ===
    
    @Override
    public Admin createAdmin(Admin admin) {
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setCreatedAt(LocalDateTime.now());
        admin.setActive(true);
        return adminRepository.save(admin);
    }
    
    @Override
    public Admin updateAdmin(Long id, Admin admin) {
        Admin existingAdmin = getAdminById(id);
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            existingAdmin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        existingAdmin.setFullName(admin.getFullName());
        existingAdmin.setEmail(admin.getEmail());
        existingAdmin.setPhone(admin.getPhone());
        existingAdmin.setActive(admin.isActive());
        existingAdmin.setSuperAdmin(admin.isSuperAdmin());
        return adminRepository.save(existingAdmin);
    }
    
    @Override
    public void deleteAdmin(Long id) {
        adminRepository.deleteById(id);
    }
    
    @Override
    public Admin getAdminById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé avec l'ID: " + id));
    }
    
    @Override
    public Admin getAdminByUsername(String username) {
        return adminRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé: " + username));
    }
    
    @Override
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }
    
    @Override
    public List<Admin> searchAdmins(String searchTerm) {
        return adminRepository.searchAdmins(searchTerm);
    }
    
    @Override
    public void toggleAdminStatus(Long id) {
        Admin admin = getAdminById(id);
        admin.setActive(!admin.isActive());
        adminRepository.save(admin);
    }
    
    // === GESTION DES UTILISATEURS ===
    
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
    }
    
    @Override
    public User createUser(User user) {
        if (user.getId() != null) {
            // sécurité: éviter d'écraser un autre user
            user.setId(null);
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        // si non fourni, activer par défaut
        user.setEnabled(user.isEnabled());
        return userRepository.save(user);
    }

    @Override
    public User createUser(User user, java.util.List<Long> secteurIds, java.util.List<Long> gammeIds) {
        if (user.getId() != null) {
            user.setId(null);
        }
        // Normalize username and ensure uniqueness
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new RuntimeException("Le nom d'utilisateur est requis");
        }
        String normalizedUsername = user.getUsername().trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new RuntimeException("Le nom d'utilisateur existe déjà: " + normalizedUsername);
        }
        user.setUsername(normalizedUsername);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        user.setEnabled(user.isEnabled());

        // Bind secteurs
        if (secteurIds != null && !secteurIds.isEmpty()) {
            java.util.Set<Secteur> secteurs = new java.util.HashSet<>(secteurRepository.findAllById(secteurIds));
            user.setSecteurs(secteurs);
        } else {
            user.setSecteurs(null);
        }

        // Bind gammes
        if (gammeIds != null && !gammeIds.isEmpty()) {
            java.util.List<Gamme> gammes = gammeRepository.findAllById(gammeIds);
            user.setGammes(gammes);
        } else {
            user.setGammes(null);
        }
        return userRepository.save(user);
    }

    @Override
    public User createUser(User user, java.util.List<Long> secteurIds, java.util.List<Long> gammeIds, Long roleId, Long managerId) {
        if (user.getId() != null) {
            user.setId(null);
        }
        // Normalize username and ensure uniqueness
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new RuntimeException("Le nom d'utilisateur est requis");
        }
        String normalizedUsername = user.getUsername().trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new RuntimeException("Le nom d'utilisateur existe déjà: " + normalizedUsername);
        }
        user.setUsername(normalizedUsername);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        user.setEnabled(user.isEnabled());

        // Role
        if (roleId != null) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Rôle non trouvé avec l'ID: " + roleId));
            user.setRole(role);
        }

        // Manager
        if (managerId != null) {
            User manager = userRepository.findById(managerId)
                    .orElseThrow(() -> new RuntimeException("Gestionnaire non trouvé avec l'ID: " + managerId));
            user.setManager(manager);
        } else {
            user.setManager(null);
        }

        // Secteurs
        if (secteurIds != null && !secteurIds.isEmpty()) {
            java.util.Set<Secteur> secteurs = new java.util.HashSet<>(secteurRepository.findAllById(secteurIds));
            user.setSecteurs(secteurs);
        } else {
            user.setSecteurs(null);
        }

        // Gammes
        if (gammeIds != null && !gammeIds.isEmpty()) {
            java.util.List<Gamme> gammes = gammeRepository.findAllById(gammeIds);
            user.setGammes(gammes);
        } else {
            user.setGammes(null);
        }

        return userRepository.save(user);
    }

    @Override
    public User updateUser(Long id, User user) {
        User existingUser = getUserById(id);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        existingUser.setUsername(user.getUsername());
        existingUser.setEnabled(user.isEnabled());
        existingUser.setRole(user.getRole());
        return userRepository.save(existingUser);
    }

    @Override
    public User updateUser(Long id, User user, java.util.List<Long> secteurIds, java.util.List<Long> gammeIds, Long roleId, Long managerId) {
        User existingUser = getUserById(id);

        // Username: keep existing if unchanged; normalize if provided
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            existingUser.setUsername(user.getUsername().trim().toLowerCase());
        }

        // Password: only if provided
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Enabled flag
        existingUser.setEnabled(user.isEnabled());

        // Role
        if (roleId != null) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Rôle non trouvé avec l'ID: " + roleId));
            existingUser.setRole(role);
        }

        // Manager
        if (managerId != null) {
            User manager = userRepository.findById(managerId)
                    .orElseThrow(() -> new RuntimeException("Gestionnaire non trouvé avec l'ID: " + managerId));
            existingUser.setManager(manager);
        } else {
            existingUser.setManager(null);
        }

        // Secteurs
        if (secteurIds != null) {
            if (!secteurIds.isEmpty()) {
                java.util.Set<Secteur> secteurs = new java.util.HashSet<>(secteurRepository.findAllById(secteurIds));
                existingUser.setSecteurs(secteurs);
            } else {
                existingUser.setSecteurs(null);
            }
        }

        // Gammes
        if (gammeIds != null) {
            if (!gammeIds.isEmpty()) {
                java.util.List<Gamme> gammes = gammeRepository.findAllById(gammeIds);
                existingUser.setGammes(gammes);
            } else {
                existingUser.setGammes(null);
            }
        }

        return userRepository.save(existingUser);
    }
    
    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    @Override
    public void toggleUserStatus(Long id) {
        User user = getUserById(id);
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }
    
    @Override
    public List<User> getUsersByRole(String roleName) {
        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            throw new RuntimeException("Rôle non trouvé: " + roleName);
        }
        // Implémentation à adapter selon votre structure de données
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() != null && user.getRole().getName().equals(roleName))
                .toList();
    }
    
    @Override
    public Map<String, Long> getUserCountByRole() {
        Map<String, Long> countByRole = new HashMap<>();
        List<Role> roles = roleRepository.findAll();
        for (Role role : roles) {
            long count = userRepository.findAll().stream()
                    .filter(user -> user.getRole() != null && user.getRole().getId().equals(role.getId()))
                    .count();
            countByRole.put(role.getName(), count);
        }
        return countByRole;
    }
    
    // === GESTION DES RÔLES ===
    
    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
    
    @Override
    public Role createRole(Role role) {
        return roleRepository.save(role);
    }
    
    @Override
    public Role updateRole(Long id, Role role) {
        Role existingRole = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rôle non trouvé avec l'ID: " + id));
        existingRole.setName(role.getName());
        return roleRepository.save(existingRole);
    }
    
    @Override
    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }
    
    // === GESTION DES PHARMACIES ===
    
    @Override
    public List<Pharmacy> getAllPharmacies() {
        return pharmacyRepository.findAll();
    }
    
    @Override
    public Pharmacy getPharmacyById(Long id) {
        return pharmacyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pharmacie non trouvée avec l'ID: " + id));
    }
    
    @Override
    public Pharmacy updatePharmacy(Long id, Pharmacy pharmacy) {
        Pharmacy existingPharmacy = getPharmacyById(id);
        existingPharmacy.setNom(pharmacy.getNom());
        existingPharmacy.setAdresse(pharmacy.getAdresse());
        existingPharmacy.setTelephone(pharmacy.getTelephone());
        existingPharmacy.setSecteur(pharmacy.getSecteur());
        existingPharmacy.setPayed(pharmacy.isPayed());
        existingPharmacy.setIce(pharmacy.getIce());
        return pharmacyRepository.save(existingPharmacy);
    }
    
    @Override
    public void deletePharmacy(Long id) {
        pharmacyRepository.deleteById(id);
    }
    
    @Override
    public Map<String, Long> getPharmacyCountBySecteur() {
        Map<String, Long> countBySecteur = new HashMap<>();
        List<Secteur> secteurs = secteurRepository.findAll();
        for (Secteur secteur : secteurs) {
            long count = pharmacyRepository.findAll().stream()
                    .filter(pharmacy -> pharmacy.getSecteur() != null && 
                            pharmacy.getSecteur().getId().equals(secteur.getId()))
                    .count();
            countBySecteur.put(secteur.getNom(), count);
        }
        return countBySecteur;
    }
    
    // === GESTION DES PRODUITS ===
    
    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'ID: " + id));
    }
    
    @Override
    public Product createProduct(Product product) {
        product.setNom(product.getNom());
        return productRepository.save(product);
    }

    @Override
    public Product createProduct(Product product, java.util.List<Long> gammeIds) {
        product.setNom(product.getNom());
        if (gammeIds != null && !gammeIds.isEmpty()) {
            product.setGammes(gammeRepository.findAllById(gammeIds));
        } else {
            product.setGammes(java.util.Collections.emptyList());
        }
        return productRepository.save(product);
    }
    
    @Override
    public Product updateProduct(Long id, Product product) {
        Product existingProduct = getProductById(id);
        existingProduct.setNom(product.getNom());
        existingProduct.setCode(product.getCode());
        existingProduct.setPrix(product.getPrix());
        existingProduct.setQuantite(product.getQuantite());
        existingProduct.setGammes(product.getGammes());
        // updatedAt handled by auditing
        return productRepository.save(existingProduct);
    }

    @Override
    public Product updateProduct(Long id, Product product, java.util.List<Long> gammeIds) {
        Product existingProduct = getProductById(id);
        existingProduct.setNom(product.getNom());
        existingProduct.setCode(product.getCode());
        existingProduct.setPrix(product.getPrix());
        existingProduct.setQuantite(product.getQuantite());
        if (gammeIds != null) {
            existingProduct.setGammes(gammeRepository.findAllById(gammeIds));
        }
        // updatedAt handled by auditing
        return productRepository.save(existingProduct);
    }
    
    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
    
    // === GESTION DES OFFRES ===
    
    @Override
    public List<Offre> getAllOffres() {
        return offreRepository.findAll();
    }
    
    @Override
    public Offre getOffreById(Long id) {
        return offreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offre non trouvée avec l'ID: " + id));
    }
    
    @Override
    public void deleteOffre(Long id) {
        offreRepository.deleteById(id);
    }
    
    // === GESTION DES COMMANDES ===
    
    @Override
    public List<Commande> getAllCommandes() {
        return commandeRepository.findAll();
    }
    
    @Override
    public Commande getCommandeById(Long id) {
        return commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec l'ID: " + id));
    }
    
    @Override
    public void deleteCommande(Long id) {
        commandeRepository.deleteById(id);
    }
    
    @Override
    public Map<String, Long> getCommandeCountByStatus() {
        Map<String, Long> countByStatus = new HashMap<>();
        for (CommandeStatus status : CommandeStatus.values()) {
            long count = commandeRepository.findAll().stream()
                    .filter(commande -> commande.getStatus() == status)
                    .count();
            countByStatus.put(status.name(), count);
        }
        return countByStatus;
    }
    
    // === GESTION DES GAMMES ===
    
    @Override
    public List<Gamme> getAllGammes() {
        return gammeRepository.findAll();
    }
    
    @Override
    public Gamme getGammeById(Long id) {
        return gammeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gamme non trouvée avec l'ID: " + id));
    }
    
    @Override
    public Gamme createGamme(Gamme gamme) {
        return gammeRepository.save(gamme);
    }
    
    @Override
    public Gamme updateGamme(Long id, Gamme gamme) {
        Gamme existingGamme = gammeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gamme non trouvée avec l'ID: " + id));
        existingGamme.setNom(gamme.getNom());
        existingGamme.setDescription(gamme.getDescription());
        return gammeRepository.save(existingGamme);
    }
    
    @Override
    public void deleteGamme(Long id) {
        gammeRepository.deleteById(id);
    }
    
    // === GESTION DES SECTEURS ===
    
    @Override
    public List<Secteur> getAllSecteurs() {
        return secteurRepository.findAll();
    }
    
    @Override
    public Secteur getSecteurById(Long id) {
        return secteurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Secteur non trouvé avec l'ID: " + id));
    }
    
    @Override
    public Secteur createSecteur(Secteur secteur) {
        return secteurRepository.save(secteur);
    }
    
    @Override
    public Secteur updateSecteur(Long id, Secteur secteur) {
        Secteur existingSecteur = secteurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Secteur non trouvé avec l'ID: " + id));
        existingSecteur.setNom(secteur.getNom());
        return secteurRepository.save(existingSecteur);
    }
    
    @Override
    public void deleteSecteur(Long id) {
        secteurRepository.deleteById(id);
    }
    
    // === STATISTIQUES GLOBALES ===
    
    @Override
    public Map<String, Object> getGlobalStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Comptages
        stats.put("totalUsers", userRepository.count());
        stats.put("totalPharmacies", pharmacyRepository.count());
        stats.put("totalProducts", productRepository.count());
        stats.put("totalOffres", offreRepository.count());
        stats.put("totalCommandes", commandeRepository.count());
        stats.put("totalGammes", gammeRepository.count());
        stats.put("totalSecteurs", secteurRepository.count());
        
        // Répartition par rôle
        stats.put("usersByRole", getUserCountByRole());
        
        // Répartition par secteur
        stats.put("pharmaciesBySecteur", getPharmacyCountBySecteur());
        
        // Répartition par statut de commande
        stats.put("commandesByStatus", getCommandeCountByStatus());
        
        return stats;
    }
    
    @Override
    public Map<String, Long> getSystemOverview() {
        Map<String, Long> overview = new HashMap<>();
        overview.put("users", userRepository.count());
        overview.put("pharmacies", pharmacyRepository.count());
        overview.put("products", productRepository.count());
        overview.put("offres", offreRepository.count());
        overview.put("commandes", commandeRepository.count());
        overview.put("gammes", gammeRepository.count());
        overview.put("secteurs", secteurRepository.count());
        overview.put("admins", adminRepository.count());
        return overview;
    }
}
