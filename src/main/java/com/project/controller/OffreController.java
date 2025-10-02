
package com.project.controller;

import com.project.entity.*;
import com.project.service.*;
import com.project.service.NotificationService;
import com.project.service.dto.OrderLineInput;
import com.project.service.dto.OrderPreview;
import com.project.service.dto.OrderSubmitInput;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/client")
public class OffreController {

    private static final Logger logger = LoggerFactory.getLogger(OffreController.class);

    private final OffreService offreService;
    private final PharmacyService pharmacyService;
    private final ProductService productService;
    private final UserService userService;
    private final CommandeService commandeService;
    private final BonCommandePdfService bonCommandePdfService;
    @org.springframework.beans.factory.annotation.Autowired
    private NotificationService notificationService;
    private final com.project.repository.LigneCommandeRepository ligneCommandeRepository;

    public OffreController(OffreService offreService,
                           PharmacyService pharmacyService,
                           ProductService productService,
                           UserService userService,
                           CommandeService commandeService,
                           BonCommandePdfService bonCommandePdfService,
                           com.project.repository.LigneCommandeRepository ligneCommandeRepository) {
        this.offreService = offreService;
        this.pharmacyService = pharmacyService;
        this.productService = productService;
        this.userService = userService;
        this.commandeService = commandeService;
        this.bonCommandePdfService = bonCommandePdfService;
        this.ligneCommandeRepository = ligneCommandeRepository;
    }

    // Inject notifications for VM header (badge + dropdown + websocket userId)
    @ModelAttribute
    public void injectNotifications(Model model) {
        try {
            User u = getConnectedUser();
            if (u != null) {
                long unread = notificationService.unreadCount(u);
                model.addAttribute("unreadCount", unread);
                model.addAttribute("latestNotifications", notificationService.latestFor(u));
                model.addAttribute("notifBase", "/client");
                model.addAttribute("currentUserId", u.getId());
            }
        } catch (Exception ignored) {}
    }

    @GetMapping("/notifications/{id}/open")
    public String clientOpenNotification(@PathVariable Long id) {
        return notificationService.findById(id)
                .map(n -> {
                    try {
                        User u = getConnectedUser();
                        if (n.getRecipient() != null && u != null && n.getRecipient().getId().equals(u.getId())) {
                            if (!n.isReadFlag()) { n.setReadFlag(true); notificationService.save(n); }
                        }
                    } catch (Exception ignored) {}
                    return "redirect:/client/commandes";
                })
                .orElse("redirect:/client/commandes");
    }

