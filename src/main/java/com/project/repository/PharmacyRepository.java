package com.project.repository;

import com.project.entity.Pharmacy;
import com.project.entity.Secteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import java.util.List;

@Repository
public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {
    List<Pharmacy> findBySecteurIn(List<Secteur> secteurs);
    List<Pharmacy> findByPayedFalse(); // Pharmacies non payées
    List<Pharmacy> findByPayedTrue();  // Pharmacies à jour
    Optional<Pharmacy> findByIce(String ice);

}
