package com.project.service;

import com.project.entity.Pharmacy;
import java.util.List;

public interface PharmacyService {
    List<Pharmacy> findAll();
    Pharmacy findById(Long id);
    Pharmacy save(Pharmacy pharmacy);
    void deleteById(Long id);
    /** Helper to fetch pharmacies by allowed secteurs */
    List<Pharmacy> findBySecteurs(java.util.List<com.project.entity.Secteur> secteurs);
}
