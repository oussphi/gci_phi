package com.project.service;

import com.project.entity.Secteur;
import com.project.repository.SecteurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecteurServiceImpl implements SecteurService {

    @Autowired
    private SecteurRepository secteurRepository;

    @Override
    public List<Secteur> findAll() {
        return secteurRepository.findAll();
    }
}
