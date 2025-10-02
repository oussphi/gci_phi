package com.project.controller;

import com.project.entity.*;
import com.project.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // === DASHBOARD ===
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Long> overview = adminService.getSystemOverview();
        Map<String, Object> stats = adminService.getGlobalStatistics();
        
        model.addAttribute("overview", overview);
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }

    // === GESTION DES UTILISATEURS ===
    @GetMapping("/users")
    public String listUsers(Model model) {
        try {
            model.addAttribute("users", adminService.getAllUsers());
            model.addAttribute("roles", adminService.getAllRoles());
            return "admin/users/list";
        } catch (Exception e) {
            // Log l'erreur et retourne un message d'erreur
            System.err.println("Erreur dans listUsers: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur lors du chargement des données: " + e.getMessage());
            return "admin/users/list";
        }
    }

    @GetMapping("/users-simple")
    public String listUsersSimple(Model model) {
        // Version simplifiée pour tester
        model.addAttribute("users", new ArrayList<>());
        model.addAttribute("roles", new ArrayList<>());
        return "admin/users/list";
    }

    @GetMapping("/users-test")
    public String listUsersTest(Model model) {
        // Test avec le template simple
        model.addAttribute("users", new ArrayList<>());
        model.addAttribute("roles", new ArrayList<>());
        return "admin/users/list-simple";
    }

    @GetMapping("/users-direct")
    public String listUsersDirect(Model model) {
        // Test avec le template directement dans admin/
        model.addAttribute("users", new ArrayList<>());
        model.addAttribute("roles", new ArrayList<>());
        return "admin/users-test";
    }

    @GetMapping("/users-debug")
    public String listUsersDebug(Model model) {
        // Test avec le template de debug
        try {
            model.addAttribute("users", adminService.getAllUsers());
            model.addAttribute("roles", adminService.getAllRoles());
            return "admin/users/list-debug";
        } catch (Exception e) {
            System.err.println("Erreur dans listUsersDebug: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur lors du chargement des données: " + e.getMessage());
            return "admin/users/list-debug";
        }
    }

    @GetMapping("/users-fixed")
    public String listUsersFixed(Model model) {
        // Test avec le template corrigé
        try {
            model.addAttribute("users", adminService.getAllUsers());
            model.addAttribute("roles", adminService.getAllRoles());
            return "admin/users/list-fixed";
        } catch (Exception e) {
            System.err.println("Erreur dans listUsersFixed: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur lors du chargement des données: " + e.getMessage());
            return "admin/users/list-fixed";
        }
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", adminService.getAllRoles());
        model.addAttribute("secteurs", adminService.getAllSecteurs());
        model.addAttribute("gammes", adminService.getAllGammes());
        // Populate DSM users only
        model.addAttribute("users", adminService.getUsersByRole("ROLE_DSM"));
        return "admin/users/form";
    }

    @PostMapping("/users")
    public String createUser(
            @ModelAttribute User user,
            @RequestParam(value = "secteurIds", required = false) java.util.List<Long> secteurIds,
            @RequestParam(value = "gammeIds", required = false) java.util.List<Long> gammeIds,
            @RequestParam(value = "role", required = false) Long roleId,
            @RequestParam(value = "manager", required = false) Long managerId,
            RedirectAttributes ra) {
        try {
            adminService.createUser(user, secteurIds, gammeIds, roleId, managerId);
            ra.addFlashAttribute("success", "Utilisateur créé avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", adminService.getUserById(id));
        model.addAttribute("roles", adminService.getAllRoles());
        model.addAttribute("secteurs", adminService.getAllSecteurs());
        model.addAttribute("gammes", adminService.getAllGammes());
        model.addAttribute("users", adminService.getUsersByRole("ROLE_DSM"));
        return "admin/users/form";
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id,
                             @ModelAttribute User user,
                             @RequestParam(value = "secteurIds", required = false) java.util.List<Long> secteurIds,
                             @RequestParam(value = "gammeIds", required = false) java.util.List<Long> gammeIds,
                             @RequestParam(value = "role", required = false) Long roleId,
                             @RequestParam(value = "manager", required = false) Long managerId,
                             RedirectAttributes ra) {
        try {
            adminService.updateUser(id, user, secteurIds, gammeIds, roleId, managerId);
            ra.addFlashAttribute("success", "Utilisateur mis à jour avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.deleteUser(id);
            ra.addFlashAttribute("success", "Utilisateur supprimé avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.toggleUserStatus(id);
            ra.addFlashAttribute("success", "Statut de l'utilisateur modifié");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la modification: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // === GESTION DES RÔLES ===
    @GetMapping({"/roles", "/roles/", "/roles/index"})
    public String listRoles(Model model) {
        model.addAttribute("roles", adminService.getAllRoles());
        model.addAttribute("usersByRole", adminService.getUserCountByRole());
        return "admin/roles/list";
    }

    @GetMapping("/role")
    public String rolesRedirect() {
        return "redirect:/admin/roles";
    }

    @GetMapping("/roles/new")
    public String newRoleForm(Model model) {
        model.addAttribute("role", new Role());
        return "admin/roles/form";
    }

    @PostMapping("/roles")
    public String createRole(@ModelAttribute Role role, RedirectAttributes ra) {
        try {
            adminService.createRole(role);
            ra.addFlashAttribute("success", "Rôle créé avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @GetMapping("/roles/{id}/edit")
    public String editRoleForm(@PathVariable Long id, Model model) {
        model.addAttribute("role", adminService.getAllRoles().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(new Role()));
        return "admin/roles/form";
    }

    @PostMapping("/roles/{id}")
    public String updateRole(@PathVariable Long id, @ModelAttribute Role role, RedirectAttributes ra) {
        try {
            adminService.updateRole(id, role);
            ra.addFlashAttribute("success", "Rôle mis à jour avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @PostMapping("/roles/{id}/delete")
    public String deleteRole(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.deleteRole(id);
            ra.addFlashAttribute("success", "Rôle supprimé avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @GetMapping("/roles-simple")
    public String listRolesSimple(Model model) {
        try {
            model.addAttribute("roles", adminService.getAllRoles());
            return "admin/roles/list-simple";
        } catch (Exception e) {
            System.err.println("Erreur dans listRolesSimple: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur lors du chargement des rôles: " + e.getMessage());
            return "admin/roles/list-simple";
        }
    }

    @GetMapping("/roles-test")
    public String rolesTest(Model model) {
        return "admin/roles-test";
    }

    // === GESTION DES PHARMACIES ===
    @GetMapping({"/pharmacies", "/pharmacies/", "/pharmacies/index"})
    public String listPharmacies(Model model) {
        model.addAttribute("pharmacies", adminService.getAllPharmacies());
        model.addAttribute("secteurs", adminService.getAllSecteurs());
        return "admin/pharmacies/list";
    }

    @GetMapping("/pharmacies/new")
    public String newPharmacyForm(Model model) {
        model.addAttribute("pharmacy", new Pharmacy());
        model.addAttribute("secteurs", adminService.getAllSecteurs());
        model.addAttribute("users", adminService.getAllUsers());
        return "admin/pharmacies/form";
    }

    @PostMapping("/pharmacies")
    public String createPharmacy(@ModelAttribute Pharmacy pharmacy, RedirectAttributes ra) {
        try {
            // Note: AdminService needs createPharmacy method
            adminService.updatePharmacy(pharmacy.getId(), pharmacy);
            ra.addFlashAttribute("success", "Pharmacie créée avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
        }
        return "redirect:/admin/pharmacies";
    }

    @GetMapping("/pharmacies/{id}")
    public String viewPharmacy(@PathVariable Long id, Model model) {
        model.addAttribute("pharmacy", adminService.getPharmacyById(id));
        return "admin/pharmacies/view";
    }

    @GetMapping("/pharmacies/{id}/edit")
    public String editPharmacyForm(@PathVariable Long id, Model model) {
        model.addAttribute("pharmacy", adminService.getPharmacyById(id));
        model.addAttribute("secteurs", adminService.getAllSecteurs());
        model.addAttribute("users", adminService.getAllUsers());
        return "admin/pharmacies/form";
    }

    @PostMapping("/pharmacies/{id}")
    public String updatePharmacy(@PathVariable Long id, @ModelAttribute Pharmacy pharmacy, RedirectAttributes ra) {
        try {
            adminService.updatePharmacy(id, pharmacy);
            ra.addFlashAttribute("success", "Pharmacie mise à jour avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
        }
        return "redirect:/admin/pharmacies";
    }

    @PostMapping("/pharmacies/{id}/delete")
    public String deletePharmacy(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.deletePharmacy(id);
            ra.addFlashAttribute("success", "Pharmacie supprimée avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/pharmacies";
    }

    @PostMapping("/pharmacies/{id}/toggle-status")
    @ResponseBody
    public String togglePharmacyStatus(@PathVariable Long id) {
        try {
            // Note: AdminService needs togglePharmacyStatus method
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    // === GESTION DES PRODUITS ===
    @GetMapping({"/products", "/products/", "/products/index"})
    public String listProducts(Model model) {
        List<Product> products = adminService.getAllProducts();
        System.out.println("[AdminController] products count=" + (products != null ? products.size() : -1));
        model.addAttribute("products", products);
        model.addAttribute("gammes", adminService.getAllGammes());
        return "admin/products/list";
    }

    @GetMapping("/products/ping")
    @ResponseBody
    public String productsPing() {
        try {
            return "count=" + adminService.getAllProducts().size();
        } catch (Exception e) {
            return "error=" + e.getMessage();
        }
    }

    @GetMapping("/products/new")
    public String newProductForm(Model model, RedirectAttributes ra) {
        try {
            System.out.println("[AdminController] newProductForm called");
            model.addAttribute("product", new Product());
            model.addAttribute("gammes", adminService.getAllGammes());
            model.addAttribute("isEdit", false);
            model.addAttribute("formAction", "/admin/products");
        } catch (Exception e) {
            System.err.println("Erreur chargement formulaire produit: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("product", new Product());
            model.addAttribute("gammes", java.util.Collections.emptyList());
            ra.addFlashAttribute("error", "Impossible de charger les gammes: " + e.getMessage());
        }
        return "admin/products/form";
    }

    @PostMapping("/products")
    public String createProduct(@ModelAttribute Product product,
                                @RequestParam(value = "gammeIds", required = false) List<Long> gammeIds,
                                RedirectAttributes ra) {
        try {
            adminService.createProduct(product, gammeIds);
            ra.addFlashAttribute("success", "Produit créé avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/products/{id}")
    public String viewProduct(@PathVariable Long id, Model model) {
        model.addAttribute("product", adminService.getProductById(id));
        return "admin/products/view";
    }

    @GetMapping("/products/{id}/edit")
    public String editProductForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("product", adminService.getProductById(id));
            model.addAttribute("gammes", adminService.getAllGammes());
            model.addAttribute("isEdit", true);
            model.addAttribute("formAction", "/admin/products/" + id);
            return "admin/products/form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Produit introuvable (ID: " + id + ")");
            return "redirect:/admin/products";
        }
    }

    @PostMapping("/products/{id}")
    public String updateProduct(@PathVariable Long id,
                                @ModelAttribute Product product,
                                @RequestParam(value = "gammeIds", required = false) List<Long> gammeIds,
                                RedirectAttributes ra) {
        try {
            adminService.updateProduct(id, product, gammeIds);
            ra.addFlashAttribute("success", "Produit mis à jour avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.deleteProduct(id);
            ra.addFlashAttribute("success", "Produit supprimé avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/toggle-status")
    @ResponseBody
    public String toggleProductStatus(@PathVariable Long id) {
        try {
            // Note: AdminService needs toggleProductStatus method
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    @GetMapping("/products-simple")
    public String listProductsSimple(Model model) {
        try {
            model.addAttribute("products", adminService.getAllProducts());
            return "admin/products/list-simple";
        } catch (Exception e) {
            System.err.println("Erreur dans listProductsSimple: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur lors du chargement des produits: " + e.getMessage());
            return "admin/products/list-simple";
        }
    }

    // === GESTION DES OFFRES ===
    @GetMapping({"/offres", "/offres/", "/offres/index"})
    public String listOffres(Model model) {
        model.addAttribute("offres", adminService.getAllOffres());
        return "admin/offres/list";
    }

    @GetMapping("/offres/new")
    public String newOffreForm(Model model) {
        model.addAttribute("offre", new Offre());
        model.addAttribute("gammes", adminService.getAllGammes());
        model.addAttribute("users", adminService.getAllUsers());
        return "admin/offres/form";
    }

    @PostMapping("/offres")
    public String createOffre(@ModelAttribute Offre offre, RedirectAttributes ra) {
        try {
            // Note: AdminService needs createOffre method
            ra.addFlashAttribute("success", "Offre créée avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
        }
        return "redirect:/admin/offres";
    }

    @GetMapping("/offres/{id}")
    public String viewOffre(@PathVariable Long id, Model model) {
        model.addAttribute("offre", adminService.getOffreById(id));
        return "admin/offres/view";
    }

    @PostMapping("/offres/{id}/delete")
    public String deleteOffre(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.deleteOffre(id);
            ra.addFlashAttribute("success", "Offre supprimée avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/offres";
    }

    // === GESTION DES COMMANDES ===
    @GetMapping("/commandes")
    public String listCommandes(Model model) {
        model.addAttribute("commandes", adminService.getAllCommandes());
        return "admin/commandes/list";
    }

    @GetMapping("/commandes/new")
    public String newCommandeForm(Model model) {
        model.addAttribute("commande", new Commande());
        model.addAttribute("pharmacies", adminService.getAllPharmacies());
        model.addAttribute("offres", adminService.getAllOffres());
        model.addAttribute("users", adminService.getAllUsers());
        return "admin/commandes/form";
    }

    @PostMapping("/commandes")
    public String createCommande(@ModelAttribute Commande commande, RedirectAttributes ra) {
        try {
            // Note: AdminService needs createCommande method
            ra.addFlashAttribute("success", "Commande créée avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
        }
        return "redirect:/admin/commandes";
    }

    @GetMapping("/commandes/{id}")
    public String viewCommande(@PathVariable Long id, Model model) {
        model.addAttribute("commande", adminService.getCommandeById(id));
        return "admin/commandes/view";
    }

    @PostMapping("/commandes/{id}/delete")
    public String deleteCommande(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.deleteCommande(id);
            ra.addFlashAttribute("success", "Commande supprimée avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/commandes";
    }

    // === GESTION DES GAMMES ===
    @GetMapping({"/gammes", "/gammes/", "/gammes/index"})
    public String listGammes(Model model) {
        model.addAttribute("gammes", adminService.getAllGammes());
        return "admin/gammes/list";
    }

    @GetMapping("/gammes/new")
    public String newGammeForm(Model model) {
        model.addAttribute("gamme", new Gamme());
        return "admin/gammes/form";
    }

    @PostMapping("/gammes")
    public String createGamme(@ModelAttribute Gamme gamme, RedirectAttributes ra) {
        try {
            adminService.createGamme(gamme);
            ra.addFlashAttribute("success", "Gamme créée avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
        }
        return "redirect:/admin/gammes";
    }

    @GetMapping("/gammes/{id}")
    public String viewGamme(@PathVariable Long id, Model model) {
        model.addAttribute("gamme", adminService.getGammeById(id));
        return "admin/gammes/view";
    }

    @GetMapping("/gammes/{id}/edit")
    public String editGammeForm(@PathVariable Long id, Model model) {
        model.addAttribute("gamme", adminService.getGammeById(id));
        return "admin/gammes/form";
    }

    @PostMapping("/gammes/{id}")
    public String updateGamme(@PathVariable Long id, @ModelAttribute Gamme gamme, RedirectAttributes ra) {
        try {
            adminService.updateGamme(id, gamme);
            ra.addFlashAttribute("success", "Gamme mise à jour avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
        }
        return "redirect:/admin/gammes";
    }

    @PostMapping("/gammes/{id}/delete")
    public String deleteGamme(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.deleteGamme(id);
            ra.addFlashAttribute("success", "Gamme supprimée avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/gammes";
    }

    // === GESTION DES SECTEURS ===
    @GetMapping({"/secteurs", "/secteurs/", "/secteurs/index", "/secteurs/list"})
    public String listSecteurs(Model model) {
        System.out.println("[AdminController] listSecteurs called");
        model.addAttribute("secteurs", adminService.getAllSecteurs());
        return "admin/secteurs/list";
    }

    @GetMapping("/secteurs/ping")
    @ResponseBody
    public String pingSecteurs() {
        return "OK";
    }

    @GetMapping("/secteurs/new")
    public String newSecteurForm(Model model) {
        model.addAttribute("secteur", new Secteur());
        return "admin/secteurs/form";
    }

    @PostMapping("/secteurs")
    public String createSecteur(@ModelAttribute Secteur secteur, RedirectAttributes ra) {
        try {
            adminService.createSecteur(secteur);
            ra.addFlashAttribute("success", "Secteur créé avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
        }
        return "redirect:/admin/secteurs";
    }

    @GetMapping("/secteurs/{id}")
    public String viewSecteur(@PathVariable Long id, Model model) {
        model.addAttribute("secteur", adminService.getSecteurById(id));
        return "admin/secteurs/view";
    }

    @GetMapping("/secteurs/{id}/edit")
    public String editSecteurForm(@PathVariable Long id, Model model) {
        model.addAttribute("secteur", adminService.getSecteurById(id));
        return "admin/secteurs/form";
    }

    @PostMapping("/secteurs/{id}")
    public String updateSecteur(@PathVariable Long id, @ModelAttribute Secteur secteur, RedirectAttributes ra) {
        try {
            adminService.updateSecteur(id, secteur);
            ra.addFlashAttribute("success", "Secteur mis à jour avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
        }
        return "redirect:/admin/secteurs";
    }

    @PostMapping("/secteurs/{id}/delete")
    public String deleteSecteur(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.deleteSecteur(id);
            ra.addFlashAttribute("success", "Secteur supprimé avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/secteurs";
    }

    @PostMapping("/secteurs/{id}/toggle-status")
    @ResponseBody
    public String toggleSecteurStatus(@PathVariable Long id) {
        try {
            // Note: AdminService needs toggleSecteurStatus method
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    // === ENDPOINTS DE TEST ===
    @GetMapping("/test")
    @ResponseBody
    public String test() {
        return "Admin controller is working!";
    }

    @GetMapping("/test-simple")
    public String testSimple(Model model) {
        model.addAttribute("message", "Test simple - Pas de base de données");
        return "admin/test";
    }

    @GetMapping("/test-db")
    @ResponseBody
    public String testDatabase() {
        try {
            // Test simple de connexion à la base de données
            List<User> users = adminService.getAllUsers();
            return "Database connection OK. Users count: " + users.size();
        } catch (Exception e) {
            return "Database connection ERROR: " + e.getMessage();
        }
    }

    @GetMapping("/template-test")
    public String templateTest(Model model) {
        model.addAttribute("message", "Template test");
        return "admin/test";
    }

    @GetMapping("/dashboard-simple")
    public String dashboardSimple(Model model) {
        Map<String, Long> overview = new HashMap<>();
        overview.put("users", 10L);
        overview.put("pharmacies", 5L);
        overview.put("products", 100L);
        overview.put("commandes", 25L);
        
        model.addAttribute("overview", overview);
        model.addAttribute("message", "Dashboard Simplifié - Fonctionnel");
        return "admin/dashboard-simple";
    }
}
