package com.project.service;

import com.project.entity.Pharmacy;
import com.project.entity.Secteur;
import com.project.repository.PharmacyRepository;
import com.project.service.PharmacyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PharmacyServiceImpl implements PharmacyService {

    private final PharmacyRepository pharmacyRepo;

    @Autowired
    public PharmacyServiceImpl(PharmacyRepository pharmacyRepo) {
        this.pharmacyRepo = pharmacyRepo;
    }

    @Override
    public List<Pharmacy> findAll() {
        return pharmacyRepo.findAll();
    }

    @Override
    public Pharmacy findById(Long id) {
        return pharmacyRepo.findById(id).orElse(null);
    }

    @Override
    public Pharmacy save(Pharmacy pharmacy) {
        return pharmacyRepo.save(pharmacy);
    }

    @Override
    public void deleteById(Long id) {
        pharmacyRepo.deleteById(id);
    }

    @Override
    public java.util.List<Pharmacy> findBySecteurs(java.util.List<Secteur> secteurs) {
        if (secteurs == null || secteurs.isEmpty()) return java.util.Collections.emptyList();
        return pharmacyRepo.findBySecteurIn(secteurs);
    }
}
