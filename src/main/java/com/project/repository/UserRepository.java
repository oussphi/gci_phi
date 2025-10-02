package com.project.repository;

import com.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);

        // membres de l'équipe (si champ manager présent)
    List<User> findByManager(User manager);

    // Récupérer les utilisateurs par nom de rôle (ex: "ROLE_OKACHA")
    java.util.List<User> findByRole_Name(String name);
}
