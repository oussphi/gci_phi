package com.project.controller;

import com.project.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/simple-test")
@PreAuthorize("hasRole('ADMIN')")
public class SimpleTestController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/basic")
    public String basicTest(Model model) {
        model.addAttribute("message", "Test basique réussi");
        return "admin/test";
    }

    @GetMapping("/data")
    public String dataTest(Model model) {
        try {
            // Test simple sans service
            model.addAttribute("message", "Test de données réussi");
            model.addAttribute("testData", "Données de test");
        } catch (Exception e) {
            model.addAttribute("error", "Erreur: " + e.getMessage());
        }
        return "admin/test";
    }

    @GetMapping("/service-test")
    public String serviceTest(Model model) {
        try {
            // Test avec le service AdminService
            int usersCount = adminService.getAllUsers().size();
            int rolesCount = adminService.getAllRoles().size();
            
            model.addAttribute("message", "Test du service réussi");
            model.addAttribute("usersCount", usersCount);
            model.addAttribute("rolesCount", rolesCount);
        } catch (Exception e) {
            model.addAttribute("error", "Erreur du service: " + e.getMessage());
            e.printStackTrace();
        }
        return "admin/test";
    }

    @GetMapping("/admin-users-test")
    public String adminUsersTest(Model model) {
        try {
            // Test qui reproduit exactement la logique du contrôleur AdminController
            model.addAttribute("users", adminService.getAllUsers());
            model.addAttribute("roles", adminService.getAllRoles());
            return "admin/users/list";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur dans adminUsersTest: " + e.getMessage());
            e.printStackTrace();
            return "admin/test";
        }
    }

    @GetMapping("/step-by-step-test")
    public String stepByStepTest(Model model) {
        try {
            // Test étape par étape
            model.addAttribute("step1", "Étape 1: Contrôleur accessible");
            
            // Test 1: Récupération des utilisateurs
            try {
                var users = adminService.getAllUsers();
                model.addAttribute("step2", "Étape 2: Utilisateurs récupérés (" + users.size() + ")");
            } catch (Exception e) {
                model.addAttribute("step2", "Étape 2: Erreur utilisateurs - " + e.getMessage());
                throw e;
            }
            
            // Test 2: Récupération des rôles
            try {
                var roles = adminService.getAllRoles();
                model.addAttribute("step3", "Étape 3: Rôles récupérés (" + roles.size() + ")");
            } catch (Exception e) {
                model.addAttribute("step3", "Étape 3: Erreur rôles - " + e.getMessage());
                throw e;
            }
            
            // Test 3: Ajout des attributs au modèle
            model.addAttribute("users", adminService.getAllUsers());
            model.addAttribute("roles", adminService.getAllRoles());
            model.addAttribute("step4", "Étape 4: Modèle préparé");
            
            return "admin/test";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur dans stepByStepTest: " + e.getMessage());
            e.printStackTrace();
            return "admin/test";
        }
    }
}