    private User getConnectedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userService.findByUsername(auth.getName());
    }

    @GetMapping("/offres")
    public String afficherOffres(Model model) {
        try {
            User user = getConnectedUser();
            List<Offre> offres = offreService.findActiveOffresForUser(user);

            model.addAttribute("offres", offres);
            return "VM/offres";
        } catch (Exception e) {
            logger.error("Erreur lors du chargement des offres", e);
            model.addAttribute("error", "Impossible de charger les offres");
            return "VM/offres";
        }
    }

    @GetMapping("/offre/{id}/details")
    public String voirDetailsOffrePourCommande(@PathVariable("id") Long offreId, Model model) {
        try {
            Offre offre = offreService.getOffreById(offreId);
            List<OffreProduit> produits = offreService.getProduitsByOffre(offreId);
            User user = getConnectedUser();
            List<Secteur> secteurs = new ArrayList<>(user.getSecteurs());
            List<Pharmacy> pharmacies = pharmacyService.findBySecteurs(secteurs);

            boolean allowed = offreService.isOffreAccessibleForUser(offre, user, LocalDate.now());
            if (!allowed) {
                model.addAttribute("error", "Vous n'avez pas accès à cette offre.");
                return "redirect:/client/offres";
            }

            model.addAttribute("offre", offre);
            model.addAttribute("produits", produits);
            model.addAttribute("pharmacies", pharmacies);

            return "VM/offre-details";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors du chargement");
            return "redirect:/client/offres";
        }
    }

    @GetMapping("/commandes")
    @Transactional(readOnly = true)
    public String afficherHistoriqueCommandes(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Integer month,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false, name = "pharmacie") String pharmacyName,
        @RequestParam(required = false, name = "secteur") Long secteurId,
        Model model
    ) {
        User currentUser = getConnectedUser();

        // Récupérer les secteurs et pharmacies accessibles au VM pour les filtres (sécurisé)
        List<Secteur> secteurs = (currentUser != null && currentUser.getSecteurs() != null)
                ? new ArrayList<>(currentUser.getSecteurs())
                : new ArrayList<>();
        List<Pharmacy> pharmacies = secteurs.isEmpty()
                ? new ArrayList<>()
                : pharmacyService.findBySecteurs(secteurs);

        List<Commande> commandes = commandeService.findByUserWithFiltersAdvanced(
            currentUser, startDate, endDate, month, year, pharmacyName, secteurId
        );

        // Charger explicitement les lignes (et produits) pour éviter les soucis LAZY en vue
        java.util.Map<Long, java.util.List<com.project.service.dto.LigneCommandeView>> lignesByCommande = new java.util.HashMap<>();
        if (commandes != null) {
            for (Commande c : commandes) {
                try {
                    java.util.List<LigneCommande> lcs = ligneCommandeRepository.findByCommandeId(c.getId());
                    // Build DTOs for safe rendering in view
                    java.util.List<com.project.service.dto.LigneCommandeView> views = new java.util.ArrayList<>();
                    for (LigneCommande l : lcs) {
                        com.project.service.dto.LigneCommandeView v = new com.project.service.dto.LigneCommandeView();
                        v.setId(l.getId());
                        String nom = "-";
                        try { if (l.getProduit() != null) nom = l.getProduit().getNom(); } catch (Exception ignored) {}
                        v.setProduitNom(nom);
                        v.setQuantite(l.getQuantite());
                        v.setPrixUnitaire(l.getPrixUnitaire());
                        v.setStatutProduit(l.getStatutProduit());
                        v.setRupture(l.getStatutProduit() == com.project.entity.LigneCommandeStatus.RUPTURE_STOCK);
                        views.add(v);
                    }
                    lignesByCommande.put(c.getId(), views);
                    // also keep lcs set on entity for other usages (optional)
                    c.setLignes(lcs);
                } catch (Exception ignored) {}
            }
        }

        model.addAttribute("commandes", commandes);
        // Valeurs des filtres (persistantes via URL)
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        model.addAttribute("pharmacie", pharmacyName);
        model.addAttribute("secteur", secteurId);

        // Données pour les inputs de filtre
        model.addAttribute("secteurs", secteurs);
        model.addAttribute("pharmacies", pharmacies);
        model.addAttribute("lignesByCommande", lignesByCommande);

        return "VM/historique-commandes";
    }

    @GetMapping(value = "/commander", params = "offreId")
    public String remplirCommande(@RequestParam Long offreId, Model model) {
        try {
            Offre offre = offreService.getOffreById(offreId);
            List<OffreProduit> produits = offreService.getProduitsByOffre(offreId);
            User user = getConnectedUser();
            List<Secteur> secteurs = (user != null && user.getSecteurs() != null)
                    ? new ArrayList<>(user.getSecteurs())
                    : new ArrayList<>();
            List<Pharmacy> pharmacies = secteurs.isEmpty()
                    ? new ArrayList<>()
                    : pharmacyService.findBySecteurs(secteurs);

            boolean allowed = offreService.isOffreAccessibleForUser(offre, user, LocalDate.now());
            if (!allowed) {
                model.addAttribute("error", "Vous n'avez pas accès à cette offre.");
                return "redirect:/client/offres";
            }

            model.addAttribute("offreSelectionnee", offre);
            model.addAttribute("produits", produits);
            model.addAttribute("pharmacies", pharmacies);
            model.addAttribute("secteurs", secteurs);

            return "VM/offre-details";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur de chargement");
            return "redirect:/client/offres";
        }
    }

    // Fallback si l'ID de l'offre n'est pas fourni en paramètre
    @GetMapping("/commander")
    public String commanderSansId() {
        return "redirect:/client/offres";
    }

    @GetMapping("/previewCommande")
    public String previewCommandeGet() {
        // Accès direct en GET: rediriger vers la liste des offres pour éviter un 404
        return "redirect:/client/offres";
    }

    @PostMapping("/previewCommande")
    public String validerCommande(@ModelAttribute OrderSubmitInput input, HttpServletRequest request, Model model) {
        try {
            Long pharmacyId = input.getPharmacyId();
            Long offreId = input.getOffreId();
            User user = getConnectedUser();
            Offre offre = offreService.getOffreById(offreId);
            boolean allowed = offreService.isOffreAccessibleForUser(offre, user, LocalDate.now());
            if (!allowed) {
                model.addAttribute("error", "Vous n'avez pas accès à cette offre.");
                return "redirect:/client/offres";
            }

            // Build lines from request parameters quantite_{productId} if not bound
            if (input.getLines() == null || input.getLines().isEmpty()) {
                List<OrderLineInput> lines = new ArrayList<>();
                var params = request.getParameterMap();
                for (String key : params.keySet()) {
                    if (key != null && key.startsWith("quantite_")) {
                        String val = request.getParameter(key);
                        if (val == null || val.isBlank()) continue;
                        int qty = Integer.parseInt(val);
                        if (qty <= 0) continue;
                        Long productId = Long.parseLong(key.replace("quantite_", ""));
                        lines.add(new OrderLineInput(productId, qty));
                    }
                }
                input.setLines(lines);
            }

            OrderPreview preview = commandeService.buildPreview(user, offreId, pharmacyId, input.getLines(),
                    input.getConditionPaiement(), input.getTypeReglement(), input.getConsignes());

            Pharmacy pharmacy = pharmacyService.findById(pharmacyId);

            model.addAttribute("pharmacyId", pharmacyId);
            model.addAttribute("offreId", offreId);
            model.addAttribute("offre", offre);
            model.addAttribute("preview", preview);
            model.addAttribute("pharmacy", pharmacy);
            model.addAttribute("conditionPaiement", input.getConditionPaiement());
            model.addAttribute("typeReglement", input.getTypeReglement());
            model.addAttribute("consignes", input.getConsignes());

            return "VM/confirmation-commande";

        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la validation : " + e.getMessage());
            return "redirect:/client/offres";
        }
    }

    @PostMapping("/confirmerCommande")
    public String confirmerCommande(@ModelAttribute OrderSubmitInput input, HttpServletRequest request, Model model) {
        try {
            Long pharmacyId = input.getPharmacyId();
            Long offreId = input.getOffreId();
            User user = getConnectedUser();
            Offre offre = offreService.getOffreById(offreId);
            boolean allowed = offreService.isOffreAccessibleForUser(offre, user, LocalDate.now());
            if (!allowed) {
                model.addAttribute("error", "Vous n'avez pas accès à cette offre.");
                return "VM/confirmation-commande";
            }
            // Build lines if not set
            if (input.getLines() == null || input.getLines().isEmpty()) {
                List<OrderLineInput> lines = new ArrayList<>();
                var params = request.getParameterMap();
                for (String key : params.keySet()) {
                    if (key != null && key.startsWith("quantite_")) {
                        String val = request.getParameter(key);
                        if (val == null || val.isBlank()) continue;
                        int qty = Integer.parseInt(val);
                        if (qty <= 0) continue;
                        Long productId = Long.parseLong(key.replace("quantite_", ""));
                        lines.add(new OrderLineInput(productId, qty));
                    }
                }
                input.setLines(lines);
            }

            Commande cmd = commandeService.createCommande(user, offreId, pharmacyId, input.getLines(),
                    input.getConditionPaiement(), input.getTypeReglement(), input.getConsignes());
            // Afficher page qui télécharge le PDF puis redirige vers le dashboard client
            model.addAttribute("downloadUrl", "/client/commande/" + cmd.getId() + "/bon.pdf");
            model.addAttribute("redirectUrl", "/dashboard/client");
            model.addAttribute("delaySeconds", 2);
            model.addAttribute("title", "Commande confirmée");
            model.addAttribute("message", "Le bon va se télécharger, vous allez être redirigé(e) vers votre tableau de bord.");
            return "download-and-redirect";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la confirmation : " + e.getMessage());
            return "redirect:/client/offres";
        }
    }
    @GetMapping("/commande/{id}/bon.pdf")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CLIENT')")
    public void downloadBonDeCommande(@PathVariable Long id,
                                      HttpServletResponse response,
                                      org.springframework.security.core.Authentication auth) throws IOException {
        Commande cmd = commandeService.getById(id);
        if (cmd == null || cmd.getUser() == null || auth == null || !cmd.getUser().getUsername().equals(auth.getName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        byte[] pdf = bonCommandePdfService.generate(cmd);

        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bon_commande_" + id + ".pdf");
        response.getOutputStream().write(pdf);
        response.flushBuffer();
    }
}
