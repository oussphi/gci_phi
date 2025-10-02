package com.project.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.project.entity.Commande;
import com.project.entity.LigneCommande;
import com.project.entity.OffreProduit;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
public class BonCommandePdfService {

    // ---- Palette ----
    private static final Color BRAND      = new Color(20, 90, 200);       // bleu
    private static final Color BAND_BG    = new Color(238, 243, 255);     // bandeau léger
    private static final Color LABEL_BG   = new Color(245, 245, 245);
    private static final Color TABLE_HDR  = new Color(230, 230, 230);
    private static final Color ROW_ALT    = new Color(250, 250, 250);
    private static final Color BORDER     = new Color(210, 210, 210);

    // chemins classpath essayés dans l’ordre
    private static final List<String> LOGO_CANDIDATES = Arrays.asList(
            "static/images/logo4.png",
            "images/logo4.png",
            "/static/images/logo4.png",
            "/images/logo4.png",
            "logo4.png"
    );

    public byte[] generate(Commande cmd) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // ==== BANDEAU MARQUE ====
            addBrandBand(doc);

            // ==== TITRE ====
            Font title = new Font(Font.HELVETICA, 16, Font.BOLD, Color.BLACK);
            Paragraph pTitle = new Paragraph("BON DE COMMANDE Pharmacies", title);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            pTitle.setSpacingBefore(6f);
            pTitle.setSpacingAfter(12f);
            doc.add(pTitle);

            // ==== SECTION : INFORMATIONS ====
            addSectionHeader(doc, "Informations");
            PdfPTable header = new PdfPTable(new float[]{1.6f, 3.2f});
            header.setWidthPercentage(100);
            header.getDefaultCell().setBorderColor(BORDER);

            Font h = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font n = new Font(Font.HELVETICA, 11, Font.NORMAL);
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            header.addCell(labelCell("N° Commande"));
            header.addCell(valueCell(String.valueOf(cmd.getId()), n));

            header.addCell(labelCell("Date"));
            header.addCell(valueCell(cmd.getDateCommande() != null ? df.format(cmd.getDateCommande()) : "-", n));

            header.addCell(labelCell("Client (VM)"));
            header.addCell(valueCell(cmd.getUser() != null ? cmd.getUser().getUsername() : "-", n));

            header.addCell(labelCell("Pharmacie"));
            header.addCell(valueCell(cmd.getPharmacy() != null ? cmd.getPharmacy().getNom() : "-", n));

            header.addCell(labelCell("ICE"));
            header.addCell(valueCell(cmd.getPharmacy() != null ? nullToDash(cmd.getPharmacy().getIce()) : "-", n));

            String secteur = (cmd.getPharmacy() != null && cmd.getPharmacy().getSecteur() != null)
                    ? cmd.getPharmacy().getSecteur().getNom() : "-";
            header.addCell(labelCell("Secteur"));
            header.addCell(valueCell(secteur, n));

            header.addCell(labelCell("Offre"));
            header.addCell(valueCell(cmd.getOffre() != null ? cmd.getOffre().getNom() : "-", n));

            header.addCell(labelCell("Statut"));
            header.addCell(valueCell(cmd.getStatus() != null ? cmd.getStatus().name() : "-", n));

            header.addCell(labelCell("Condition de paiement"));
            header.addCell(valueCell(nullToDash(cmd.getConditionPaiement()), n));

            header.addCell(labelCell("Type de règlement"));
            header.addCell(valueCell(nullToDash(cmd.getTypeReglement()), n));

            header.addCell(labelCell("Consignes"));
            header.addCell(valueCell(nullToDash(cmd.getConsignes()), n));

            header.setSpacingAfter(12f);
            doc.add(header);

            // ==== SECTION : LIGNES ====
            addSectionHeader(doc, "Détails de la commande");

            PdfPTable table = new PdfPTable(new float[]{2.8f, 1.1f, 1.0f, 0.9f, 1.2f, 1.3f});
            table.setWidthPercentage(100);
            table.getDefaultCell().setBorderColor(BORDER);

            // en-têtes
            table.addCell(th("Produit"));
            table.addCell(th("Statut"));
            table.addCell(th("Colisage"));
            table.addCell(th("Qté"));
            table.addCell(th("PU (DH)"));
            table.addCell(th("Total (DH)"));

