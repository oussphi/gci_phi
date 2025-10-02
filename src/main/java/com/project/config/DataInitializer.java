package com.project.config;

import com.project.entity.Role;
import com.project.entity.User;
import com.project.repository.RoleRepository;
import com.project.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepo, UserRepository userRepo, PasswordEncoder encoder) {
        return args -> {
            // Create roles if they don't exist
            Role clientRole = roleRepo.findByName("ROLE_CLIENT");
            if (clientRole == null) {
                clientRole = new Role();
                clientRole.setName("ROLE_CLIENT");
                roleRepo.save(clientRole);
            }

            Role DSMRole = roleRepo.findByName("ROLE_DSM");
            if (DSMRole == null) {
                DSMRole = new Role();
                DSMRole.setName("ROLE_DSM");
                roleRepo.save(DSMRole);
            }

            Role grossisteRole = roleRepo.findByName("ROLE_GROSSISTE");
            if (grossisteRole == null) {
                grossisteRole = new Role();
                grossisteRole.setName("ROLE_GROSSISTE");
                roleRepo.save(grossisteRole);
            }

            Role usineRole = roleRepo.findByName("ROLE_USINE");
            if (usineRole == null) {
                usineRole = new Role();
                usineRole.setName("ROLE_USINE");
                roleRepo.save(usineRole);
            }

            // NEW: OKACHA role
            Role okachaRole = roleRepo.findByName("ROLE_OKACHA");
            if (okachaRole == null) {
                okachaRole = new Role();
                okachaRole.setName("ROLE_OKACHA");
                roleRepo.save(okachaRole);
            }

            // NEW: ADMIN role
            Role adminRole = roleRepo.findByName("ROLE_ADMIN");
            if (adminRole == null) {
                adminRole = new Role();
                adminRole.setName("ROLE_ADMIN");
                roleRepo.save(adminRole);
            }

            // Create test users if they don't exist
            if (userRepo.findByUsername("client") == null) {
                User client = new User();
                client.setUsername("client");
                client.setPassword(encoder.encode("client123"));
                client.setEnabled(true);
                client.setRole(clientRole);
                userRepo.save(client);
            }

            if (userRepo.findByUsername("DSM") == null) {
                User DSM = new User();
                DSM.setUsername("DSM");
                DSM.setPassword(encoder.encode("DSM123"));
                DSM.setEnabled(true);
                DSM.setRole(DSMRole);
                userRepo.save(DSM);
            }

            if (userRepo.findByUsername("grossiste") == null) {
                User grossiste = new User();
                grossiste.setUsername("grossiste");
                grossiste.setPassword(encoder.encode("grossiste123"));
                grossiste.setEnabled(true);
                grossiste.setRole(grossisteRole);
                userRepo.save(grossiste);
            }

            if (userRepo.findByUsername("usine") == null) {
                User usine = new User();
                usine.setUsername("usine");
                usine.setPassword(encoder.encode("usine123"));
                usine.setEnabled(true);
                usine.setRole(usineRole);
                userRepo.save(usine);
            }

            // NEW: default OKACHA user
            if (userRepo.findByUsername("okacha") == null) {
                User okacha = new User();
                okacha.setUsername("okacha");
                okacha.setPassword(encoder.encode("okacha123"));
                okacha.setEnabled(true);
                okacha.setRole(okachaRole);
                userRepo.save(okacha);
            }

            // NEW: default ADMIN user
            if (userRepo.findByUsername("admin") == null) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("admin123"));
                admin.setEnabled(true);
                admin.setRole(adminRole);
                userRepo.save(admin);
            }
        };
    }
}