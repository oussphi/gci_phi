package com.project.service;

import com.project.entity.Gamme;
import java.util.List;

public interface GammeService {
    List<Gamme> findAll();
    Gamme save(Gamme gamme);
    Gamme findById(Long id);
    void deleteById(Long id);
    boolean userHasAccessToGamme(Long userId, Long gammeId);
}
