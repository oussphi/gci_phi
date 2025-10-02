package com.project.repository;

import com.project.entity.Gamme;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface GammeRepository extends JpaRepository<Gamme, Long> {
    Optional<Gamme> findByNomIgnoreCase(String nom);
}
