package com.project.service;

import com.project.entity.Gamme;
import com.project.entity.Offre;
import com.project.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OffreServiceImplTest {

    @Test
    void isOffreAccessibleForUser_respectsDatesActiveAndGamme() {
        OffreServiceImpl svc = new OffreServiceImpl();

        Gamme g = new Gamme();
        g.setId(1L);
        Offre o = new Offre();
        o.setActive(true);
        o.setDateDebut(LocalDate.now().minusDays(1));
        o.setDateFin(LocalDate.now().plusDays(1));
        o.setGamme(g);

        User u = new User();
        u.setGammes(List.of(g));

        assertTrue(svc.isOffreAccessibleForUser(o, u, LocalDate.now()));

        o.setActive(false);
        assertFalse(svc.isOffreAccessibleForUser(o, u, LocalDate.now()));
    }
}

