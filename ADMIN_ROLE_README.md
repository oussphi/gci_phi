# Rôle Administrateur - Ph.I

## Vue d'ensemble

Le rôle **ADMIN** a été créé pour offrir une gestion complète et centralisée du système Ph.I. Ce rôle dispose de tous les droits sur l'ensemble du système et peut gérer tous les aspects de l'application.

## Accès et Sécurité

- **URL d'accès** : `/admin/**`
- **Sécurité** : `@PreAuthorize("hasRole('ADMIN')")`
- **Redirection automatique** : Les utilisateurs avec le rôle ADMIN sont automatiquement redirigés vers `/admin/dashboard`

## Utilisateur par défaut

- **Username** : `admin`
- **Password** : `admin123`
- **Rôle** : `ROLE_ADMIN`

## Fonctionnalités disponibles

### 1. Dashboard Administrateur (`/admin/dashboard`)
- **Statistiques globales** : Vue d'ensemble de tous les éléments du système
- **Compteurs** : Utilisateurs, pharmacies, produits, commandes, gammes, secteurs
- **Liens rapides** : Accès direct à toutes les sections de gestion

### 2. Gestion des Administrateurs (`/admin/admins`)
- **CRUD complet** : Créer, lire, modifier, supprimer des administrateurs
- **Gestion des statuts** : Activer/désactiver des comptes admin
- **Gestion des permissions** : Définir les super-administrateurs

### 3. Gestion des Utilisateurs (`/admin/users`)
- **Vue d'ensemble** : Liste de tous les utilisateurs du système
- **Modification** : Changer les rôles, mots de passe, statuts
- **Statistiques** : Répartition des utilisateurs par rôle
- **Actions** : Activer/désactiver, supprimer des comptes

### 4. Gestion des Rôles (`/admin/roles`)
- **CRUD des rôles** : Créer, modifier, supprimer des rôles système
- **Gestion des permissions** : Définir les droits d'accès

### 5. Gestion des Pharmacies (`/admin/pharmacies`)
- **Vue complète** : Toutes les pharmacies du système
- **Modification** : Mettre à jour les informations, secteurs
- **Statistiques** : Répartition par secteur géographique
- **Actions** : Supprimer des pharmacies

### 6. Gestion des Produits (`/admin/products`)
- **Catalogue complet** : Tous les produits du système
- **CRUD** : Créer, modifier, supprimer des produits
- **Gestion des gammes** : Associer/désassocier des gammes
- **Gestion des prix** : Modifier les tarifs et quantités

### 7. Gestion des Offres (`/admin/offres`)
- **Vue d'ensemble** : Toutes les offres actives et inactives
- **Détails complets** : Produits, prix, remises, validité
- **Actions** : Supprimer des offres

### 8. Gestion des Commandes (`/admin/commandes`)
- **Historique complet** : Toutes les commandes du système
- **Statistiques** : Répartition par statut (Créée, En cours, En livraison, Livrée)
- **Actions** : Supprimer des commandes

### 9. Gestion des Gammes (`/admin/gammes`)
- **CRUD complet** : Créer, modifier, supprimer des gammes de produits
- **Gestion des produits** : Associer/désassocier des produits

### 10. Gestion des Secteurs (`/admin/secteurs`)
- **CRUD complet** : Créer, modifier, supprimer des secteurs géographiques
- **Gestion des pharmacies** : Associer/désassocier des pharmacies

## Architecture technique

### Entités créées
- **`Admin`** : Entité pour les administrateurs système
- **`AdminRepository`** : Interface de persistance
- **`AdminService`** : Interface de service avec toutes les méthodes de gestion
- **`AdminServiceImpl`** : Implémentation complète du service
- **`AdminController`** : Contrôleur REST avec tous les endpoints

### Sécurité
- **Spring Security** : Protection de tous les endpoints `/admin/**`
- **Rôle requis** : `ROLE_ADMIN`
- **Redirection automatique** : Intégration avec le système de dashboard

### Base de données
- **Table `admin`** : Stockage des informations administrateur
- **Relations** : Intégration avec le système de rôles existant

## Utilisation

### 1. Connexion
```bash
# Se connecter avec le compte admin par défaut
Username: admin
Password: admin123
```

### 2. Navigation
- **Dashboard** : `/admin/dashboard` - Vue d'ensemble du système
- **Utilisateurs** : `/admin/users` - Gestion des comptes utilisateur
- **Pharmacies** : `/admin/pharmacies` - Gestion des pharmacies
- **Produits** : `/admin/products` - Gestion du catalogue
- **Commandes** : `/admin/commandes` - Suivi des commandes
- **Gammes** : `/admin/gammes` - Gestion des catégories
- **Secteurs** : `/admin/secteurs` - Gestion géographique

### 3. Actions principales
- **Créer** : Nouveaux éléments via les formulaires
- **Modifier** : Édition des éléments existants
- **Supprimer** : Suppression avec confirmation
- **Activer/Désactiver** : Gestion des statuts

## Maintenance

### Ajout d'un nouvel administrateur
1. Aller sur `/admin/admins/new`
2. Remplir le formulaire avec les informations
3. Le mot de passe sera automatiquement chiffré
4. Le compte sera créé avec le rôle ADMIN

### Modification des permissions
- **Super-admin** : Accès complet à tous les aspects
- **Admin standard** : Accès à la gestion du système
- **Rôles personnalisés** : Possibilité de créer des rôles spécifiques

## Sécurité et bonnes pratiques

- **Changement de mot de passe** : Modifier le mot de passe par défaut après la première connexion
- **Audit** : Toutes les actions sont tracées dans les logs
- **Sauvegarde** : Effectuer des sauvegardes régulières de la base de données
- **Monitoring** : Surveiller les accès et actions des administrateurs

## Support et développement

Pour toute question ou amélioration du rôle administrateur :
- Vérifier les logs d'application
- Consulter la documentation Spring Security
- Tester les fonctionnalités dans un environnement de développement

---

**Note** : Ce rôle donne un accès complet au système. Utilisez-le avec précaution et assurez-vous que seules les personnes autorisées y ont accès.
