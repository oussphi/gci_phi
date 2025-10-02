package com.project.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }

        return "login";
    }

    @GetMapping("/postLogin")
    public String postLogin(Authentication authentication) {
        if (authentication != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                switch (role) {
                    case "ROLE_CLIENT":
                        return "redirect:/dashboard/client";
                    case "ROLE_OKACHA":
                        return "redirect:/OKACHA/dashboard";
                    case "ROLE_DSM":
                        return "redirect:/dashboard/DSM";
                    case "ROLE_GROSSISTE":
                        return "redirect:/dashboard/grossiste";
                    case "ROLE_USINE":
                        return "redirect:/dashboard/usine";
                    case "ROLE_ADMIN":
                        return "redirect:/admin/dashboard";
                    default:
                        return "redirect:/accessDenied";
                }
            }
        }
        return "redirect:/login";
    }

    // Fallback GET logout handler to avoid 405 when a link hits /perform_logout
    @GetMapping("/perform_logout")
    public String logoutGet(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        try {
            if (authentication != null) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
            } else {
                request.getSession(false);
                if (request.getSession(false) != null) request.getSession(false).invalidate();
            }
        } catch (Exception ignored) {}
        return "redirect:/login?logout=true";
    }
}