            // lignes (zebra)
            boolean alt = false;
            if (cmd.getLignes() != null) {
                for (LigneCommande l : cmd.getLignes()) {
                    boolean rupture = false;
                    try {
                        rupture = l.getStatutProduit() == com.project.entity.LigneCommandeStatus.RUPTURE_STOCK;
                    } catch (Exception ignored) {}
                    String produit = l.getProduit() != null ? l.getProduit().getNom() : "-";
                    table.addCell(tdZ(produit, alt));
                    table.addCell(tdZ(rupture ? "RUPTURE" : "Incluse", alt));
                    String colStr = lookupColisage(cmd, l);
                    table.addCell(tdZ(colStr, alt));
                    table.addCell(tdZ(String.valueOf(l.getQuantite()), alt));
                    table.addCell(tdZ(String.format("%.2f", l.getPrixUnitaire()), alt));
                    table.addCell(tdZ(String.format("%.2f", l.getPrixUnitaire() * l.getQuantite()), alt));
                    alt = !alt;
                }
            }
            table.setSpacingAfter(10f);
            doc.add(table);

            // ==== PANNEAU TOTAUX ====
            PdfPTable totalsBox = new PdfPTable(new float[]{2.2f, 2.2f});
            totalsBox.setWidthPercentage(60);
            totalsBox.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Font totalKey = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font totalVal = new Font(Font.HELVETICA, 12, Font.BOLD, BRAND);

            PdfPCell qKey = new PdfPCell(new Phrase("Quantité totale", totalKey));
            stylePanelCell(qKey, LABEL_BG, Element.ALIGN_LEFT);
            PdfPCell qVal = new PdfPCell(new Phrase(String.valueOf(cmd.getTotalQuantite()), totalVal));
            stylePanelCell(qVal, Color.WHITE, Element.ALIGN_RIGHT);

            PdfPCell tKey = new PdfPCell(new Phrase("Total après modification (DH)", totalKey));
            stylePanelCell(tKey, LABEL_BG, Element.ALIGN_LEFT);
            PdfPCell tVal = new PdfPCell(new Phrase(String.format("%.2f", cmd.getTotalPrix()), totalVal));
            stylePanelCell(tVal, Color.WHITE, Element.ALIGN_RIGHT);

            totalsBox.addCell(qKey); totalsBox.addCell(qVal);
            totalsBox.addCell(tKey); totalsBox.addCell(tVal);
            if (cmd.getTotalAvantModification() != null) {
                PdfPCell bKey = new PdfPCell(new Phrase("Total avant modification (DH)", totalKey));
                stylePanelCell(bKey, LABEL_BG, Element.ALIGN_LEFT);
                PdfPCell bVal = new PdfPCell(new Phrase(String.format("%.2f", cmd.getTotalAvantModification()), totalVal));
                stylePanelCell(bVal, Color.WHITE, Element.ALIGN_RIGHT);
                totalsBox.addCell(bKey); totalsBox.addCell(bVal);
            }
            totalsBox.setSpacingAfter(16f);
            doc.add(totalsBox);

