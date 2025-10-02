package com.project.repository;

import com.project.entity.Secteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SecteurRepository extends JpaRepository<Secteur, Long> {
    Optional<Secteur> findByNomIgnoreCase(String nom);
}
// Ce repository gère les opérations CRUD pour l'entité Secteur.
// Il hérite de JpaRepository pour bénéficier des méthodes de base.
// Vous pouvez ajouter des méthodes personnalisées si nécessaire, par exemple pour trouver des secteurs par nom ou par région.
// Assurez-vous que l'entité Secteur est correctement définie dans votre projet.
// Vous pouvez également ajouter des annotations supplémentaires si vous avez besoin de fonctionnalités spécifiques, comme des requêtes personnalisées.
// N'oubliez pas de gérer les exceptions et les transactions si nécessaire dans votre service qui utilise ce repository.
