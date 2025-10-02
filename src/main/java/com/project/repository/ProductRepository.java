package com.project.repository;

import com.project.entity.Gamme;
import com.project.entity.Product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findDistinctByGammesIn(List<Gamme> gammes);
    Optional<Product> findByCode(String code);
    Optional<Product> findByNomIgnoreCase(String nom);
    


}