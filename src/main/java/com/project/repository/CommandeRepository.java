package com.project.repository;

import com.project.entity.Commande;
import com.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.project.repository.projection.StatsLabelAmount;
import com.project.repository.projection.StatsMonth;
import com.project.repository.projection.StatsMonthCount;
import com.project.repository.projection.StatsLabelCount;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;


public interface CommandeRepository extends JpaRepository<Commande, Long> {

    // 🔹 Pour les DSMs : récupérer toutes les commandes avec filtres
    @Query("""
        SELECT c FROM Commande c
        WHERE (:startDate IS NULL OR DATE(c.dateCommande) >= :startDate)
        AND (:endDate IS NULL OR DATE(c.dateCommande) <= :endDate)
        AND (:month IS NULL OR FUNCTION('MONTH', c.dateCommande) = :month)
        AND (:year IS NULL OR FUNCTION('YEAR', c.dateCommande) = :year)
        AND (:status IS NULL OR c.status = :status)
        ORDER BY c.dateCommande DESC
    """)
    List<Commande> findAllWithFilters(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("month") Integer month,
        @Param("year") Integer year,
        @Param("status") com.project.entity.CommandeStatus status
    );

    @Query(value = """
        SELECT c FROM Commande c
        WHERE (:startDate IS NULL OR DATE(c.dateCommande) >= :startDate)
        AND (:endDate IS NULL OR DATE(c.dateCommande) <= :endDate)
        AND (:month IS NULL OR FUNCTION('MONTH', c.dateCommande) = :month)
        AND (:year IS NULL OR FUNCTION('YEAR', c.dateCommande) = :year)
        AND (:status IS NULL OR c.status = :status)
        ORDER BY c.dateCommande DESC
    """,
    countQuery = """
        SELECT COUNT(c) FROM Commande c
        WHERE (:startDate IS NULL OR DATE(c.dateCommande) >= :startDate)
        AND (:endDate IS NULL OR DATE(c.dateCommande) <= :endDate)
        AND (:month IS NULL OR FUNCTION('MONTH', c.dateCommande) = :month)
        AND (:year IS NULL OR FUNCTION('YEAR', c.dateCommande) = :year)
        AND (:status IS NULL OR c.status = :status)
    """)
    org.springframework.data.domain.Page<Commande> findAllWithFiltersPaged(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("month") Integer month,
        @Param("year") Integer year,
        @Param("status") com.project.entity.CommandeStatus status,
        org.springframework.data.domain.Pageable pageable
    );

    // 🔹 Pour filtrer par utilisateur spécifique
    @Query("""
        SELECT c FROM Commande c
        WHERE c.user = :user
        AND (:startDate IS NULL OR DATE(c.dateCommande) >= :startDate)
        AND (:endDate IS NULL OR DATE(c.dateCommande) <= :endDate)
        AND (:month IS NULL OR FUNCTION('MONTH', c.dateCommande) = :month)
        AND (:year IS NULL OR FUNCTION('YEAR', c.dateCommande) = :year)
        AND (:status IS NULL OR c.status = :status)
        ORDER BY c.dateCommande DESC
    """)
    List<Commande> findByUserWithFilters(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("month") Integer month,
        @Param("year") Integer year,
        @Param("status") com.project.entity.CommandeStatus status
    );

    @Query(value = """
        SELECT c FROM Commande c
        WHERE c.user = :user
        AND (:startDate IS NULL OR DATE(c.dateCommande) >= :startDate)
        AND (:endDate IS NULL OR DATE(c.dateCommande) <= :endDate)
        AND (:month IS NULL OR FUNCTION('MONTH', c.dateCommande) = :month)
        AND (:year IS NULL OR FUNCTION('YEAR', c.dateCommande) = :year)
        AND (:status IS NULL OR c.status = :status)
        ORDER BY c.dateCommande DESC
    """,
    countQuery = """
        SELECT COUNT(c) FROM Commande c
        WHERE c.user = :user
        AND (:startDate IS NULL OR DATE(c.dateCommande) >= :startDate)
        AND (:endDate IS NULL OR DATE(c.dateCommande) <= :endDate)
        AND (:month IS NULL OR FUNCTION('MONTH', c.dateCommande) = :month)
        AND (:year IS NULL OR FUNCTION('YEAR', c.dateCommande) = :year)
        AND (:status IS NULL OR c.status = :status)
    """)
    org.springframework.data.domain.Page<Commande> findByUserWithFiltersPaged(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("month") Integer month,
        @Param("year") Integer year,
        @Param("status") com.project.entity.CommandeStatus status,
        org.springframework.data.domain.Pageable pageable
    );

