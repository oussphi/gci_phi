package com.project.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.entity.*;
import com.project.repository.CommandeRepository;
import com.project.repository.ProductRepository;
import com.project.repository.OffreProduitRepository;
import com.project.repository.SecteurRepository;
import com.project.repository.UserRepository;
import com.project.repository.NotificationRepository;
import com.project.service.ExcelPharmacyImporter;
import com.project.service.OfferImportResult;
import com.project.service.OfferImportService;
import com.project.service.OffreService;
import com.project.service.PharmacyService;
import com.project.service.UserService;
import com.project.service.NotificationService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/OKACHA")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('OKACHA')")
public class OkachaController {

    @Autowired private UserService userService;
    @Autowired private CommandeRepository commandeRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private UserRepository userRepository;
    @Autowired private OffreService offreService;
    @Autowired private PharmacyService pharmacyService;
    @Autowired private SecteurRepository secteurRepository;
    @Autowired private ExcelPharmacyImporter excelPharmacyImporter;
    @Autowired private OfferImportService offerImportService;
    @Autowired private OffreProduitRepository offreProduitRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private com.project.service.CommandeService commandeService;
    @Autowired private com.project.repository.LigneCommandeRepository ligneCommandeRepository;

    @ModelAttribute
    public void injectNotifications(Model model, Principal principal) {
        try {
            if (principal != null) {
                User okacha = userRepository.findByUsername(principal.getName());
                if (okacha != null) {
                    long unread = notificationService.unreadCount(okacha);
                    model.addAttribute("unreadCount", unread);
                    model.addAttribute("latestNotifications", notificationService.latestFor(okacha));
                    model.addAttribute("notifBase", "/OKACHA");
                    model.addAttribute("currentUserId", okacha.getId());
                }
            }
        } catch (Exception ignored) {}
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("username", "OKACHA");
        return "OKACHA/dashboard";
    }

    @GetMapping("/notifications")
    public String voirNotifications(Model model, Principal principal) {
        User okacha = userRepository.findByUsername(principal.getName());
        model.addAttribute("notifications", notificationService.latestFor(okacha));
        return "OKACHA/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String marquerLue(@PathVariable Long id, Principal principal) {
        User okacha = userRepository.findByUsername(principal.getName());
        notificationService.markRead(id, okacha);
        return "redirect:/OKACHA/notifications";
    }

    @GetMapping("/notifications/{id}/open")
    public String ouvrirNotification(@PathVariable Long id, Principal principal) {
        User okacha = userRepository.findByUsername(principal.getName());
        return notificationRepository.findById(id)
                .map(n -> {
                    if (n.getRecipient() != null && okacha != null && okacha.getId().equals(n.getRecipient().getId())) {
                        if (!n.isReadFlag()) { n.setReadFlag(true); notificationRepository.save(n); }
                        if (n.getCommande() != null) {
                            return "redirect:/OKACHA/commandes/" + n.getCommande().getId();
                        }
                    }
                    return "redirect:/OKACHA/notifications";
                })
                .orElse("redirect:/OKACHA/notifications");
    }

    // NEW: Détails d'une commande
    @GetMapping("/commandes/{id}")
    public String detailsCommande(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Commande cmd = commandeRepo.findById(id).orElse(null);
        if (cmd == null) {
            ra.addFlashAttribute("error", "Commande introuvable");
            return "redirect:/OKACHA/commandes";
        }
        model.addAttribute("cmd", cmd);
        // Charger les lignes et les produits de manière sûre (évite LazyInitialization en vue)
        java.util.List<com.project.entity.LigneCommande> lcs = ligneCommandeRepository.findByCommandeId(cmd.getId());
        java.util.List<com.project.service.dto.LigneCommandeView> views = new java.util.ArrayList<>();
        for (var lc : lcs) {
            com.project.service.dto.LigneCommandeView v = new com.project.service.dto.LigneCommandeView();
            v.setId(lc.getId());
            try { v.setProduitNom(lc.getProduit() != null ? lc.getProduit().getNom() : "-"); } catch (Exception e) { v.setProduitNom("-"); }
            v.setQuantite(lc.getQuantite());
            v.setPrixUnitaire(lc.getPrixUnitaire());
            v.setStatutProduit(lc.getStatutProduit());
            v.setRupture(lc.getStatutProduit() == com.project.entity.LigneCommandeStatus.RUPTURE_STOCK);
            views.add(v);
        }
        model.addAttribute("lignes", views);
        return "OKACHA/commande-detail";
    }

