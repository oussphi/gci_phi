package com.project.service;

import com.project.entity.Gamme;
import com.project.entity.User;
import com.project.repository.GammeRepository;
import com.project.repository.UserRepository;
import com.project.service.GammeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GammeServiceImpl implements GammeService {

    @Autowired private GammeRepository gammeRepo;
    @Autowired private UserRepository userRepo;

    @Override
    public List<Gamme> findAll() {
        return gammeRepo.findAll();
    }

    @Override
    public Gamme save(Gamme gamme) {
        return gammeRepo.save(gamme);
    }

    @Override
    public Gamme findById(Long id) {
        return gammeRepo.findById(id).orElse(null);
    }

    @Override
    public void deleteById(Long id) {
        gammeRepo.deleteById(id);
    }

    @Override
    public boolean userHasAccessToGamme(Long userId, Long gammeId) {
        User user = userRepo.findById(userId).orElse(null);
        return user != null && user.getGammes().stream().anyMatch(g -> g.getId().equals(gammeId));
    }
}
