package com.project.controller;
import com.project.entity.*;
import com.project.repository.CommandeRepository;
import com.project.repository.ProductRepository;
import com.project.repository.OffreProduitRepository;
import com.project.repository.OffreRepository;
import com.project.repository.PharmacyRepository;
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
import com.project.service.BonCommandePdfService;
import com.project.service.CommandeService;
import com.project.service.ProductService;
import com.project.service.dto.OrderLineInput;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.net.URLEncoder;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Controller
@RequestMapping("/DSM")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('DSM')")
public class DSMController {

    @Autowired private UserService userService;
    @Autowired private CommandeService commandeService;
    @Autowired private ProductService productService;
    @Autowired private CommandeRepository commandeRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private UserRepository userRepository;
    @Autowired private OffreService offreService;
    @Autowired private PharmacyService pharmacyService;
    @Autowired private SecteurRepository secteurRepository;    // === DASHBOARD ===
    @Autowired private ExcelPharmacyImporter excelPharmacyImporter;
    @Autowired private OfferImportService offerImportService;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private OffreProduitRepository offreProduitRepository;
    @Autowired private PharmacyRepository pharmacyRepo;
    @Autowired private OffreRepository offreRepo;
    @Autowired private BonCommandePdfService bonCommandePdfService;
    @Autowired private com.project.repository.LigneCommandeRepository ligneCommandeRepository;

    @ModelAttribute
    public void injectNotifications(Model model, Principal principal) {
        try {
            if (principal != null) {
                User dsm = userRepository.findByUsername(principal.getName());
                if (dsm != null) {
                    long unread = notificationService.unreadCount(dsm);
                    model.addAttribute("unreadCount", unread);
                    model.addAttribute("latestNotifications", notificationService.latestFor(dsm));
                    model.addAttribute("notifBase", "/DSM");
                    model.addAttribute("currentUserId", dsm.getId());
                }
            }
        } catch (Exception ignored) {}
    }

    @GetMapping("/notifications")
    public String voirNotifications(Model model, Principal principal) {
        User dsm = userRepository.findByUsername(principal.getName());
        model.addAttribute("notifications", notificationService.latestFor(dsm));
        return "DSM/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String marquerLue(@PathVariable Long id, Principal principal) {
        User dsm = userRepository.findByUsername(principal.getName());
        notificationService.markRead(id, dsm);
        return "redirect:/DSM/notifications";
    }

    @GetMapping("/notifications/{id}/open")
    public String ouvrirNotification(@PathVariable Long id, Principal principal) {
        User dsm = userRepository.findByUsername(principal.getName());
        return notificationRepository.findById(id)
                .map(n -> {
                    if (n.getRecipient() != null && dsm != null && dsm.getId().equals(n.getRecipient().getId())) {
                        if (!n.isReadFlag()) { n.setReadFlag(true); notificationRepository.save(n); }
                        if (n.getCommande() != null) {
                            return "redirect:/DSM/commandes/" + n.getCommande().getId();
                        }
                    }
                    return "redirect:/DSM/notifications";
                })
                .orElse("redirect:/DSM/notifications");
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("username", "DSM");
        return "DSM/dashboard";
    }

    // === DÉTAIL COMMANDE (DSM) ===
    @GetMapping("/commandes/{id}")
    public String dsmDetailsCommande(@PathVariable Long id, Model model, Principal principal, RedirectAttributes ra) {
        Commande cmd = commandeService.getById(id);
        if (cmd == null) {
            ra.addFlashAttribute("error", "Commande introuvable");
            return "redirect:/DSM/commandes";
        }
        // contrôle: le DSM doit être manager du VM
        User dsm = userService.findByUsername(principal.getName());
        if (cmd.getUser() == null || cmd.getUser().getManager() == null || !cmd.getUser().getManager().getId().equals(dsm.getId())) {
            ra.addFlashAttribute("error", "Accès refusé à cette commande");
            return "redirect:/DSM/commandes";
        }

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
        model.addAttribute("cmd", cmd);
        model.addAttribute("lignes", views);
        return "DSM/commande-detail";
    }