            // ==== Pied de page ====
            Font fSmall = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.DARK_GRAY);
            Paragraph footer = new Paragraph("Merci pour votre confiance.", fSmall);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF : " + e.getMessage(), e);
        }
    }

    // ---------- UI helpers ----------

    private void addBrandBand(Document doc) throws Exception {
        PdfPTable band = new PdfPTable(new float[]{1f, 3.8f});
        band.setWidthPercentage(100);
        band.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // fond bandeau
        PdfPCell bgL = new PdfPCell();
        bgL.setBackgroundColor(BAND_BG);
        bgL.setBorder(Rectangle.NO_BORDER);
        PdfPCell bgR = new PdfPCell();
        bgR.setBackgroundColor(BAND_BG);
        bgR.setBorder(Rectangle.NO_BORDER);
        band.addCell(bgL); band.addCell(bgR);

        // Re-ouvre la table pour écrire par-dessus (technique simple pour fond sur toute largeur)
        band = new PdfPTable(new float[]{1f, 3.8f});
        band.setWidthPercentage(100);
        band.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        Image logo = loadLogo();
        PdfPCell logoCell;
        if (logo != null) {
            logo.scaleToFit(70, 70);
            logoCell = new PdfPCell(logo, false);
        } else {
            logoCell = new PdfPCell(new Phrase(" "));
        }
        logoCell.setBackgroundColor(BAND_BG);
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(6f);
        logoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Font companyFont = new Font(Font.HELVETICA, 20, Font.BOLD, BRAND);
        Paragraph company = new Paragraph("PHARMACEUTICAL INSTITUTE", companyFont);
        company.setAlignment(Element.ALIGN_LEFT);

        PdfPCell nameCell = new PdfPCell(company);
        nameCell.setBackgroundColor(BAND_BG);
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setPadding(10f);
        nameCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        band.addCell(logoCell);
        band.addCell(nameCell);
        band.setSpacingAfter(8f);
        doc.add(band);

        // fine ligne séparatrice
        Paragraph sep = new Paragraph(" ");
        sep.setSpacingAfter(2f);
        doc.add(sep);
    }

    private void addSectionHeader(Document doc, String text) throws Exception {
        Font sh = new Font(Font.HELVETICA, 12, Font.BOLD, BRAND);
        Paragraph p = new Paragraph(text, sh);
        p.setSpacingBefore(6f);
        p.setSpacingAfter(6f);
        doc.add(p);
    }

    private static PdfPCell labelCell(String txt) {
        PdfPCell c = new PdfPCell(new Phrase(txt, new Font(Font.HELVETICA, 11, Font.BOLD)));
        c.setBackgroundColor(LABEL_BG);
        c.setPadding(6f);
        c.setBorderColor(BORDER);
        return c;
    }

    private static PdfPCell valueCell(String txt, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(txt == null ? "-" : txt, f));
        c.setPadding(6f);
        c.setBorderColor(BORDER);
        return c;
    }

    private static PdfPCell th(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 11, Font.BOLD)));
        c.setPadding(6f);
        c.setBackgroundColor(TABLE_HDR);
        c.setBorderColor(BORDER);
        return c;
    }

    private static PdfPCell tdZ(String text, boolean alt) {
        PdfPCell c = new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 11, Font.NORMAL)));
        c.setPadding(6f);
        c.setBorderColor(BORDER);
        if (alt) c.setBackgroundColor(ROW_ALT);
        return c;
    }

    private static void stylePanelCell(PdfPCell c, Color bg, int align) {
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(8f);
        c.setBorderColor(BORDER);
    }

    private static String nullToDash(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }

    /** Récupère le colisage depuis l'offre pour la ligne (si disponible). */
    private String lookupColisage(Commande cmd, LigneCommande l) {
        try {
            if (cmd == null || cmd.getOffre() == null || cmd.getOffre().getProduits() == null) return "-";
            if (l == null || l.getProduit() == null || l.getProduit().getId() == null) return "-";
            Long pid = l.getProduit().getId();
            for (OffreProduit op : cmd.getOffre().getProduits()) {
                if (op != null && op.getProduit() != null && pid.equals(op.getProduit().getId())) {
                    Integer c = op.getColisage();
                    return c != null ? String.valueOf(c) : "-";
                }
            }
        } catch (Exception ignored) {}
        return "-";
    }

    // ---------- Logo loader robuste ----------
    private Image loadLogo() {
        // 1) Essais via ClassPathResource
        for (String path : LOGO_CANDIDATES) {
            try (InputStream in = new ClassPathResource(path).getInputStream()) {
                byte[] bytes = in.readAllBytes();
                return Image.getInstance(bytes);
            } catch (Exception ignored) { }
        }
        // 2) Essais via ClassLoader (quelques variantes)
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (String path : LOGO_CANDIDATES) {
            try (InputStream in = cl.getResourceAsStream(path.startsWith("/") ? path.substring(1) : path)) {
                if (in != null) {
                    byte[] bytes = in.readAllBytes();
                    return Image.getInstance(bytes);
                }
            } catch (Exception ignored) { }
        }
        // 3) Fallback disque (utile en dev)
        try {
            File f = new File("src/main/resources/static/images/logo4.png");
            if (!f.exists()) f = new File("src/main/resources/images/logo4.png");
            if (!f.exists()) f = new File("images" + File.separator + "logo4.png");
            if (f.exists()) {
                byte[] bytes = Files.readAllBytes(f.toPath());
                return Image.getInstance(bytes);
            }
        } catch (Exception ignored) { }
        return null;
    }
}
