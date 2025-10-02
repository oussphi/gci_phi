package com.project.controller;

import com.project.entity.User;
import com.project.repository.UserRepository;
import com.project.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired private NotificationService notificationService;
    @Autowired private UserRepository userRepository;

    @GetMapping("")
    public String redirectToRoleDashboard(Authentication auth) {
        if (auth != null && auth.getAuthorities() != null) {
            if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CLIENT"))) {
                return "redirect:/dashboard/client";
            } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DSM"))) {
                return "redirect:/dashboard/DSM";
            } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_OKACHA"))) {
                return "redirect:/dashboard/OKACHA";
            } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_GROSSISTE"))) {
                return "redirect:/dashboard/grossiste";
            } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USINE"))) {
                return "redirect:/dashboard/usine";
            } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
                return "redirect:/admin/dashboard";
            }
        }
        // Par défaut, rediriger vers le dashboard client
        return "redirect:/dashboard/client";
    }

    // Inject notifications for VM dashboard header only
    @ModelAttribute
    public void injectVmNotifications(Model model, Authentication auth) {
        try {
            if (auth != null && auth.getAuthorities() != null &&
                    auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CLIENT"))) {
                User u = userRepository.findByUsername(auth.getName());
                if (u != null) {
                    long unread = notificationService.unreadCount(u);
                    model.addAttribute("unreadCount", unread);
                    model.addAttribute("latestNotifications", notificationService.latestFor(u));
                    model.addAttribute("notifBase", "/client");
                    model.addAttribute("currentUserId", u.getId());
                }
            }
        } catch (Exception ignored) {}
    }

    // Inject notifications for DSM dashboard header
    @ModelAttribute
    public void injectDsmNotifications(Model model, Authentication auth) {
        try {
            if (auth != null && auth.getAuthorities() != null &&
                    auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DSM"))) {
                User u = userRepository.findByUsername(auth.getName());
                if (u != null) {
                    long unread = notificationService.unreadCount(u);
                    model.addAttribute("unreadCount", unread);
                    model.addAttribute("latestNotifications", notificationService.latestFor(u));
                    model.addAttribute("notifBase", "/DSM");
                    model.addAttribute("currentUserId", u.getId());
                }
            }
        } catch (Exception ignored) {}
    }

    @GetMapping("/client")
    public String clientDashboard(Model model, Authentication auth) {
        model.addAttribute("role", "ROLE_CLIENT");
        model.addAttribute("username", auth.getName());
        return "VM/dashboard";
    }

    @GetMapping("/DSM")
    public String DSMDashboard(Model model, Authentication auth) {
        model.addAttribute("role", "DSM");
        model.addAttribute("username", auth.getName());
        return "/DSM/dashboard";
    }

    @GetMapping("/OKACHA")
    public String OKACHADashboard(Model model, Authentication auth) {
        model.addAttribute("role", "OKACHA");
        model.addAttribute("username", auth.getName());
        return "/OKACHA/dashboard";
    }

    @GetMapping("/grossiste")
    public String grossisteDashboard(Model model, Authentication auth) {
        model.addAttribute("role", "GROSSISTE");
        model.addAttribute("username", auth.getName());
        return "dashboard-grossiste";
    }

    @GetMapping("/usine")
    public String usineDashboard(Model model, Authentication auth) {
        model.addAttribute("role", "USINE");
        model.addAttribute("username", auth.getName());
        return "dashboard-usine";
    }
}