    // Modification d'une commande par OKACHA (exclusion de lignes hors stock)
    @PostMapping(path = "/commandes/{id}/modifier", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> modifierCommande(@PathVariable Long id,
                                              @RequestBody com.project.service.dto.CommandeModifyRequest req,
                                              Principal principal) {
        try {
            if (req == null || req.getSelectedLineIds() == null) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Sélection requise"));
            }
            // Valider que les lignes appartiennent bien à cette commande
            Commande cmd = commandeRepo.findById(id).orElse(null);
            if (cmd == null) return ResponseEntity.status(404).body(java.util.Map.of("error", "Commande introuvable"));
            java.util.Set<Long> allowedIds = new java.util.HashSet<>();
            for (var lc : ligneCommandeRepository.findByCommandeId(id)) { if (lc.getId() != null) allowedIds.add(lc.getId()); }
            for (Long lid : req.getSelectedLineIds()) {
                if (lid == null || !allowedIds.contains(lid)) {
                    return ResponseEntity.badRequest().body(java.util.Map.of("error", "Ligne invalide: " + lid));
                }
            }

            Commande updated = commandeService.updateCommandeSelection(id, req.getSelectedLineIds());
            var resp = new com.project.service.dto.CommandeModifyResponse();
            resp.setCommandeId(updated.getId());
            resp.setTotalAvantModification(updated.getTotalAvantModification() != null ? updated.getTotalAvantModification() : 0.0);
            resp.setTotalApresModification(updated.getTotalApresModification() != null ? updated.getTotalApresModification() : 0.0);
            resp.setTotalQuantite(updated.getTotalQuantite());

            // Préparer et envoyer une notification détaillée au propriétaire (VM)
            try {
                java.util.Set<Long> selected = new java.util.HashSet<>(req.getSelectedLineIds());
                java.util.List<com.project.entity.LigneCommande> allLines = ligneCommandeRepository.findByCommandeId(id);
                java.util.List<String> removedNames = new java.util.ArrayList<>();
                for (var lc : allLines) {
                    if (lc.getId() != null && !selected.contains(lc.getId())) {
                        try { removedNames.add(lc.getProduit() != null ? lc.getProduit().getNom() : ("Ligne " + lc.getId())); } catch (Exception e) { removedNames.add("Ligne " + lc.getId()); }
                    }
                }
                String actor = principal != null ? principal.getName() : "OKACHA";
                String ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String details = removedNames.isEmpty() ? "Aucune ligne exclue" : ("Exclues: " + String.join(", ", removedNames));
                String title = "Commande modifiée";
                String message = String.format("%s a modifié la commande #%d le %s. %s. Total: %.2f → %.2f",
                        actor,
                        updated.getId(),
                        ts,
                        details,
                        updated.getTotalAvantModification() != null ? updated.getTotalAvantModification() : 0.0,
                        updated.getTotalApresModification() != null ? updated.getTotalApresModification() : updated.getTotalPrix());
                if (updated.getUser() != null) {
                    notificationService.notifyUser(updated.getUser(), updated, title, message);
                }
            } catch (Exception ignored) {}

            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", ex.getMessage()));
        }
    }

