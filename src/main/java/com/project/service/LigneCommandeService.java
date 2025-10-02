package com.project.service;

import com.project.entity.LigneCommande;
import com.project.repository.LigneCommandeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LigneCommandeService {

    @Autowired
    private LigneCommandeRepository ligneCommandeRepository;

    public List<LigneCommande> getAllLignes() {
        return ligneCommandeRepository.findAll();
    }

    public LigneCommande save(LigneCommande ligne) {
        return ligneCommandeRepository.save(ligne);
    }

    public void deleteById(Long id) {
        ligneCommandeRepository.deleteById(id);
    }

    public LigneCommande getById(Long id) {
        return ligneCommandeRepository.findById(id).orElse(null);
    }
}
