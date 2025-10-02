package com.project.service;

import com.project.entity.Product;
import com.project.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProduitById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID: " + id));
    }

    public java.util.List<com.project.entity.Product> findAllowedForGammes(java.util.List<com.project.entity.Gamme> gammes) {
        if (gammes == null || gammes.isEmpty()) return java.util.Collections.emptyList();
        return productRepository.findDistinctByGammesIn(gammes);
    }
} 