    // --- Update Commande Status ---
    @PostMapping("/commandes/{id}/status")
    public String updateCommandeStatus(@PathVariable Long id,
                                       @RequestParam("status") CommandeStatus status,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                       @RequestParam(required = false) Integer month,
                                       @RequestParam(required = false) Integer year,
                                       @RequestParam(required = false) Long userId,
                                       @RequestParam(required = false) CommandeStatus currentStatus,
                                       RedirectAttributes ra) {
        Commande cmd = commandeRepo.findById(id).orElse(null);
        if (cmd == null) {
            ra.addFlashAttribute("error", "Commande introuvable");
            return "redirect:/OKACHA/commandes";
        }
        cmd.setStatus(status);
        commandeRepo.save(cmd);
        ra.addFlashAttribute("success", "Statut mis à jour");
        
        // Construire l'URL de redirection en préservant les filtres
        StringBuilder redirectUrl = new StringBuilder("/OKACHA/commandes");
        boolean hasParams = false;
        
        if (startDate != null) {
            redirectUrl.append(hasParams ? "&" : "?").append("startDate=").append(startDate);
            hasParams = true;
        }
        if (endDate != null) {
            redirectUrl.append(hasParams ? "&" : "?").append("endDate=").append(endDate);
            hasParams = true;
        }
        if (month != null) {
            redirectUrl.append(hasParams ? "&" : "?").append("month=").append(month);
            hasParams = true;
        }
        if (year != null) {
            redirectUrl.append(hasParams ? "&" : "?").append("year=").append(year);
            hasParams = true;
        }
        if (userId != null) {
            redirectUrl.append(hasParams ? "&" : "?").append("userId=").append(userId);
            hasParams = true;
        }
        if (currentStatus != null) {
            redirectUrl.append(hasParams ? "&" : "?").append("status=").append(currentStatus);
            hasParams = true;
        }
        
        return "redirect:" + redirectUrl.toString();
    }

