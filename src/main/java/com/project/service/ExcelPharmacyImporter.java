package com.project.service;

import com.project.entity.Pharmacy;
import com.project.entity.Secteur;
import com.project.repository.PharmacyRepository;
import com.project.repository.SecteurRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class ExcelPharmacyImporter {

    private final PharmacyRepository pharmacyRepo;
    private final SecteurRepository secteurRepo;

    public ExcelPharmacyImporter(PharmacyRepository pharmacyRepo, SecteurRepository secteurRepo) {
        this.pharmacyRepo = pharmacyRepo;
        this.secteurRepo = secteurRepo;
    }

    // --------- Import principal ----------
    public ImportReport importFromExcel(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier manquant.");
        }
        if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Format invalide : fournir un .xlsx");
        }

        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new IllegalArgumentException("Feuille 1 introuvable");

            // lecture header
            Map<String, Integer> header = readHeader(sheet.getRow(0));

            ImportReport report = new ImportReport();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;

                try {
                    String nom = getStringCell(r, header.get("nom"));
                    String adresse = getStringCell(r, header.get("adresse"));
                    String telephone = getStringCell(r, header.get("telephone"));
                    String ice = getStringCell(r, header.get("ice"));
                    String secteurNom = getStringCell(r, header.get("secteur"));   // option1 : secteur par nom
                    Long secteurId = getLongCell(r, header.get("secteur_id"));     // option2 : secteur par id
                    Boolean payed = getBooleanCell(r, header.get("payed"));

                    if (nom == null || nom.isBlank()) throw new IllegalArgumentException("nom obligatoire");
                    if (ice == null || ice.isBlank()) throw new IllegalArgumentException("ice obligatoire");

                    Secteur secteur = null;
                    if (secteurId != null) {
                        secteur = secteurRepo.findById(secteurId).orElse(null);
                        if (secteur == null) throw new IllegalArgumentException("secteur_id inconnu: " + secteurId);
                    } else if (secteurNom != null && !secteurNom.isBlank()) {
                        secteur = secteurRepo.findByNomIgnoreCase(secteurNom).orElse(null);
                        if (secteur == null) throw new IllegalArgumentException("secteur inconnu: " + secteurNom);
                    }

                    // stratégie: si ICE existe → update, sinon insert
                    Pharmacy existing = pharmacyRepo.findByIce(ice).orElse(null);
                    if (existing == null) {
                        Pharmacy p = new Pharmacy();
                        p.setNom(nom);
                        p.setAdresse(adresse);
                        p.setTelephone(telephone);
                        p.setIce(ice);
                        p.setSecteur(secteur);
                        p.setPayed(Boolean.TRUE.equals(payed)); // default false handled below
                        pharmacyRepo.save(p);
                        report.inserted++;
                        report.lines.add("Ligne " + (i+1) + " : INSERT " + nom);
                    } else {
                        existing.setNom(nom);
                        existing.setAdresse(adresse);
                        existing.setTelephone(telephone);
                        existing.setSecteur(secteur);
                        existing.setPayed(Boolean.TRUE.equals(payed));
                        pharmacyRepo.save(existing);
                        report.updated++;
                        report.lines.add("Ligne " + (i+1) + " : UPDATE " + nom);
                    }
                } catch (Exception ex) {
                    report.skipped++;
                    report.lines.add("Ligne " + (i+1) + " : SKIP - " + ex.getMessage());
                }
            }
            return report;
        }
    }

    // --------- Génération d’un modèle ----------
    public ByteArrayOutputStream generateTemplate() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("pharmacies");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("nom");
            header.createCell(1).setCellValue("adresse");
            header.createCell(2).setCellValue("telephone");
            header.createCell(3).setCellValue("secteur");     // par nom
            header.createCell(4).setCellValue("secteur_id");  // ou par id
            header.createCell(5).setCellValue("payed");       // 0/1 ou true/false ou Oui/Non
            header.createCell(6).setCellValue("ice");

            // Exemple
            Row ex = sheet.createRow(1);
            ex.createCell(0).setCellValue("Pharmacie Centrale");
            ex.createCell(1).setCellValue("10 rue X, Ville");
            ex.createCell(2).setCellValue("0600000000");
            ex.createCell(3).setCellValue("Centre-ville");
            ex.createCell(4).setCellValue(""); // vide si renseigné par nom
            ex.createCell(5).setCellValue("Oui");
            ex.createCell(6).setCellValue("MA12345678");

            for (int i = 0; i <= 6; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // --------- Helpers lecture Excel ----------
    private Map<String, Integer> readHeader(Row headerRow) {
        if (headerRow == null) throw new IllegalArgumentException("Header manquant");
        Map<String, Integer> map = new HashMap<>();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) continue;
            String key = cell.getStringCellValue();
            if (key != null) map.put(key.trim().toLowerCase(), c);
        }
        // colonnes minimum
        if (!map.containsKey("nom")) throw new IllegalArgumentException("Colonne 'nom' obligatoire");
        if (!map.containsKey("ice")) throw new IllegalArgumentException("Colonne 'ice' obligatoire");
        return map;
    }

    private String getStringCell(Row r, Integer idx) {
        if (idx == null) return null;
        Cell c = r.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue().trim();
        if (c.getCellType() == CellType.NUMERIC) return String.valueOf((long) c.getNumericCellValue());
        if (c.getCellType() == CellType.BOOLEAN) return c.getBooleanCellValue() ? "true" : "false";
        return null;
    }

    private Long getLongCell(Row r, Integer idx) {
        if (idx == null) return null;
        Cell c = r.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC) return (long) c.getNumericCellValue();
        if (c.getCellType() == CellType.STRING && !c.getStringCellValue().isBlank())
            return Long.parseLong(c.getStringCellValue().trim());
        return null;
    }

    private Boolean getBooleanCell(Row r, Integer idx) {
        if (idx == null) return false;
        Cell c = r.getCell(idx);
        if (c == null) return false;
        if (c.getCellType() == CellType.BOOLEAN) return c.getBooleanCellValue();
        if (c.getCellType() == CellType.NUMERIC) return c.getNumericCellValue() != 0.0;
        if (c.getCellType() == CellType.STRING) {
            String v = c.getStringCellValue().trim().toLowerCase();
            return v.equals("1") || v.equals("true") || v.equals("oui") || v.equals("yes");
        }
        return false;
    }

    // --------- Rapport ----------
    public static class ImportReport {
        private int inserted;
        private int updated;
        private int skipped;
        private final List<String> lines = new ArrayList<>();

        public int getInserted() { return inserted; }
        public int getUpdated() { return updated; }
        public int getSkipped() { return skipped; }
        public List<String> getLines() { return lines; }

        public String toPrettyString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Ajoutées: ").append(inserted)
              .append(" | Mises à jour: ").append(updated)
              .append(" | Ignorées: ").append(skipped).append("\n\nDétails:\n");
            for (String l : lines) sb.append(" - ").append(l).append("\n");
            return sb.toString();
        }
    }
}
