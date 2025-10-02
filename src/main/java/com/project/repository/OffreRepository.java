package com.project.repository;

import com.project.entity.Offre;
import java.util.List;
import java.time.LocalDate;

import com.project.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OffreRepository extends JpaRepository<Offre, Long> {
    List<Offre> findByCreatedByUser(User createdByUser);
    
    // Méthodes pour la pagination et séparation par date
    @Query("SELECT o FROM Offre o WHERE (o.createdByUser = :user OR o.createdByUsername = :username) AND o.dateFin >= :today ORDER BY o.dateDebut ASC")
    Page<Offre> findActiveOffresByUser(@Param("user") User user, @Param("username") String username, @Param("today") LocalDate today, Pageable pageable);
    
    @Query("SELECT o FROM Offre o WHERE (o.createdByUser = :user OR o.createdByUsername = :username) AND o.dateFin < :today ORDER BY o.dateDebut DESC")
    Page<Offre> findArchivedOffresByUser(@Param("user") User user, @Param("username") String username, @Param("today") LocalDate today, Pageable pageable);
    
    @Query("SELECT COUNT(o) FROM Offre o WHERE (o.createdByUser = :user OR o.createdByUsername = :username) AND o.dateFin >= :today")
    long countActiveOffresByUser(@Param("user") User user, @Param("username") String username, @Param("today") LocalDate today);
    
    @Query("SELECT COUNT(o) FROM Offre o WHERE (o.createdByUser = :user OR o.createdByUsername = :username) AND o.dateFin < :today")
    long countArchivedOffresByUser(@Param("user") User user, @Param("username") String username, @Param("today") LocalDate today);

    Page<Offre> findByCreatedByUserAndDateFinGreaterThanEqual(User createdByUser, LocalDate date, Pageable pageable);

    Page<Offre> findByCreatedByUserAndDateFinLessThan(User createdByUser, LocalDate date, Pageable pageable);

    long countByCreatedByUserAndDateFinGreaterThanEqual(User createdByUser, LocalDate date);

    long countByCreatedByUserAndDateFinLessThan(User createdByUser, LocalDate date);
}