    @GetMapping("/commandes")
    public String historiqueCommandes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) CommandeStatus status,
            Model model) {

        List<Commande> commandes;
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            commandes = commandeRepo.findByUserWithFilters(user, startDate, endDate, month, year, status);
        } else {
            commandes = commandeRepo.findAllWithFilters(startDate, endDate, month, year, status);
        }

        // Calculer les statistiques pour les commandes filtrées
        long totalCommandes = commandes.size();
        long commandesCreees = commandes.stream()
                .filter(cmd -> CommandeStatus.CREATED.equals(cmd.getStatus()))
                .count();
        long commandesEnCours = commandes.stream()
                .filter(cmd -> CommandeStatus.EN_COURS.equals(cmd.getStatus()))
                .count();
        long commandesEnLivraison = commandes.stream()
                .filter(cmd -> CommandeStatus.EN_LIVRAISON.equals(cmd.getStatus()))
                .count();
        long commandesLivrees = commandes.stream()
                .filter(cmd -> CommandeStatus.LIVREE.equals(cmd.getStatus()))
                .count();

        // Logs de débogage
        System.out.println("=== DEBUG STATISTIQUES ===");
        System.out.println("Total commandes: " + totalCommandes);
        System.out.println("Commandes créées: " + commandesCreees);
        System.out.println("Commandes en cours: " + commandesEnCours);
        System.out.println("Commandes en livraison: " + commandesEnLivraison);
        System.out.println("Commandes livrées: " + commandesLivrees);
        System.out.println("Statut filtré: " + status);
        System.out.println("==========================");

        List<User> utilisateurs = commandeRepo.findDistinctUsers();
        
        // Ajouter les statistiques calculées en premier
        model.addAttribute("totalCommandes", totalCommandes);
        model.addAttribute("commandesCreees", commandesCreees);
        model.addAttribute("commandesEnCours", commandesEnCours);
        model.addAttribute("commandesEnLivraison", commandesEnLivraison);
        model.addAttribute("commandesLivrees", commandesLivrees);
        
        // Ajouter les autres attributs
        model.addAttribute("commandes", commandes);
        model.addAttribute("utilisateurs", utilisateurs);
        model.addAttribute("userId", userId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        model.addAttribute("status", status);
        model.addAttribute("statuses", CommandeStatus.values());
        
        return "OKACHA/commandes";
    }

    @GetMapping("/nouvelle-offre")
    public String afficherFormulaireNouvelleOffre(Model model) {
        model.addAttribute("offre", new Offre());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        List<Gamme> gammesUtilisateur = user.getGammes();
        List<Product> produitsAutorises = productRepo.findDistinctByGammesIn(gammesUtilisateur);
        model.addAttribute("produits", produitsAutorises);
        return "OKACHA/creer-offre";
    }

    @PostMapping("/nouvelle-offre")
    public String enregistrerNouvelleOffreAvecProduits(
            @ModelAttribute Offre offre,
            @RequestParam("produitIds") List<Long> produitIds,
            @RequestParam("prix") List<Double> prix,
            @RequestParam("remise") List<Double> remises,
            @RequestParam("min") List<Integer> quantiteMin,
            @RequestParam("max") List<Integer> quantiteMax,
            @RequestParam("colisage") List<Integer> colisages,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        User okacha = userRepository.findByUsername(principal.getName());
        offre.setCreatedByUser(okacha);
        offre.setDateCreation(LocalDateTime.now());

        List<Gamme> gammesUtilisateur = okacha.getGammes();
        if (gammesUtilisateur.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucune gamme associée à l'utilisateur.");
            return "redirect:/OKACHA/nouvelle-offre";
        }

        Gamme gammeAssociee = gammesUtilisateur.get(0);
        offre.setGamme(gammeAssociee);

        List<OffreProduit> listeProduits = new ArrayList<>();
        for (int i = 0; i < produitIds.size(); i++) {
            Product produit = productRepo.findById(produitIds.get(i)).orElseThrow();
            if (!produit.getGammes().contains(gammeAssociee)) {
                continue;
            }
            OffreProduit op = new OffreProduit();
            op.setOffre(offre);
            op.setProduit(produit);
            op.setPrix(prix.get(i));
            op.setRemisePourcent(remises.get(i).intValue());
            op.setQuantiteMin(quantiteMin.get(i));
            op.setQuantiteMax(quantiteMax.get(i));
            op.setColisage(colisages.get(i));
            listeProduits.add(op);
        }

        offre.setProduits(listeProduits);
        offreService.enregistrerOffre(offre);
        redirectAttributes.addFlashAttribute("success", "Offre enregistrée avec succès !");
        return "redirect:/OKACHA/dashboard";
    }

    @GetMapping("/mes-offres")
    public String afficherMesOffres(Model model, Principal principal) {
        User u = userRepository.findByUsername(principal.getName());
        List<Offre> mesOffres = offreService.getOffresCreeesPar(u);
        model.addAttribute("offres", mesOffres);
        return "OKACHA/mes-offres";
    }

    @GetMapping("/mes-offres/{id}")
    public String afficherDetailsOffre(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        Offre offre = offreService.getOffreById(id);
        User u = userRepository.findByUsername(principal.getName());
        if (!isOwner(offre, u)) {
            redirectAttributes.addFlashAttribute("error", "Vous n'avez pas accès à cette offre.");
            return "redirect:/OKACHA/mes-offres";
        }
        model.addAttribute("offre", offre);
        model.addAttribute("produits", offre.getProduits());
        return "OKACHA/detail-offre";
    }

    @GetMapping("/mes-offres/edit/{id}")
    public String afficherFormulaireModificationOffre(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        Offre offre = offreService.getOffreById(id);
        User u = userRepository.findByUsername(principal.getName());
        if (offre == null || !isOwner(offre, u)) {
            redirectAttributes.addFlashAttribute("error", "Vous n'avez pas accès à cette offre.");
            return "redirect:/OKACHA/mes-offres";
        }
        model.addAttribute("offre", offre);
        model.addAttribute("produits", productRepo.findAll());
        return "OKACHA/edit-offre";
    }

    @PostMapping("/mes-offres/edit/{id}")
    public String enregistrerModificationsOffre(@PathVariable Long id,
                                                @RequestParam("nom") String nom,
                                                @RequestParam("description") String description,
                                                @RequestParam(value = "dateDebut", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
                                                @RequestParam(value = "dateFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
                                                @RequestParam("active") boolean active,
                                                @RequestParam(value = "colisage", required = false) List<Integer> colisages,
                                                @RequestParam("produitIds") List<Long> produitIds,
                                                @RequestParam("prix") List<Double> prix,
                                                @RequestParam("remise") List<Integer> remises,
                                                @RequestParam("min") List<Integer> quantiteMin,
                                                @RequestParam("max") List<Integer> quantiteMax,
                                                Principal principal,
                                                RedirectAttributes redirectAttributes) {
        User u = userRepository.findByUsername(principal.getName());
        Offre offre = offreService.getOffreById(id);
        if (!isOwner(offre, u)) {
            redirectAttributes.addFlashAttribute("error", "Accès refusé.");
            return "redirect:/OKACHA/mes-offres";
        }
        offre.setNom(nom);
        offre.setDescription(description);
        if (dateDebut != null) {
            offre.setDateDebut(dateDebut);
        }
        if (dateFin != null) {
            offre.setDateFin(dateFin);
        }
        offre.setActive(active);
        // Reset des lignes existantes pour éviter les doublons
        offreProduitRepository.deleteByOffreId(offre.getId());

        List<OffreProduit> nouveauxProduits = new ArrayList<>();
        for (int i = 0; i < produitIds.size(); i++) {
            Product produit = productRepo.findById(produitIds.get(i)).orElseThrow();
            OffreProduit op = new OffreProduit();
            op.setOffre(offre);
            op.setProduit(produit);
            op.setPrix(prix.get(i));
            op.setRemisePourcent(remises.get(i));
            op.setQuantiteMin(quantiteMin.get(i));
            op.setQuantiteMax(quantiteMax.get(i));
            if (colisages != null && i < colisages.size()) {
                op.setColisage(colisages.get(i));
            }
            nouveauxProduits.add(op);
        }
        offre.setProduits(nouveauxProduits);
        offreService.enregistrerOffre(offre);
        redirectAttributes.addFlashAttribute("success", "Offre modifiée avec succès !");
        return "redirect:/OKACHA/mes-offres";
    }

    private boolean isOwner(Offre offre, User user) {
        if (offre == null || user == null) return false;
        try {
            if (offre.getCreatedByUser() != null && offre.getCreatedByUser().getId() != null && user.getId() != null
                    && offre.getCreatedByUser().getId().equals(user.getId())) {
                return true;
            }
        } catch (Exception ignored) {}
        try {
            if (offre.getCreatedByUsername() != null && user.getUsername() != null
                    && offre.getCreatedByUsername().equalsIgnoreCase(user.getUsername())) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    @GetMapping("/pharmacies")
    public String afficherListePharmacies(Model model) {
        model.addAttribute("pharmacies", pharmacyService.findAll());
        model.addAttribute("secteurs", secteurRepository.findAll());
        return "OKACHA/pharmacies";
    }

    @GetMapping("/pharmacies/nouvelle")
    public String formulaireNouvellePharmacy(Model model) {
        model.addAttribute("pharmacy", new Pharmacy());
        model.addAttribute("secteurs", secteurRepository.findAll());
        return "OKACHA/form-pharmacy";
    }

    @PostMapping("/pharmacies")
    public String enregistrerPharmacy(@ModelAttribute Pharmacy pharmacy,
                                      @RequestParam("secteur.id") Long secteurId,
                                      RedirectAttributes redirectAttributes) {
        Secteur secteur = secteurRepository.findById(secteurId)
                .orElseThrow(() -> new RuntimeException("Secteur non trouvé"));
        pharmacy.setSecteur(secteur);
        pharmacyService.save(pharmacy);
        redirectAttributes.addFlashAttribute("success", "Pharmacie enregistrée avec succès !");
        return "redirect:/OKACHA/pharmacies";
    }

    @GetMapping("/pharmacies/edit/{id}")
    public String modifierPharmacy(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Pharmacy pharmacy = pharmacyService.findById(id);
        if (pharmacy == null) {
            redirectAttributes.addFlashAttribute("error", "Pharmacie introuvable.");
            return "redirect:/OKACHA/pharmacies";
        }
        model.addAttribute("pharmacy", pharmacy);
        model.addAttribute("secteurs", secteurRepository.findAll());
        return "OKACHA/form-pharmacy";
    }

    @PostMapping("/pharmacies/edit/{id}")
    public String enregistrerModificationPharmacy(@PathVariable Long id,
                                                  @ModelAttribute Pharmacy pharmacy,
                                                  @RequestParam("secteur.id") Long secteurId,
                                                  RedirectAttributes redirectAttributes) {
        Secteur secteur = secteurRepository.findById(secteurId)
                .orElseThrow(() -> new RuntimeException("Secteur non trouvé"));
        pharmacy.setSecteur(secteur);
        pharmacy.setId(id);
        pharmacyService.save(pharmacy);
        redirectAttributes.addFlashAttribute("success", "Pharmacie mise à jour !");
        return "redirect:/OKACHA/pharmacies";
    }

    @GetMapping("/pharmacies/import")
    public String showImportPharmaciesForm() {
        return "OKACHA/import-pharmacies";
    }

    @GetMapping("/pharmacies/import/template")
    public ResponseEntity<InputStreamResource> downloadPharmaciesTemplate() {
        var bos = excelPharmacyImporter.generateTemplate();
        var bis = new java.io.ByteArrayInputStream(bos.toByteArray());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template_pharmacies.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(bis));
    }

    @PostMapping("/pharmacies/import")
    public String importPharmaciesFromExcel(@RequestParam("file") MultipartFile file, Model model) {
        try {
            var report = excelPharmacyImporter.importFromExcel(file);
            model.addAttribute("message",
                    "Import terminé : %d ajoutées, %d mises à jour, %d ignorées.".formatted(report.getInserted(), report.getUpdated(), report.getSkipped()));
            model.addAttribute("report", report.toPrettyString());
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de l’import : " + e.getMessage());
        }
        return "OKACHA/import-pharmacies";
    }

    @GetMapping("/export-commandes")
    public void exportToutesLesCommandes(HttpServletResponse response) throws IOException {
        List<Commande> commandes = commandeRepo.findAll();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=commandes-et-details.xlsx");
        Workbook workbook = new XSSFWorkbook();
        Sheet commandesSheet = workbook.createSheet("Commandes");
        Row cmdHeader = commandesSheet.createRow(0);
        cmdHeader.createCell(0).setCellValue("ID Commande");
        cmdHeader.createCell(1).setCellValue("Date");
        cmdHeader.createCell(2).setCellValue("VM");
        cmdHeader.createCell(3).setCellValue("Pharmacie");
        cmdHeader.createCell(4).setCellValue("ICE");
        cmdHeader.createCell(5).setCellValue("Secteur");
        cmdHeader.createCell(6).setCellValue("Offre");
        cmdHeader.createCell(7).setCellValue("Total Commande (après)");
        cmdHeader.createCell(8).setCellValue("Total avant modification");
        int cmdRowIdx = 1;
        for (Commande cmd : commandes) {
            Row row = commandesSheet.createRow(cmdRowIdx++);
            String secteurNom = (cmd.getPharmacy() != null && cmd.getPharmacy().getSecteur() != null)
                    ? cmd.getPharmacy().getSecteur().getNom() : "-";
            row.createCell(0).setCellValue(cmd.getId());
            row.createCell(1).setCellValue(cmd.getDateCommande() != null ? cmd.getDateCommande().toLocalDate().toString() : "-");
            row.createCell(2).setCellValue(cmd.getUser() != null ? cmd.getUser().getUsername() : "-");
            row.createCell(3).setCellValue(cmd.getPharmacy() != null ? cmd.getPharmacy().getNom() : "-");
            row.createCell(4).setCellValue(cmd.getPharmacy() != null ? cmd.getPharmacy().getIce() : "-");
            row.createCell(5).setCellValue(secteurNom);
            row.createCell(6).setCellValue(cmd.getOffre() != null ? cmd.getOffre().getNom() : "-");
            row.createCell(7).setCellValue(cmd.getTotalPrix());
            if (cmd.getTotalAvantModification() != null) {
                row.createCell(8).setCellValue(cmd.getTotalAvantModification());
            }
        }
        for (int i = 0; i <= 8; i++) commandesSheet.autoSizeColumn(i);
        Sheet lignesSheet = workbook.createSheet("LignesCommande");
        Row ligneHeader = lignesSheet.createRow(0);
        ligneHeader.createCell(0).setCellValue("ID LigneCommande");
        ligneHeader.createCell(1).setCellValue("ID Commande");
        ligneHeader.createCell(2).setCellValue("Produit");
        ligneHeader.createCell(3).setCellValue("Colisage");
        ligneHeader.createCell(4).setCellValue("Quantité");
        ligneHeader.createCell(5).setCellValue("Prix Unitaire");
        ligneHeader.createCell(6).setCellValue("Total Produit");
        ligneHeader.createCell(7).setCellValue("Statut Produit");
        int ligneRowIdx = 1;
        for (Commande cmd : commandes) {
            if (cmd.getLignes() == null || cmd.getLignes().isEmpty()) continue;
            java.util.Map<Long, Integer> colisageByProduct = new java.util.HashMap<>();
            try {
                if (cmd.getOffre() != null && cmd.getOffre().getId() != null) {
                    var ops = offreProduitRepository.findByOffreId(cmd.getOffre().getId());
                    for (var op : ops) {
                        if (op != null && op.getProduit() != null && op.getProduit().getId() != null) {
                            colisageByProduct.put(op.getProduit().getId(), op.getColisage());
                        }
                    }
                }
            } catch (Exception ignored) {}
            for (LigneCommande ligne : cmd.getLignes()) {
                Row row = lignesSheet.createRow(ligneRowIdx++);
                row.createCell(0).setCellValue(ligne.getId());
                row.createCell(1).setCellValue(cmd.getId());
                row.createCell(2).setCellValue(ligne.getProduit() != null ? ligne.getProduit().getNom() : "-");
                Long pid = (ligne.getProduit() != null ? ligne.getProduit().getId() : null);
                Integer col = (pid != null ? colisageByProduct.get(pid) : null);
                row.createCell(3).setCellValue(col != null ? col : 0);
                row.createCell(4).setCellValue(ligne.getQuantite());
                row.createCell(5).setCellValue(ligne.getPrixUnitaire());
                row.createCell(6).setCellValue(ligne.getQuantite() * ligne.getPrixUnitaire());
                String st = "";
                try {
                    st = (ligne.getStatutProduit() == null) ? "" : ligne.getStatutProduit().name();
                } catch (Exception ignored) {}
                row.createCell(7).setCellValue(st);
            }
        }
        for (int i = 0; i <= 7; i++) lignesSheet.autoSizeColumn(i);
        try (OutputStream os = response.getOutputStream()) {
            workbook.write(os);
        }
        workbook.close();
    }

    @GetMapping("/offres/import")
    public String pageImportOffres(Model model) {
        return "OKACHA/import-offres";
    }

    @PostMapping("/offres/import")
    public String importerOffres(@RequestParam("file") MultipartFile file,
                                 Principal principal,
                                 RedirectAttributes ra) {
        try {
            User u = userRepository.findByUsername(principal.getName());
            OfferImportResult result = offerImportService.importWorkbook(file, u);
            if (!result.getErrors().isEmpty()) {
                ra.addFlashAttribute("error", String.join("<br/>", result.getErrors()));
            }
            ra.addFlashAttribute("success",
                    "Import terminé : " + result.getOffresCreees() + " offre(s) créée(s), " +
                            result.getLignesTraitees() + " ligne(s) traitée(s).");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Échec import : " + ex.getMessage());
        }
        return "redirect:/OKACHA/offres/import";
    }

    @GetMapping("/offres/import/template")
    public void telechargerTemplate(HttpServletResponse response) throws IOException {
        byte[] xlsx = offerImportService.generateTemplate();
        String filename = URLEncoder.encode("template_import_offres.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename);
        response.getOutputStream().write(xlsx);
        response.flushBuffer();
    }

    @GetMapping("/statistiques")
    public String statistiques(Model model,
                               @RequestParam(required = false) Long gammeId,
                               @RequestParam(required = false) Long userId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                               Principal principal) {
        User u = userRepository.findByUsername(principal.getName());
        Long dsmId = u != null ? u.getId() : null;
        List<Gamme> gammes = u.getGammes();
        model.addAttribute("gammes", gammes);
        List<User> equipe = userRepository.findByManager(u);
        model.addAttribute("equipe", equipe);
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusMonths(6).withDayOfMonth(1);
        LocalDate end   = (endDate   != null) ? endDate   : LocalDate.now();
        LocalDateTime startDT = start.atStartOfDay();
        LocalDateTime endDT   = end.atTime(23,59,59);
        var byMonth   = commandeRepo.statsRevenueByMonth(startDT, endDT, gammeId, dsmId, userId);
        var bySecteur = commandeRepo.statsBySecteur(startDT, endDT, gammeId, dsmId, userId);
        var byVm      = commandeRepo.statsByVm(startDT, endDT, gammeId, dsmId, userId);
        var topClients= commandeRepo.topClients(startDT, endDT, gammeId, dsmId, userId);
        List<String> monthsLabels = new java.util.ArrayList<>();
        List<Double> monthsValues = new java.util.ArrayList<>();
        for (var r : byMonth) {
            String label = String.format("%02d/%d", r.getMonth(), r.getYear());
            monthsLabels.add(label);
            monthsValues.add(r.getTotal() != null ? r.getTotal() : 0.0);
        }
        List<String> secteurLabels = new java.util.ArrayList<>();
        List<Double> secteurValues = new java.util.ArrayList<>();
        for (var r : bySecteur) {
            secteurLabels.add(r.getLabel() != null ? r.getLabel() : "N/A");
            secteurValues.add(r.getTotal() != null ? r.getTotal() : 0.0);
        }
        List<String> vmLabels = new java.util.ArrayList<>();
        List<Double> vmValues = new java.util.ArrayList<>();
        for (var r : byVm) {
            vmLabels.add(r.getLabel() != null ? r.getLabel() : "N/A");
            vmValues.add(r.getTotal() != null ? r.getTotal() : 0.0);
        }
        List<String> clientLabels = new java.util.ArrayList<>();
        List<Double> clientValues = new java.util.ArrayList<>();
        for (var r : topClients) {
            clientLabels.add(r.getLabel() != null ? r.getLabel() : "N/A");
            clientValues.add(r.getTotal() != null ? r.getTotal() : 0.0);
        }
        double caTotal = monthsValues.stream().mapToDouble(Double::doubleValue).sum();
        int nbCmd = 0;
        try {
            nbCmd = commandeRepo.findAll().size();
        } catch (Exception ignored){}
        model.addAttribute("filtreGammeId", gammeId);
        model.addAttribute("filtreUserId", userId);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("monthsLabels", monthsLabels);
        model.addAttribute("monthsValues", monthsValues);
        model.addAttribute("secteurLabels", secteurLabels);
        model.addAttribute("secteurValues", secteurValues);
        model.addAttribute("vmLabels", vmLabels);
        model.addAttribute("vmValues", vmValues);
        model.addAttribute("clientLabels", clientLabels);
        model.addAttribute("clientValues", clientValues);
        model.addAttribute("caTotal", caTotal);
        model.addAttribute("nbCmd", nbCmd);
        return "OKACHA/statistiques";
    }
} 
