package com.project.config;

import com.project.entity.Role;
import com.project.entity.User;
import com.project.repository.RoleRepository;
import com.project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AzureOidcUserService extends OidcUserService {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    private static final String ALLOWED_DOMAIN = "phi.ma";
    @Value("${app.security.admin-emails:}")
    private String adminEmailsProp;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);
        String email = resolveEmail(oidcUser);
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Adresse email introuvable dans le profil Microsoft");
        }
        email = email.toLowerCase();
        if (!email.endsWith("@" + ALLOWED_DOMAIN)) {
            throw new RuntimeException("Domaine email non autorisé");
        }

        User user = userRepository.findByUsername(email);
        if (user == null) {
            user = new User();
            user.setUsername(email);
            user.setPassword("{noop}oauth");
            user.setEnabled(true);
            Role clientRole = roleRepository.findByName("ROLE_CLIENT");
            if (clientRole == null) throw new RuntimeException("ROLE_CLIENT manquant");
            user.setRole(clientRole);
        }
        if (isAdminEmail(email)) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN");
            if (adminRole == null) throw new RuntimeException("ROLE_ADMIN manquant");
            user.setRole(adminRole);
        }
        userRepository.save(user);

        String roleName = user.getRole() != null ? user.getRole().getName() : "ROLE_CLIENT";
        return new org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser(
                List.of(new SimpleGrantedAuthority(roleName)),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo()
        );
    }

    private boolean isAdminEmail(String email) {
        if (email == null || email.isBlank()) return false;
        if (adminEmailsProp == null || adminEmailsProp.isBlank()) {
            return "oussama.harouach@phi.ma".equalsIgnoreCase(email);
        }
        for (String part : adminEmailsProp.split(",")) {
            String candidate = part == null ? null : part.trim();
            if (candidate != null && !candidate.isEmpty() && email.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String resolveEmail(OidcUser oidcUser) {
        String directEmail = oidcUser.getEmail();
        if (directEmail != null && !directEmail.isBlank()) {
            return directEmail;
        }

        String preferredUsername = oidcUser.getAttribute("preferred_username");
        if (preferredUsername != null && preferredUsername.contains("@")) {
            return preferredUsername;
        }

        String upn = oidcUser.getAttribute("upn");
        if (upn != null && upn.contains("@")) {
            return upn;
        }

        String uniqueName = oidcUser.getAttribute("unique_name");
        if (uniqueName != null && uniqueName.contains("@")) {
            return uniqueName;
        }

        String mail = oidcUser.getAttribute("mail");
        if (mail != null && !mail.isBlank()) {
            return mail;
        }

        List<String> emails = oidcUser.getAttribute("emails");
        if (emails != null) {
            for (String e : emails) {
                if (e != null && !e.isBlank()) {
                    return e;
                }
            }
        }

        return null;
    }
}
