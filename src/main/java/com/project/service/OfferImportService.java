package com.project.service;

import com.project.entity.*;
import com.project.repository.GammeRepository;
import com.project.repository.OffreRepository;
import com.project.repository.ProductRepository;
import com.project.repository.UserRepository;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OfferImportService {

    private final ProductRepository productRepo;
    private final OffreRepository offreRepo;
    private final GammeRepository gammeRepo;

    public OfferImportService(ProductRepository productRepo,
                              OffreRepository offreRepo,
                              GammeRepository gammeRepo) {
        this.productRepo = productRepo;
        this.offreRepo = offreRepo;
        this.gammeRepo = gammeRepo;
    }

    // === Import principal ===
    public OfferImportResult importWorkbook(MultipartFile file, User dsm) throws Exception {
        OfferImportResult result = new OfferImportResult();

        if (file == null || file.isEmpty()) {
            result.getErrors().add("Aucun fichier reçu.");
            return result;
        }
        if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            result.getErrors().add("Le fichier doit être au format .xlsx");
            return result;
        }

        // Gammes accessibles au DSM
        Set<Long> gammeIdsDSM = dsm.getGammes() == null ? Set.of()
                : dsm.getGammes().stream().map(Gamme::getId).collect(Collectors.toSet());

        Map<String, List<Row>> groups = new LinkedHashMap<>();
        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                result.getErrors().add("La première feuille est vide.");
                return result;
            }

            // Header attendu
            // OffreNom | Description | DateDebut | DateFin | Active | ProduitCode | ProduitNom | Prix | Remise% | Min | Max | Colisage (optionnel) | Gamme (optionnel)
            Map<String, Integer> idx = readHeaderIndexes(sheet.getRow(0));
            if (!idx.keySet().containsAll(
                    Set.of("OffreNom","Description","DateDebut","DateFin","Active","Prix","Remise%","Min","Max"))) {
                result.getErrors().add("En-têtes obligatoires manquants. Attendus: OffreNom, Description, DateDebut, DateFin, Active, Prix, Remise%, Min, Max (+ ProduitCode ou ProduitNom). Colisage est optionnel.");
                return result;
            }

            // Regrouper par (OffreNom + DateDebut + DateFin) pour une même offre
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                if (isRowEmpty(row)) continue;

                result.setLignesTraitees(result.getLignesTraitees() + 1);

                String offreNom = readString(row, idx.get("OffreNom"));
                LocalDate debut = readDate(row, idx.get("DateDebut"));
                LocalDate fin = readDate(row, idx.get("DateFin"));

                if (offreNom == null || debut == null || fin == null) {
                    result.getErrors().add("Ligne " + (r+1) + " : OffreNom/DateDebut/DateFin manquant(s).");
                    continue;
                }
                String key = offreNom.trim() + "|" + debut + "|" + fin;
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            }

            // Pour chaque groupe -> créer l'Offre + produits
            for (Map.Entry<String, List<Row>> entry : groups.entrySet()) {
                List<Row> rows = entry.getValue();
                if (rows.isEmpty()) continue;

                Row first = rows.get(0);
                String offreNom = readString(first, idx.get("OffreNom"));
                String description = readString(first, idx.get("Description"));
                LocalDate dateDebut = readDate(first, idx.get("DateDebut"));
                LocalDate dateFin   = readDate(first, idx.get("DateFin"));
                Boolean active      = readBoolean(first, idx.get("Active"));
                String gammeCell    = idx.containsKey("Gamme") ? readString(first, idx.get("Gamme")) : null;

                // Déterminer la gamme : si fournie, vérifier autorisation ; sinon prendre la 1ère gamme du DSM
                final Gamme[] gammeHolder = {null};
                if (gammeCell != null && !gammeCell.isBlank()) {
                    gammeHolder[0] = resolveGammeByNameOrId(gammeCell);
                    if (gammeHolder[0] == null) {
                        result.getErrors().add("Offre '" + offreNom + "': Gamme '" + gammeCell + "' introuvable.");
                        continue;
                    }
                    if (!gammeIdsDSM.contains(gammeHolder[0].getId())) {
                        result.getErrors().add("Offre '" + offreNom + "': Gamme non autorisée pour ce DSM.");
                        continue;
                    }
                } else {
                    if (dsm.getGammes() == null || dsm.getGammes().isEmpty()) {
                        result.getErrors().add("Offre '" + offreNom + "': Ce DSM n'a aucune gamme associée.");
                        continue;
                    }
                    gammeHolder[0] = dsm.getGammes().get(0);
                }
                Gamme gamme = gammeHolder[0];

                Offre offre = new Offre();
                offre.setNom(offreNom);
                offre.setDescription(description);
                offre.setDateDebut(dateDebut);
                offre.setDateFin(dateFin);
                offre.setActive(active != null ? active : true);
                offre.setCreatedByUser(dsm);
                offre.setDateCreation(LocalDate.now().atStartOfDay());
                offre.setGamme(gamme);

                List<OffreProduit> ops = new ArrayList<>();
                for (Row row : rows) {
                    // Résolution produit : par code sinon par nom
                    String code = idx.containsKey("ProduitCode") ? readString(row, idx.get("ProduitCode")) : null;
                    String nom  = idx.containsKey("ProduitNom")  ? readString(row, idx.get("ProduitNom"))  : null;

                    Product produit = null;
                    if (code != null && !code.isBlank()) {
                        produit = productRepo.findByCode(code).orElse(null);
                    }
                    if (produit == null && nom != null && !nom.isBlank()) {
                        produit = productRepo.findByNomIgnoreCase(nom).orElse(null);
                    }
                    if (produit == null) {
                        result.getErrors().add("Offre '" + offreNom + "': produit introuvable (Code: "
                                + code + ", Nom: " + nom + "). Ligne " + (row.getRowNum()+1));
                        continue;
                    }
                    // Sécurité : le produit doit appartenir à la même gamme
                    if (produit.getGammes() == null || produit.getGammes().stream().noneMatch(g -> g.getId().equals(gamme.getId()))) {
                        result.getErrors().add("Offre '" + offreNom + "': produit '" + produit.getNom() + "' n'appartient pas à la gamme '" + gamme.getNom() + "'.");
                        continue;
                    }

                    Double prix = readDouble(row, idx.get("Prix"));
                    Integer remise = readInteger(row, idx.get("Remise%"));
                    Integer qMin = readInteger(row, idx.get("Min"));
                    Integer qMax = readInteger(row, idx.get("Max"));
                    Integer colisage = idx.containsKey("Colisage") ? readInteger(row, idx.get("Colisage")) : null;

                    if (prix == null || remise == null || qMin == null || qMax == null) {
                        result.getErrors().add("Ligne " + (row.getRowNum()+1) + ": Prix/Remise%/Min/Max manquant(s).");
                        continue;
                    }

                    OffreProduit op = new OffreProduit();
                    op.setOffre(offre);
                    op.setProduit(produit);
                    op.setPrix(prix);
                    op.setRemisePourcent(remise);
                    op.setQuantiteMin(qMin);
                    op.setQuantiteMax(qMax);
                    op.setColisage(colisage);
                    ops.add(op);
                }

                if (ops.isEmpty()) {
                    result.getErrors().add("Offre '" + offreNom + "': aucune ligne produit valide.");
                    continue;
                }

                offre.setProduits(ops);
                offreRepo.save(offre);
                result.setOffresCreees(result.getOffresCreees() + 1);
            }
        }

        return result;
    }

    // === Template XLSX ===
    public byte[] generateTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("ImportOffres");
            Row h = sh.createRow(0);
            String[] headers = {
                "OffreNom","Description","DateDebut","DateFin","Active",
                "ProduitCode","ProduitNom","Prix","Remise%","Min","Max","Colisage","Gamme"
            };
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);

            // Exemple
            Row sample = sh.createRow(1);
            sample.createCell(0).setCellValue("Offre Demo");
            sample.createCell(1).setCellValue("Description démo");
            sample.createCell(2).setCellValue("2025-09-01");
            sample.createCell(3).setCellValue("2025-09-30");
            sample.createCell(4).setCellValue("true");
            sample.createCell(5).setCellValue("PDT-001"); // ou vide si nom utilisé
            sample.createCell(6).setCellValue("Doliprane 500"); // ou vide si code utilisé
            sample.createCell(7).setCellValue(12.5);
            sample.createCell(8).setCellValue(10);
            sample.createCell(9).setCellValue(5);
            sample.createCell(10).setCellValue(50);
            sample.createCell(11).setCellValue(12); // Colisage (optionnel)
            sample.createCell(12).setCellValue("Gamme A"); // optionnel

            for (int i = 0; i < headers.length; i++) sh.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ==== Helpers ====
    private Map<String,Integer> readHeaderIndexes(Row header) {
        Map<String,Integer> idx = new HashMap<>();
        if (header == null) return idx;
        for (int c = header.getFirstCellNum(); c < header.getLastCellNum(); c++) {
            Cell cell = header.getCell(c);
            if (cell == null) continue;
            String v = cell.getStringCellValue();
            if (v != null && !v.isBlank()) idx.put(v.trim(), c);
        }
        return idx;
    }

    private boolean isRowEmpty(Row r) {
        if (r == null) return true;
        for (int c = r.getFirstCellNum(); c < r.getLastCellNum(); c++) {
            Cell cell = r.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private String readString(Row r, Integer c) {
        if (c == null) return null;
        Cell cell = r.getCell(c);
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        String s = cell.getStringCellValue();
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private LocalDate readDate(Row r, Integer c) {
        if (c == null) return null;
        Cell cell = r.getCell(c);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else {
            String s = readString(r, c);
            if (s == null) return null;
            try { return LocalDate.parse(s); } catch (Exception e) { return null; }
        }
    }

    private Boolean readBoolean(Row r, Integer c) {
        if (c == null) return null;
        Cell cell = r.getCell(c);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.BOOLEAN) return cell.getBooleanCellValue();
        String s = readString(r, c);
        if (s == null) return null;
        return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("oui") || s.equalsIgnoreCase("1");
    }

    private Double readDouble(Row r, Integer c) {
        if (c == null) return null;
        Cell cell = r.getCell(c);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        String s = readString(r, c);
        if (s == null) return null;
        try { return Double.parseDouble(s.replace(",", ".")); } catch (Exception e) { return null; }
    }

    private Integer readInteger(Row r, Integer c) {
        Double d = readDouble(r, c);
        return d == null ? null : d.intValue();
    }

    private Gamme resolveGammeByNameOrId(String val) {
        // Essaye ID
        try {
            Long id = Long.parseLong(val);
            return gammeRepo.findById(id).orElse(null);
        } catch (NumberFormatException ignore) { }
        // Sinon par nom (ignore case)
        return gammeRepo.findByNomIgnoreCase(val).orElse(null);
    }
}
