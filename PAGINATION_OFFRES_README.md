# Pagination et Séparation des Offres par Date - DSM

## Vue d'ensemble

Cette implémentation ajoute la pagination et la séparation automatique des offres par date pour le module DSM. Les offres sont automatiquement séparées en deux catégories :

- **Offres Actives** : Offres dont la date de fin est supérieure ou égale à aujourd'hui
- **Offres Archivées** : Offres dont la date de fin est antérieure à aujourd'hui

## Fonctionnalités

### 1. Séparation Automatique par Date
- Les offres sont automatiquement classées selon leur date de fin
- Utilisation de `LocalDate.now()` pour déterminer la date d'aujourd'hui
- Logique : `dateFin >= aujourd'hui` = Active, `dateFin < aujourd'hui` = Archivée

### 2. Pagination
- Pagination configurable avec tailles de page : 5, 10, 20, 50
- Navigation entre les pages (Précédent/Suivant)
- Affichage du numéro de page actuel
- Compteurs d'offres par catégorie

### 3. Interface Utilisateur
- Onglets pour basculer entre offres actives et archives
- Indicateurs visuels du statut des offres
- Informations contextuelles sur le type d'offres affichées
- Tableau responsive avec formatage des dates

## Structure Technique

### Repository (`OffreRepository.java`)
```java
// Offres actives (dateFin >= aujourd'hui)
Page<Offre> findActiveOffresByUser(User user, LocalDate today, Pageable pageable);

// Offres archivées (dateFin < aujourd'hui)
Page<Offre> findArchivedOffresByUser(User user, LocalDate today, Pageable pageable);

// Compteurs
long countActiveOffresByUser(User user, LocalDate today);
long countArchivedOffresByUser(User user, LocalDate today);
```

### Service (`OffreService.java` et `OffreServiceImpl.java`)
```java
// Méthodes de pagination
Page<Offre> getActiveOffresByUser(User user, Pageable pageable);
Page<Offre> getArchivedOffresByUser(User user, Pageable pageable);

// Compteurs
long getActiveOffresCount(User user);
long getArchivedOffresCount(User user);
```

### Contrôleur (`DSMController.java`)
```java
@GetMapping("/mes-offres")
public String afficherMesOffres(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "active") String type,
    Model model, 
    Principal principal)
```

### Utilitaire (`OffreDateUtil.java`)
```java
public static boolean isOffreActive(LocalDate dateFin)
public static boolean isOffreArchived(LocalDate dateFin)
public static LocalDate getToday()
public static String formatDate(LocalDate date)
```

## URL et Paramètres

### URL de base
```
http://localhost:8081/DSM/mes-offres
```

### Paramètres de requête
- `page` : Numéro de page (défaut: 0)
- `size` : Taille de page (défaut: 10, options: 5, 10, 20, 50)
- `type` : Type d'offres (défaut: "active", options: "active", "archived")

### Exemples d'URL
```
/DSM/mes-offres?page=0&size=20&type=active
/DSM/mes-offres?page=1&size=10&type=archived
/DSM/mes-offres?type=archived
```

## Logs et Debug

Le service inclut des logs détaillés pour faciliter le débogage :
- Nombre d'offres trouvées par catégorie
- Nombre de pages générées
- Utilisateur concerné et date de référence

## Améliorations Futures Possibles

1. **Filtres avancés** : Ajout de filtres par gamme, secteur, etc.
2. **Tri personnalisé** : Tri par nom, date de création, etc.
3. **Recherche** : Barre de recherche textuelle
4. **Export** : Export des offres en CSV/Excel
5. **Notifications** : Alertes pour les offres expirant bientôt

## Tests

Pour tester la fonctionnalité :

1. Créer des offres avec des dates de fin différentes
2. Accéder à `/DSM/mes-offres`
3. Vérifier la séparation automatique des offres
4. Tester la pagination avec différentes tailles de page
5. Basculer entre les onglets actifs/archives

## Dépendances

- Spring Boot 3.x
- Spring Data JPA
- Thymeleaf
- Tailwind CSS
- Java 17+