    // === COMMANDES AVEC FILTRES ===
    @GetMapping("/commandes")
    public String historiqueCommandes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, name = "status") CommandeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(Math.max(page,0), Math.max(size,1), org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "dateCommande"));
        org.springframework.data.domain.Page<Commande> commandesPage;
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            commandesPage = commandeRepo.findByUserWithFiltersPaged(user, startDate, endDate, month, year, status, pageable);
        } else {
            commandesPage = commandeRepo.findAllWithFiltersPaged(startDate, endDate, month, year, status, pageable);
        }

        List<User> utilisateurs = commandeRepo.findDistinctUsers();
        model.addAttribute("commandes", commandesPage.getContent());
        model.addAttribute("utilisateurs", utilisateurs);
        model.addAttribute("userId", userId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        model.addAttribute("status", status);
        // Pagination attributes
        model.addAttribute("page", commandesPage.getNumber());
        model.addAttribute("size", commandesPage.getSize());
        model.addAttribute("totalPages", commandesPage.getTotalPages());
        model.addAttribute("hasNext", commandesPage.hasNext());
        model.addAttribute("hasPrevious", commandesPage.hasPrevious());
        model.addAttribute("totalElements", commandesPage.getTotalElements());
        return "DSM/commandes";
    }

    // === CRÉATION DE COMMANDE POUR UN VM (DSM) ===
    @GetMapping("/commandes/nouvelle")
    public String dsmNouveauChoixOffre(Model model) {
        LocalDate today = LocalDate.now();
        // Récupérer le DSM connecté et ses gammes
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User dsm = userRepository.findByUsername(auth.getName());
        List<Gamme> gammes = (dsm != null && dsm.getGammes() != null) ? dsm.getGammes() : java.util.Collections.emptyList();

        List<Offre> offres = offreService.getAllOffres().stream()
                .filter(o -> o.getDateDebut() != null && o.getDateFin() != null)
                .filter(o -> !o.getDateDebut().isAfter(today))
                .filter(o -> !o.getDateFin().isBefore(today))
                .filter(Offre::isActive)
                .filter(o -> o.getGamme() != null && gammes.contains(o.getGamme()))
                .toList();
        model.addAttribute("offres", offres);
        return "DSM/commandes-offres";
    }

    @GetMapping("/commandes/commander")
    public String dsmCommander(@RequestParam Long offreId, Model model, Principal principal) {
        Offre offre = offreService.getOffreById(offreId);
        List<OffreProduit> produits = offreService.getProduitsByOffre(offreId);

        User dsm = userRepository.findByUsername(principal.getName());
        List<User> vms = userRepository.findByManager(dsm);

        // Sécurité: vérifier que l'offre est bien accessible au DSM (gamme + période + active)
        LocalDate today = LocalDate.now();
        boolean allowed = offre != null
                && offre.isActive()
                && offre.getDateDebut() != null && offre.getDateFin() != null
                && !offre.getDateDebut().isAfter(today)
                && !offre.getDateFin().isBefore(today)
                && dsm != null && dsm.getGammes() != null
                && offre.getGamme() != null && dsm.getGammes().contains(offre.getGamme());
        if (!allowed) {
            model.addAttribute("error", "Vous n'avez pas accès à cette offre.");
            return "redirect:/DSM/commandes/nouvelle";
        }

        // Pré-charger avec les secteurs du DSM pour une meilleure UX (affichage initial)
        // Puis, après sélection d'un VM, la liste sera rechargée dynamiquement pour ce VM.
        List<Secteur> secteurs = (dsm != null && dsm.getSecteurs() != null)
                ? new java.util.ArrayList<>(dsm.getSecteurs())
                : java.util.Collections.emptyList();
        List<Pharmacy> pharmacies = secteurs.isEmpty() ? java.util.Collections.emptyList() : pharmacyRepo.findBySecteurIn(secteurs);

        model.addAttribute("offreSelectionnee", offre);
        model.addAttribute("produits", produits);
        model.addAttribute("vms", vms);
        model.addAttribute("secteurs", secteurs);
        model.addAttribute("pharmacies", pharmacies);
        return "DSM/commande-offre";
    }

    // === Endpoints AJAX: secteurs et pharmacies du VM sélectionné ===
    @GetMapping("/vm/{vmId}/secteurs")
    @ResponseBody
    public List<Map<String, Object>> getVmSecteurs(@PathVariable Long vmId, Principal principal) {
        User dsm = userRepository.findByUsername(principal.getName());
        User vm = userRepository.findById(vmId).orElse(null);
        if (vm == null || vm.getManager() == null || dsm == null || !vm.getManager().getId().equals(dsm.getId())) {
            return java.util.List.of();
        }
        java.util.Set<Secteur> secteurs = vm.getSecteurs();
        if (secteurs == null) return java.util.List.of();
        java.util.List<Map<String, Object>> res = new java.util.ArrayList<>();
        for (Secteur s : secteurs) {
            if (s == null) continue;
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", s.getId());
            m.put("nom", s.getNom());
            res.add(m);
        }
        return res;
    }

    @GetMapping("/vm/{vmId}/pharmacies")
    @ResponseBody
    public List<Map<String, Object>> getVmPharmacies(@PathVariable Long vmId,
                                                     @RequestParam(required = false) Long secteurId,
                                                     Principal principal) {
        User dsm = userRepository.findByUsername(principal.getName());
        User vm = userRepository.findById(vmId).orElse(null);
        if (vm == null || vm.getManager() == null || dsm == null || !vm.getManager().getId().equals(dsm.getId())) {
            return java.util.List.of();
        }
        java.util.Set<Secteur> vmSecteurs = vm.getSecteurs();
        if (vmSecteurs == null || vmSecteurs.isEmpty()) return java.util.List.of();

        java.util.List<Secteur> allowed = new java.util.ArrayList<>(vmSecteurs);
        java.util.List<Pharmacy> pharmacies;
        if (secteurId != null) {
            pharmacies = pharmacyRepo.findBySecteurIn(allowed).stream()
                    .filter(p -> p.getSecteur() != null && secteurId.equals(p.getSecteur().getId()))
                    .toList();
        } else {
            pharmacies = pharmacyRepo.findBySecteurIn(allowed);
        }

        java.util.List<Map<String, Object>> res = new java.util.ArrayList<>();
        for (Pharmacy p : pharmacies) {
            if (p == null) continue;
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", p.getId());
            m.put("nom", p.getNom());
            m.put("secteurId", p.getSecteur() != null ? p.getSecteur().getId() : null);
            try { m.put("payed", p.isPayed()); } catch (Exception ignored) { m.put("payed", true); }
            res.add(m);
        }
        return res;
    }

    @PostMapping("/commandes/preview")
    public String dsmPreviewCommande(@RequestParam Map<String, String> params, Model model) {
        try {
            Long vmId = Long.parseLong(params.get("vmId"));
            Long pharmacyId = Long.parseLong(params.get("pharmacyId"));
            Long offreId = Long.parseLong(params.get("offreId"));

            User vm = userRepository.findById(vmId).orElseThrow();
            Pharmacy pharmacy = pharmacyRepo.findById(pharmacyId).orElseThrow();
            Offre offre = offreRepo.findById(offreId).orElseThrow();

            // Récupérer le DSM et vérifier l'accès à l'offre
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User dsm = userRepository.findByUsername(auth.getName());
            LocalDate today = LocalDate.now();
            boolean allowed = offre != null
                    && offre.isActive()
                    && offre.getDateDebut() != null && offre.getDateFin() != null
                    && !offre.getDateDebut().isAfter(today)
                    && !offre.getDateFin().isBefore(today)
                    && dsm != null && dsm.getGammes() != null
                    && offre.getGamme() != null && dsm.getGammes().contains(offre.getGamme());
            if (!allowed) {
                model.addAttribute("error", "Vous n'avez pas accès à cette offre.");
                return "redirect:/DSM/commandes/nouvelle";
            }

            String conditionPaiement = params.get("conditionPaiement");
            String typeReglement = params.get("typeReglement");
            String consignes = params.get("consignes");

            Commande cmd = new Commande();
            cmd.setPharmacy(pharmacy);
            cmd.setOffre(offre);
            cmd.setUser(vm);
            cmd.setDateCommande(LocalDateTime.now());
            cmd.setConditionPaiement(conditionPaiement);
            cmd.setTypeReglement(typeReglement);
            cmd.setConsignes(consignes);

            int totalQuantite = 0;
            double totalPrix = 0;
            List<LigneCommande> lignes = new ArrayList<>();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("quantite_") && !entry.getValue().isEmpty()) {
                    Long productId = Long.parseLong(entry.getKey().replace("quantite_", ""));
                    int qty = Integer.parseInt(entry.getValue());
                    if (qty <= 0) continue;

                    Product produit = productRepo.findById(productId).orElseThrow();
                    double prixUnitaire = offreService.getPrixProduitDansOffre(offreId, productId);
                    double remise = offreService.getRemisePourProduit(offreId, productId);
                    double prixRemise = prixUnitaire - (prixUnitaire * remise / 100.0);

                    LigneCommande ligne = new LigneCommande();
                    ligne.setCommande(cmd);
                    ligne.setProduit(produit);
                    ligne.setQuantite(qty);
                    ligne.setPrixUnitaire(prixRemise);

                    lignes.add(ligne);
                    totalQuantite += qty;
                    totalPrix += prixRemise * qty;
                }
            }

            cmd.setLignes(lignes);
            cmd.setTotalQuantite(totalQuantite);
            cmd.setTotalPrix(totalPrix);

            model.addAttribute("vm", vm);
            model.addAttribute("vmId", vmId);
            model.addAttribute("pharmacyId", pharmacyId);
            model.addAttribute("offreId", offreId);
            model.addAttribute("pharmacy", pharmacy);
            model.addAttribute("offre", offre);
            model.addAttribute("lignes", lignes);
            model.addAttribute("totalQuantite", totalQuantite);
            model.addAttribute("totalPrix", totalPrix);
            model.addAttribute("conditionPaiement", conditionPaiement);
            model.addAttribute("typeReglement", typeReglement);
            model.addAttribute("consignes", consignes);

            return "DSM/confirmation-commande";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la validation : " + e.getMessage());
            return "redirect:/DSM/commandes/nouvelle";
        }
    }

    @PostMapping("/commandes/confirmer")
    public String dsmConfirmerCommande(@RequestParam Map<String, String> params, Model model) {
        try {
            Long vmId = Long.parseLong(params.get("vmId"));
            Long pharmacyId = Long.parseLong(params.get("pharmacyId"));
            Long offreId = Long.parseLong(params.get("offreId"));

            User vm = userService.findById(vmId);
            Pharmacy pharmacy = pharmacyService.findById(pharmacyId);
            Offre offre = offreService.getOffreById(offreId);

            // Récupérer le DSM et vérifier l'accès à l'offre
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User dsm = userService.findByUsername(auth.getName());
            LocalDate today = LocalDate.now();
            boolean allowed = offre != null
                    && offre.isActive()
                    && offre.getDateDebut() != null && offre.getDateFin() != null
                    && !offre.getDateDebut().isAfter(today)
                    && !offre.getDateFin().isBefore(today)
                    && dsm != null && dsm.getGammes() != null
                    && offre.getGamme() != null && dsm.getGammes().contains(offre.getGamme());
            if (!allowed) {
                model.addAttribute("error", "Vous n'avez pas accès à cette offre.");
                return "redirect:/DSM/commandes/nouvelle";
            }

            String conditionPaiement = params.get("conditionPaiement");
            String typeReglement = params.get("typeReglement");
            String consignes = params.get("consignes");

            List<OrderLineInput> lines = new ArrayList<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("quantite_")) {
                    int qty = Integer.parseInt(entry.getValue());
                    if (qty <= 0) continue;
                    Long productId = Long.parseLong(entry.getKey().replace("quantite_", ""));
                    lines.add(new OrderLineInput(productId, qty));
                }
            }
            Commande cmd = commandeService.createCommande(vm, offreId, pharmacyId, lines, conditionPaiement, typeReglement, consignes);

            // Page qui déclenche le téléchargement et redirige vers le dashboard DSM
            model.addAttribute("downloadUrl", "/DSM/commande/" + cmd.getId() + "/bon.pdf");
            model.addAttribute("redirectUrl", "/dashboard/DSM");
            model.addAttribute("delaySeconds", 2);
            model.addAttribute("title", "Commande confirmée");
            model.addAttribute("message", "Le bon va se télécharger, vous allez être redirigé(e) vers votre tableau de bord.");
            return "download-and-redirect";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la confirmation : " + e.getMessage());
            return "redirect:/DSM/commandes/nouvelle";
        }
    }

    @GetMapping("/commande/{id}/bon.pdf")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('DSM')")
    public void dsmBonPdf(@PathVariable Long id, HttpServletResponse response, Principal principal) throws IOException {
        Commande cmd = commandeService.getById(id);
        User dsm = userService.findByUsername(principal.getName());
        if (cmd == null || cmd.getUser() == null || cmd.getUser().getManager() == null || !cmd.getUser().getManager().getId().equals(dsm.getId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        byte[] pdf = bonCommandePdfService.generate(cmd);
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bon_commande_" + id + ".pdf");
        response.getOutputStream().write(pdf);
        response.flushBuffer();
    }

    // === ÉDITION D'UNE COMMANDE D'ÉQUIPE ===
    @GetMapping("/commandes/{id}/edit")
    public String dsmEditCommande(@PathVariable Long id, Model model, Principal principal, RedirectAttributes ra) {
        Commande cmd = commandeService.getById(id);
        if (cmd == null) {
            ra.addFlashAttribute("error", "Commande introuvable");
            return "redirect:/DSM/commandes";
        }
        // contrôle: le DSM doit être manager du VM
        User dsm = userService.findByUsername(principal.getName());
        if (cmd.getUser() == null || cmd.getUser().getManager() == null || !cmd.getUser().getManager().getId().equals(dsm.getId())) {
            ra.addFlashAttribute("error", "Accès refusé à cette commande");
            return "redirect:/DSM/commandes";
        }

        Offre offre = cmd.getOffre();
        List<OffreProduit> produits = offreService.getProduitsByOffre(offre.getId());

        // Map produitId -> quantité existante pour pré-remplir le formulaire
        java.util.Map<Long, Integer> qmap = new java.util.HashMap<>();
        if (cmd.getLignes() != null) {
            for (LigneCommande l : cmd.getLignes()) {
                if (l.getProduit() != null && l.getProduit().getId() != null) {
                    qmap.put(l.getProduit().getId(), l.getQuantite());
                }
            }
        }

        model.addAttribute("cmd", cmd);
        model.addAttribute("produits", produits);
        model.addAttribute("offre", offre);
        model.addAttribute("qmap", qmap);
        return "DSM/commande-edit";
    }

    @PostMapping("/commandes/{id}/edit")
    public String dsmSaveEditCommande(@PathVariable Long id,
                                      @RequestParam Map<String, String> params,
                                      Principal principal,
                                      RedirectAttributes ra) {
        try {
            Commande cmd = commandeService.getById(id);
            User dsm = userService.findByUsername(principal.getName());
            if (cmd.getUser() == null || cmd.getUser().getManager() == null || !cmd.getUser().getManager().getId().equals(dsm.getId())) {
                ra.addFlashAttribute("error", "Accès refusé");
                return "redirect:/DSM/commandes";
            }
            java.util.List<OrderLineInput> lines = new java.util.ArrayList<>();
            for (var e : params.entrySet()) {
                if (e.getKey().startsWith("quantite_")) {
                    int qty = Integer.parseInt(e.getValue().isBlank()?"0":e.getValue());
                    if (qty <= 0) continue;
                    Long productId = Long.parseLong(e.getKey().replace("quantite_", ""));
                    lines.add(new OrderLineInput(productId, qty));
                }
            }
            commandeService.updateCommandeLines(id, lines);

            ra.addFlashAttribute("success", "Commande mise à jour");
            return "redirect:/DSM/commandes/" + id;
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
            return "redirect:/DSM/commandes";
        }
    }

    // === CRÉATION NOUVELLE OFFRE ===
    @GetMapping("/nouvelle-offre")
    public String afficherFormulaireNouvelleOffre(Model model) {
        model.addAttribute("offre", new Offre());
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(auth.getName());
        List<Gamme> gammesUtilisateur = user.getGammes();
        
        List<Product> produitsAutorises = productService.findAllowedForGammes(gammesUtilisateur);
        model.addAttribute("produits", produitsAutorises);
        
        return "DSM/creer-offre";
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
    
        User DSM = userService.findByUsername(principal.getName());
        offre.setCreatedByUser(DSM);
        offre.setDateCreation(LocalDateTime.now());
    
        // Associer automatiquement la première gamme de l'utilisateur
        List<Gamme> gammesUtilisateur = DSM.getGammes();
        if (gammesUtilisateur.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucune gamme associée à l'utilisateur.");
            return "redirect:/DSM/nouvelle-offre";
        }
    
        Gamme gammeAssociee = gammesUtilisateur.get(0); // ou autre logique de sélection
        offre.setGamme(gammeAssociee);
    
        List<OffreProduit> listeProduits = new ArrayList<>();
        for (int i = 0; i < produitIds.size(); i++) {
            Product produit = productService.getProduitById(produitIds.get(i));
    
            // Vérification sécurité : produit doit appartenir à la gamme sélectionnée
            if (!produit.getGammes().contains(gammeAssociee)) {
                continue; // ou lever une exception si nécessaire
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
        return "redirect:/DSM/dashboard";
    }
    

    // === CRÉATION D’UN CLIENT ===
    @GetMapping("/nouveau-client")
    public String showCreateClientForm(Model model) {
        model.addAttribute("user", new User());
        return "DSM/create-client";
    }

    @PostMapping("/nouveau-client")
    public String createClient(@ModelAttribute("user") User user, RedirectAttributes redirectAttributes) {
        try {
            userService.createClientUser(user);
            redirectAttributes.addFlashAttribute("success", "Client créé avec succès !");
            return "redirect:/DSM/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur : " + e.getMessage());
            return "redirect:/DSM/nouveau-client";
        }
    }

// === MES OFFRES ===
@GetMapping("/mes-offres")
public String afficherMesOffres(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "active") String type,
        Model model,
        Principal principal
) {
    // Récupération de l'utilisateur connecté
    User dsm = userRepository.findByUsername(principal.getName());

    // Configuration de la pagination
    Pageable pageable = PageRequest.of(page, size);

    // Récupération des offres selon le type
    Page<Offre> offresPage;
    long totalOffres;

    if ("archived".equalsIgnoreCase(type)) {
        offresPage = offreService.getArchivedOffresByUser(dsm, pageable);
        totalOffres = offreService.getArchivedOffresCount(dsm);
    } else {
        offresPage = offreService.getActiveOffresByUser(dsm, pageable);
        totalOffres = offreService.getActiveOffresCount(dsm);
    }

    // Informations de pagination
    int totalPages   = offresPage.getTotalPages();
    int currentPage  = offresPage.getNumber();
    boolean hasNext  = offresPage.hasNext();
    boolean hasPrev  = offresPage.hasPrevious();

    // Ajout des attributs au modèle
    model.addAttribute("offres", offresPage.getContent());
    model.addAttribute("currentPage", currentPage);
    model.addAttribute("totalPages", totalPages);
    model.addAttribute("hasNext", hasNext);
    model.addAttribute("hasPrevious", hasPrev);
    model.addAttribute("size", size);
    model.addAttribute("type", type);
    model.addAttribute("totalOffres", totalOffres);
    model.addAttribute("pageNumbers", IntStream.range(0, Math.max(totalPages, 1)).boxed().toList());
    model.addAttribute("today", LocalDate.now());

    // Compteurs pour les onglets actif/archivé
    model.addAttribute("activeOffresCount", offreService.getActiveOffresCount(dsm));
    model.addAttribute("archivedOffresCount", offreService.getArchivedOffresCount(dsm));

    // Vue retournée
    return "DSM/mes-offres";
}


    

    // === DÉTAIL D’UNE OFFRE ===
    @GetMapping("/mes-offres/{id}")
    public String afficherDetailsOffre(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        Offre offre = offreService.getOffreById(id);
        User dsm = userRepository.findByUsername(principal.getName());

        if (!isOwner(offre, dsm)) {
            redirectAttributes.addFlashAttribute("error", "Vous n'avez pas accès à cette offre.");
            return "redirect:/DSM/mes-offres";
        }

        model.addAttribute("offre", offre);
        model.addAttribute("produits", offre.getProduits());
        return "DSM/detail-offre";
    }
// === MODIFICATION OFFRE ===
@GetMapping("/mes-offres/edit/{id}")
public String afficherFormulaireModificationOffre(@PathVariable Long id,
                                                  Model model,
                                                  Principal principal,
                                                  RedirectAttributes redirectAttributes) {
    // 1) Offret existante ?
    Offre offre;
    try {
        offre = offreService.getOffreById(id); // lève une RuntimeException si introuvable
    } catch (RuntimeException ex) {
        redirectAttributes.addFlashAttribute("error", "Offre introuvable.");
        return "redirect:/DSM/mes-offres";
    }

    // 2) Utilisateur connecté (évite NPE si principal est null)
    if (principal == null) {
        redirectAttributes.addFlashAttribute("error", "Session expirée. Veuillez vous reconnecter.");
        return "redirect:/login"; // ou ta page de login
    }
    User dsm = userRepository.findByUsername(principal.getName());

    // 3) Contrôle d’accès robuste (même logique que isOwner)
    if (!isOwner(offre, dsm)) {
        redirectAttributes.addFlashAttribute("error", "Vous n'avez pas accès à cette offre.");
        return "redirect:/DSM/mes-offres";
    }

    // 4) OK : on prépare le modèle
    model.addAttribute("offre", offre);
    model.addAttribute("produits", productRepo.findAll());
    return "DSM/edit-offre";
}


@PostMapping("/mes-offres/edit/{id}")
public String enregistrerModificationsOffre(@PathVariable Long id,
                                            @RequestParam("nom") String nom,
                                            @RequestParam("description") String description,
                                            @RequestParam(value = "dateDebut", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
                                            @RequestParam(value = "dateFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
                                            @RequestParam("active") boolean active,
                                            @RequestParam(value = "produitIds", required = false) List<Long> produitIds,
                                            @RequestParam(value = "prix", required = false) List<Double> prix,
                                            @RequestParam(value = "remise", required = false) List<Integer> remises,
                                            @RequestParam(value = "min", required = false) List<Integer> quantiteMin,
                                            @RequestParam(value = "max", required = false) List<Integer> quantiteMax,
                                            @RequestParam(value = "colisage", required = false) List<Integer> colisages,
                                            Principal principal,
                                            RedirectAttributes redirectAttributes) {

    try {
        System.out.println("=== DEBUG DSM EDIT POST ===");
        System.out.println("ID: " + id);
        System.out.println("Nom: " + nom);
        System.out.println("Description: " + description);
        System.out.println("DateDebut: " + dateDebut);
        System.out.println("DateFin: " + dateFin);
        System.out.println("Active: " + active);
        System.out.println("ProduitIds: " + produitIds);
        System.out.println("Prix: " + prix);
        System.out.println("Principal: " + (principal != null ? principal.getName() : "NULL"));
        
        User dsm = userRepository.findByUsername(principal.getName());
        System.out.println("DSM User: " + (dsm != null ? dsm.getUsername() : "NULL"));
        
        Offre offre = offreService.getOffreById(id);
        System.out.println("Offre: " + (offre != null ? offre.getNom() : "NULL"));

        if (!isOwner(offre, dsm)) {
            redirectAttributes.addFlashAttribute("error", "Accès refusé.");
            return "redirect:/DSM/mes-offres";
        }

        // Construire la liste des produits uniquement si des champs produits sont fournis
        List<OffreProduit> nouveauxProduits = null;
        boolean hasProduitPayload = (produitIds != null && !produitIds.isEmpty() &&
                                     prix != null && remises != null && quantiteMin != null && quantiteMax != null);
        if (hasProduitPayload) {
            nouveauxProduits = new ArrayList<>();
            for (int i = 0; i < produitIds.size(); i++) {
                Product produit = productRepo.findById(produitIds.get(i)).orElseThrow();
                OffreProduit op = new OffreProduit();
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
        }

        // Utiliser une méthode de service qui préserve les produits si la liste est null
        offreService.updateOffreDSM(id, nom, description, dateDebut, dateFin, active, nouveauxProduits);

        redirectAttributes.addFlashAttribute("success", "Offre modifiée avec succès !");
        return "redirect:/DSM/mes-offres";
    } catch (Exception e) {
        System.err.println("ERREUR DSM EDIT: " + e.getMessage());
        e.printStackTrace();
        redirectAttributes.addFlashAttribute("error", "Erreur lors de la modification: " + e.getMessage());
        return "redirect:/DSM/mes-offres";
    }
}
    // === Activer / Désactiver une offre ===
    @PostMapping("/mes-offres/{id}/toggle")
    public String toggleOffre(@PathVariable Long id,
                              @RequestParam("active") boolean active,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(defaultValue = "active") String type,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        User dsm = userRepository.findByUsername(principal.getName());
        try {
            offreService.toggleActive(id, dsm, active);
            redirectAttributes.addFlashAttribute("success", "Offre mise à jour");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/DSM/mes-offres?page=" + page + "&size=" + size + "&type=" + type;
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

     // === Liste des pharmacies ===
    @GetMapping("/pharmacies")
    public String afficherListePharmacies(Model model) {
        model.addAttribute("pharmacies", pharmacyService.findAll());
        model.addAttribute("secteurs", secteurRepository.findAll());

        return "DSM/pharmacies";
    }

    // === Formulaire création ===
    @GetMapping("/pharmacies/nouvelle")
    public String formulaireNouvellePharmacy(Model model) {
        model.addAttribute("pharmacy", new Pharmacy());
        model.addAttribute("secteurs", secteurRepository.findAll());
        return "DSM/form-pharmacy";
    }

    // === Enregistrer nouvelle pharmacie ===
    @PostMapping("/pharmacies")
    public String enregistrerPharmacy(@ModelAttribute Pharmacy pharmacy,
                                      @RequestParam("secteur.id") Long secteurId,
                                      RedirectAttributes redirectAttributes) {

        Secteur secteur = secteurRepository.findById(secteurId)
                .orElseThrow(() -> new RuntimeException("Secteur non trouvé"));
        pharmacy.setSecteur(secteur);

        pharmacyService.save(pharmacy);
        redirectAttributes.addFlashAttribute("success", "Pharmacie enregistrée avec succès !");
        return "redirect:/DSM/pharmacies";
    }

    // === Formulaire modification ===
    @GetMapping("/pharmacies/edit/{id}")
    public String modifierPharmacy(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Pharmacy pharmacy = pharmacyService.findById(id);
        if (pharmacy == null) {
            redirectAttributes.addFlashAttribute("error", "Pharmacie introuvable.");
            return "redirect:/DSM/pharmacies";
        }
        model.addAttribute("pharmacy", pharmacy);
        model.addAttribute("secteurs", secteurRepository.findAll());
        return "DSM/form-pharmacy";
    }

    // === Enregistrer modification ===
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
        return "redirect:/DSM/pharmacies";
    }

        // === FORMULAIRE D'UPLOAD (GET) ===
        @GetMapping("/pharmacies/import")
        public String showImportPharmaciesForm() {
            return "DSM/import-pharmacies";
        }

           // === TÉLÉCHARGER UN MODÈLE (GET) ===
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

    // === TRAITER L'UPLOAD (POST) ===
    @PostMapping("/pharmacies/import")
    public String importPharmaciesFromExcel(@RequestParam("file") MultipartFile file, Model model) {
        try {
            var report = excelPharmacyImporter.importFromExcel(file);
            model.addAttribute("message",
                    "Import terminé : %d ajoutées, %d mises à jour, %d ignorées."
                            .formatted(report.getInserted(), report.getUpdated(), report.getSkipped()));
            model.addAttribute("report", report.toPrettyString());
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de l’import : " + e.getMessage());
        }
        return "DSM/import-pharmacies";
    }

        


    @GetMapping("/export-commandes")
public void exportToutesLesCommandes(HttpServletResponse response) throws IOException {
    System.out.println(">>> Export en cours...");

    try {
        List<Commande> commandes = commandeRepo.findAll();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=commandes-et-details.xlsx");

        Workbook workbook = new XSSFWorkbook();

        // === Feuille 1 : Commandes ===
        Sheet commandesSheet = workbook.createSheet("Commandes");

        Row cmdHeader = commandesSheet.createRow(0);
        cmdHeader.createCell(0).setCellValue("ID Commande");
        cmdHeader.createCell(1).setCellValue("Date");
        cmdHeader.createCell(2).setCellValue("VM");
        cmdHeader.createCell(3).setCellValue("Pharmacie");
        cmdHeader.createCell(4).setCellValue("ICE");
        cmdHeader.createCell(5).setCellValue("Secteur");
        cmdHeader.createCell(6).setCellValue("Offre");
        cmdHeader.createCell(7).setCellValue("Total Commande");

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
        }

        for (int i = 0; i <= 7; i++) commandesSheet.autoSizeColumn(i);

        // === Feuille 2 : Détails des lignes commande ===
        Sheet lignesSheet = workbook.createSheet("LignesCommande");

        Row ligneHeader = lignesSheet.createRow(0);
        ligneHeader.createCell(0).setCellValue("ID LigneCommande");
        ligneHeader.createCell(1).setCellValue("ID Commande");
        ligneHeader.createCell(2).setCellValue("Produit");
        ligneHeader.createCell(3).setCellValue("Colisage");
        ligneHeader.createCell(4).setCellValue("Quantité");
        ligneHeader.createCell(5).setCellValue("Prix Unitaire");
        ligneHeader.createCell(6).setCellValue("Total Produit");

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
            }
        }

        for (int i = 0; i <= 6; i++) lignesSheet.autoSizeColumn(i);

        // Enregistrement
        try (OutputStream os = response.getOutputStream()) {
            workbook.write(os);
        }

        workbook.close();
        System.out.println(">>> Export terminé avec 2 feuilles.");
    } catch (Exception e) {
        e.printStackTrace();
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur lors de l’export Excel.");
    }
}


    /* */
    
 // --- Page d’import (formulaire) ---
    @GetMapping("/offres/import")
    public String pageImportOffres(Model model) {
        return "DSM/import-offres"; // template ci-dessous
    }

    // --- Action d’import ---
    @PostMapping("/offres/import")
    public String importerOffres(@RequestParam("file") MultipartFile file,
                                 Principal principal,
                                 RedirectAttributes ra) {
        try {
            User dsm = userRepository.findByUsername(principal.getName());
            OfferImportResult result = offerImportService.importWorkbook(file, dsm);

            if (!result.getErrors().isEmpty()) {
                ra.addFlashAttribute("error", String.join("<br/>", result.getErrors()));
            }
            ra.addFlashAttribute("success",
                    "Import terminé : " + result.getOffresCreees() + " offre(s) créée(s), " +
                    result.getLignesTraitees() + " ligne(s) traitée(s).");

        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Échec import : " + ex.getMessage());
        }
        return "redirect:/DSM/offres/import";
    }

    // --- Générer / télécharger un template Excel ---
    @GetMapping("/offres/import/template")
    public void telechargerTemplate(HttpServletResponse response) throws IOException {
        byte[] xlsx = offerImportService.generateTemplate();

        String filename = URLEncoder.encode("template_import_offres.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename);
        response.getOutputStream().write(xlsx);
        response.flushBuffer();
    }






    // src/main/java/com/project/controller/DSMController.java (extrait)
@GetMapping("/statistiques")
public String statistiquesDSM(Model model,
                              @RequestParam(required = false) Long gammeId,
                              @RequestParam(required = false) Long userId,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                              Principal principal) {

    // DSM connecté
    User dsm = userRepository.findByUsername(principal.getName());
    Long dsmId = dsm != null ? dsm.getId() : null;

    // Gammes du DSM pour alimenter le filtre
    List<Gamme> gammesDSM = dsm != null ? dsm.getGammes() : java.util.List.of();
    model.addAttribute("gammes", gammesDSM);

    // Équipe du DSM (si manager est mappé)
    List<User> equipe = userRepository.findByManager(dsm);
    model.addAttribute("equipe", equipe);

    // Bornes de date par défaut : 6 derniers mois
    LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusMonths(6).withDayOfMonth(1);
    LocalDate end   = (endDate   != null) ? endDate   : LocalDate.now();
    LocalDateTime startDT = start.atStartOfDay();
    LocalDateTime endDT   = end.atTime(23,59,59);

    // ==== Stats ====
    var byMonth      = commandeRepo.statsRevenueByMonth(startDT, endDT, gammeId, dsmId, userId);
    var bySecteur    = commandeRepo.statsBySecteur(startDT, endDT, gammeId, dsmId, userId);
    var byVm         = commandeRepo.statsByVm(startDT, endDT, gammeId, dsmId, userId);
    var topClients   = commandeRepo.topClients(startDT, endDT, gammeId, dsmId, userId);
    var byOffre      = commandeRepo.revenueByOffre(startDT, endDT, gammeId, dsmId, userId);
    var byGamme      = commandeRepo.revenueByGamme(startDT, endDT, gammeId, dsmId, userId);
    var orderCounts  = commandeRepo.countOrdersByMonth(startDT, endDT, gammeId, dsmId, userId);
    var topProductsQ = commandeRepo.topProductsByQuantity(startDT, endDT, gammeId, dsmId, userId);
    var statusSplit  = commandeRepo.statusBreakdown(startDT, endDT, gammeId, dsmId, userId);
    var avgBasketVm  = commandeRepo.averageBasketByVm(startDT, endDT, gammeId, dsmId, userId);

    // Séries prêtes pour Chart.js
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

    // KPIs rapides
    double caTotal = monthsValues.stream().mapToDouble(Double::doubleValue).sum();
    long nbCmd = orderCounts.stream().mapToLong(r -> r.getCount() != null ? r.getCount() : 0L).sum();

    // Préparer autres séries
    List<String> offreLabels = new java.util.ArrayList<>();
    List<Double> offreValues = new java.util.ArrayList<>();
    for (var r : byOffre) { offreLabels.add(r.getLabel()); offreValues.add(r.getTotal()); }

    List<String> gammeLabels = new java.util.ArrayList<>();
    List<Double> gammeValues = new java.util.ArrayList<>();
    for (var r : byGamme) { gammeLabels.add(r.getLabel()); gammeValues.add(r.getTotal()); }

    List<String> ordersMonthLabels = new java.util.ArrayList<>();
    List<Long> ordersMonthValues = new java.util.ArrayList<>();
    for (var r : orderCounts) {
        String label = String.format("%02d/%d", r.getMonth(), r.getYear());
        ordersMonthLabels.add(label);
        ordersMonthValues.add(r.getCount());
    }

    List<String> prodLabels = new java.util.ArrayList<>();
    List<Double> prodValues = new java.util.ArrayList<>();
    for (var r : topProductsQ) { prodLabels.add(r.getLabel()); prodValues.add(r.getTotal()); }

    List<String> statusLabels = new java.util.ArrayList<>();
    List<Long> statusValues = new java.util.ArrayList<>();
    for (var r : statusSplit) { statusLabels.add(r.getLabel()); statusValues.add(r.getCount()); }

    List<String> avgVmLabels = new java.util.ArrayList<>();
    List<Double> avgVmValues = new java.util.ArrayList<>();
    for (var r : avgBasketVm) { avgVmLabels.add(r.getLabel()); avgVmValues.add(r.getTotal()); }

    // Filtres de rappel
    model.addAttribute("filtreGammeId", gammeId);
    model.addAttribute("filtreUserId", userId);
    model.addAttribute("startDate", start);
    model.addAttribute("endDate", end);

    // Données pour chart.js
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

    // Nouveaux jeux de données
    model.addAttribute("offreLabels", offreLabels);
    model.addAttribute("offreValues", offreValues);
    model.addAttribute("gammeLabels", gammeLabels);
    model.addAttribute("gammeValues", gammeValues);
    model.addAttribute("ordersMonthLabels", ordersMonthLabels);
    model.addAttribute("ordersMonthValues", ordersMonthValues);
    model.addAttribute("prodLabels", prodLabels);
    model.addAttribute("prodValues", prodValues);
    model.addAttribute("statusLabels", statusLabels);
    model.addAttribute("statusValues", statusValues);
    model.addAttribute("avgVmLabels", avgVmLabels);
    model.addAttribute("avgVmValues", avgVmValues);

    return "DSM/statistiques";
}

}
