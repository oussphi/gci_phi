package com.project.controller;

import com.project.entity.User;
import com.project.entity.Role;
import com.project.repository.RoleRepository;
import com.project.repository.UserRepository;
import com.project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Autowired
    public UserController(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          UserService userService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    // Créer un nouveau client (DSM uniquement)
    @PostMapping("/create-client")
    @PreAuthorize("hasRole('DSM')") // DSM uniquement
    public ResponseEntity<?> createClient(@RequestParam String username, @RequestParam String password) {

        if (userRepository.findByUsername(username) != null) {
            return ResponseEntity.badRequest().body("Utilisateur déjà existant");
        }

        User u = new User();
        u.setUsername(username);
        u.setPassword(password); // will be encoded in service
        userService.createClientUser(u);
        return ResponseEntity.ok("Utilisateur client créé avec succès");
    }
}
