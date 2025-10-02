package com.project.service;

import com.project.service.impl.CommandeServiceImpl;
import com.project.service.port.NotificationPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class CommandeServiceTest {

    @Test
    void applyPaymentDiscount_twoPercentForCash() {
        OffreService offreStub = new OffreService() {
            // minimal stubs for compile; not used in this test
            public java.util.List<com.project.entity.Offre> getAllOffres(){return java.util.List.of();}
            public com.project.entity.Offre getOffreById(Long id){return null;}
            public java.util.List<com.project.entity.OffreProduit> getProduitsByOffre(Long offreId){return java.util.List.of();}
            public double getRemisePourProduit(Long offreId, Long produitId){return 0;}
            public double getPrixProduitDansOffre(Long offreId, Long produitId){return 0;}
            public void enregistrerOffre(com.project.entity.Offre o){}
            public java.util.List<com.project.entity.Offre> getOffresCreeesPar(com.project.entity.User u){return java.util.List.of();}
            public org.springframework.data.domain.Page<com.project.entity.Offre> getActiveOffresByUser(com.project.entity.User u, org.springframework.data.domain.Pageable p){return org.springframework.data.domain.Page.empty();}
            public org.springframework.data.domain.Page<com.project.entity.Offre> getArchivedOffresByUser(com.project.entity.User u, org.springframework.data.domain.Pageable p){return org.springframework.data.domain.Page.empty();}
            public long getActiveOffresCount(com.project.entity.User u){return 0;}
            public long getArchivedOffresCount(com.project.entity.User u){return 0;}
            public void toggleActive(Long id, com.project.entity.User owner, boolean active){}
            public void updateOffreDSM(Long id,String n,String d,java.time.LocalDate a,java.time.LocalDate b,boolean c, java.util.List<com.project.entity.OffreProduit> l){}
            public boolean isOffreAccessibleForUser(com.project.entity.Offre o, com.project.entity.User u, java.time.LocalDate t){return false;}
            public java.util.List<com.project.entity.Offre> findActiveOffresForUser(com.project.entity.User u){return java.util.List.of();}
            public void createOffre(com.project.entity.Offre o, java.util.List<com.project.service.offer.OffreProduitInput> p, com.project.entity.User owner){}
        };
        NotificationPort notif = (m,c,t,ms) -> {};
        CommandeService svc = new CommandeServiceImpl(null, offreStub, notif);

        BigDecimal total = new BigDecimal("100.00");
        BigDecimal discounted = svc.applyPaymentDiscount(total, "AU COMPTANT");
        assertEquals(new BigDecimal("98.00"), discounted);

        BigDecimal same = svc.applyPaymentDiscount(total, "A TERME");
        assertEquals(total, same);
    }
}
