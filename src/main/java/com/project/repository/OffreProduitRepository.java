package com.project.repository;

import com.project.entity.OffreProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// AJOUTER:
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface OffreProduitRepository extends JpaRepository<OffreProduit, Long> {
    List<OffreProduit> findByOffreId(Long offreId);

    // IMPORTANT : annotation @Modifying (+ @Transactional)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    long deleteByOffreId(Long offreId);
}