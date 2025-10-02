package com.project.util;

import java.time.LocalDate;

/**
 * Classe utilitaire pour gérer la logique de séparation des offres par date
 */
public class OffreDateUtil {
    
    /**
     * Détermine si une offre est active (date de fin >= aujourd'hui)
     * @param dateFin la date de fin de l'offre
     * @return true si l'offre est active, false sinon
     */
    public static boolean isOffreActive(LocalDate dateFin) {
        LocalDate today = LocalDate.now();
        return dateFin != null && !dateFin.isBefore(today);
    }
    
    /**
     * Détermine si une offre est archivée (date de fin < aujourd'hui)
     * @param dateFin la date de fin de l'offre
     * @return true si l'offre est archivée, false sinon
     */
    public static boolean isOffreArchived(LocalDate dateFin) {
        return !isOffreActive(dateFin);
    }
    
    /**
     * Obtient la date d'aujourd'hui
     * @return la date d'aujourd'hui
     */
    public static LocalDate getToday() {
        return LocalDate.now();
    }
    
    /**
     * Formate une date en format français (dd/MM/yyyy)
     * @param date la date à formater
     * @return la date formatée
     */
    public static String formatDate(LocalDate date) {
        if (date == null) return "N/A";
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
