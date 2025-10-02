package com.project.controller;

import com.project.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/test")
@PreAuthorize("hasRole('ADMIN')")
public class TestController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/admin-data")
    public String testAdminData(Model model) {
        try {
            // Test des méthodes du service
            model.addAttribute("usersCount", adminService.getAllUsers().size());
            model.addAttribute("rolesCount", adminService.getAllRoles().size());
            model.addAttribute("pharmaciesCount", adminService.getAllPharmacies().size());
            model.addAttribute("success", "Données récupérées avec succès");
        } catch (Exception e) {
            model.addAttribute("error", "Erreur: " + e.getMessage());
            e.printStackTrace();
        }
        return "admin/test";
    }
}