    // 🔹 Variante avancée pour VM: filtres par pharmacie (nom contient) et secteur
    @Query("""
        SELECT c FROM Commande c
        WHERE c.user = :user
        AND (:startDate IS NULL OR DATE(c.dateCommande) >= :startDate)
        AND (:endDate IS NULL OR DATE(c.dateCommande) <= :endDate)
        AND (:month IS NULL OR FUNCTION('MONTH', c.dateCommande) = :month)
        AND (:year IS NULL OR FUNCTION('YEAR', c.dateCommande) = :year)
        AND (:pharmacyName IS NULL OR LOWER(c.pharmacy.nom) LIKE LOWER(CONCAT('%', :pharmacyName, '%')))
        AND (:secteurId IS NULL OR c.pharmacy.secteur.id = :secteurId)
        ORDER BY c.dateCommande DESC
    """)
    List<Commande> findByUserWithFiltersAdvanced(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("month") Integer month,
        @Param("year") Integer year,
        @Param("pharmacyName") String pharmacyName,
        @Param("secteurId") Long secteurId
    );

    // 🔹 Récupérer tous les utilisateurs ayant passé au moins une commande
    @Query("SELECT DISTINCT c.user FROM Commande c WHERE c.user IS NOT NULL")
    List<User> findDistinctUsers();





     // ====== NOUVELLES REQUÊTES STATS ======

    /** CA par mois (filtrable) */
    @Query("""
        SELECT 
          FUNCTION('YEAR', c.dateCommande) AS year,
          FUNCTION('MONTH', c.dateCommande) AS month,
          SUM(c.totalPrix) AS total
        FROM Commande c
        WHERE c.dateCommande BETWEEN :start AND :end
          AND (:gammeId IS NULL OR c.offre.gamme.id = :gammeId)
          AND (:dsmId IS NULL OR c.user.manager.id = :dsmId)
          AND (:userId IS NULL OR c.user.id = :userId)
        GROUP BY FUNCTION('YEAR', c.dateCommande), FUNCTION('MONTH', c.dateCommande)
        ORDER BY year, month
    """)
    List<StatsMonth> statsRevenueByMonth(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("gammeId") Long gammeId,
            @Param("dsmId") Long dsmId,
            @Param("userId") Long userId
    );

    /** CA par secteur (on considère Secteur comme région) */
    @Query("""
        SELECT 
          COALESCE(c.pharmacy.secteur.nom, 'Sans secteur') AS label,
          SUM(c.totalPrix) AS total
        FROM Commande c
        WHERE c.dateCommande BETWEEN :start AND :end
          AND (:gammeId IS NULL OR c.offre.gamme.id = :gammeId)
          AND (:dsmId IS NULL OR c.user.manager.id = :dsmId)
          AND (:userId IS NULL OR c.user.id = :userId)
        GROUP BY c.pharmacy.secteur.nom
        ORDER BY total DESC
    """)
    List<StatsLabelAmount> statsBySecteur(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("gammeId") Long gammeId,
            @Param("dsmId") Long dsmId,
            @Param("userId") Long userId
    );

    /** CA par VM (membres de l’équipe) */
    @Query("""
        SELECT 
          c.user.username AS label,
          SUM(c.totalPrix) AS total
        FROM Commande c
        WHERE c.dateCommande BETWEEN :start AND :end
          AND (:gammeId IS NULL OR c.offre.gamme.id = :gammeId)
          AND (:dsmId IS NULL OR c.user.manager.id = :dsmId)
          AND (:userId IS NULL OR c.user.id = :userId)
        GROUP BY c.user.username
        ORDER BY total DESC
    """)
    List<StatsLabelAmount> statsByVm(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("gammeId") Long gammeId,
            @Param("dsmId") Long dsmId,
            @Param("userId") Long userId
    );

    /** Top clients (pharmacies) */
    @Query("""
        SELECT 
          c.pharmacy.nom AS label,
          SUM(c.totalPrix) AS total
        FROM Commande c
        WHERE c.dateCommande BETWEEN :start AND :end
          AND (:gammeId IS NULL OR c.offre.gamme.id = :gammeId)
          AND (:dsmId IS NULL OR c.user.manager.id = :dsmId)
          AND (:userId IS NULL OR c.user.id = :userId)
        GROUP BY c.pharmacy.nom
        ORDER BY total DESC
    """)
    List<StatsLabelAmount> topClients(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("gammeId") Long gammeId,
            @Param("dsmId") Long dsmId,
            @Param("userId") Long userId
    );

