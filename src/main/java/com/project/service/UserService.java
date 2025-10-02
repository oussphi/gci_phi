package com.project.service;

import com.project.entity.User;
import com.project.repository.UserRepository;
import com.project.repository.RoleRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createClientUser(User user) {
        System.out.println("Username reçu: " + user.getUsername());
    
        var role = roleRepository.findByName("ROLE_CLIENT");
        if (role == null) {
            throw new RuntimeException("Le rôle ROLE_CLIENT est introuvable !");
        }
    
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);
        user.setRole(role);
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        if (username == null) return null;
        return userRepository.findByUsername(username);
    }
    
    public java.util.List<User> findByManager(User manager) {
        if (manager == null) return java.util.Collections.emptyList();
        return userRepository.findByManager(manager);
    }

    public User findById(Long id) {
        if (id == null) return null;
        return userRepository.findById(id).orElse(null);
    }
    
}