    /** Nombre de commandes par mois */
    @Query("""
        SELECT 
          FUNCTION('YEAR', c.dateCommande) AS year,
          FUNCTION('MONTH', c.dateCommande) AS month,
          COUNT(c) AS count
        FROM Commande c
        WHERE c.dateCommande BETWEEN :start AND :end
          AND (:gammeId IS NULL OR c.offre.gamme.id = :gammeId)
          AND (:dsmId IS NULL OR c.user.manager.id = :dsmId)
          AND (:userId IS NULL OR c.user.id = :userId)
        GROUP BY FUNCTION('YEAR', c.dateCommande), FUNCTION('MONTH', c.dateCommande)
        ORDER BY year, month
    """)
    List<StatsMonthCount> countOrdersByMonth(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("gammeId") Long gammeId,
            @Param("dsmId") Long dsmId,
            @Param("userId") Long userId
    );

    /** CA par offre */
    @Query("""
        SELECT 
          c.offre.nom AS label,
          SUM(c.totalPrix) AS total
        FROM Commande c
        WHERE c.dateCommande BETWEEN :start AND :end
          AND (:gammeId IS NULL OR c.offre.gamme.id = :gammeId)
          AND (:dsmId IS NULL OR c.user.manager.id = :dsmId)
          AND (:userId IS NULL OR c.user.id = :userId)
        GROUP BY c.offre.nom
        ORDER BY total DESC
    """)
    List<StatsLabelAmount> revenueByOffre(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("gammeId") Long gammeId,
            @Param("dsmId") Long dsmId,
            @Param("userId") Long userId
    );

    /** CA par gamme */
    @Query("""
        SELECT 
          c.offre.gamme.nom AS label,
          SUM(c.totalPrix) AS total
        FROM Commande c
        WHERE c.dateCommande BETWEEN :start AND :end
          AND (:gammeId IS NULL OR c.offre.gamme.id = :gammeId)
          AND (:dsmId IS NULL OR c.user.manager.id = :dsmId)
          AND (:userId IS NULL OR c.user.id = :userId)
        GROUP BY c.offre.gamme.nom
        ORDER BY total DESC
    """)
    List<StatsLabelAmount> revenueByGamme(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("gammeId") Long gammeId,
            @Param("dsmId") Long dsmId,
            @Param("userId") Long userId
    );

    /** Top produits (quantité totale) */
    @Query("""
        SELECT 
          p.nom AS label,
          SUM(l.quantite) AS total
        FROM LigneCommande l
        JOIN l.commande c
        JOIN l.produit p
        WHERE c.dateCommande BETWEEN :start AND :end
          AND (:gammeId IS NULL OR c.offre.gamme.id = :gammeId)
          AND (:dsmId IS NULL OR c.user.manager.id = :dsmId)
          AND (:userId IS NULL OR c.user.id = :userId)
        GROUP BY p.nom
        ORDER BY total DESC
    """)
    List<StatsLabelAmount> topProductsByQuantity(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("gammeId") Long gammeId,
            @Param("dsmId") Long dsmId,
            @Param("userId") Long userId
    );

    /** Répartition par statut de commande */
    @Query("""
        SELECT 
          CAST(c.status AS string) AS label,
          COUNT(c) AS count
        FROM Commande c
        WHERE c.dateCommande BETWEEN :start AND :end
          AND (:gammeId IS NULL OR c.offre.gamme.id = :gammeId)
          AND (:dsmId IS NULL OR c.user.manager.id = :dsmId)
          AND (:userId IS NULL OR c.user.id = :userId)
        GROUP BY c.status
        ORDER BY count DESC
    """)
    List<StatsLabelCount> statusBreakdown(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("gammeId") Long gammeId,
            @Param("dsmId") Long dsmId,
            @Param("userId") Long userId
    );

    /** Panier moyen par VM */
    @Query("""
        SELECT 
          c.user.username AS label,
          AVG(c.totalPrix) AS total
        FROM Commande c
        WHERE c.dateCommande BETWEEN :start AND :end
          AND (:gammeId IS NULL OR c.offre.gamme.id = :gammeId)
          AND (:dsmId IS NULL OR c.user.manager.id = :dsmId)
          AND (:userId IS NULL OR c.user.id = :userId)
        GROUP BY c.user.username
        ORDER BY total DESC
    """)
    List<StatsLabelAmount> averageBasketByVm(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("gammeId") Long gammeId,
            @Param("dsmId") Long dsmId,
            @Param("userId") Long userId
    );
}
