package com.mmd.json;


import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mmd.json.Difference;
import com.mmd.json.ChangeType;


public class ReportGenerator {
    private String outputFolder;
    private String timestamp;
    private String baseName = "JSONReport";
    private String refFileName = null;
    private String newFileName = null;

    public ReportGenerator(String outputFolder, String timestamp, String baseName) {
        this.outputFolder = outputFolder;
        this.timestamp = timestamp;
        if (baseName != null && !baseName.isEmpty()) {
            this.baseName = baseName;
        }
    }

    // Permet de passer les noms de fichiers à afficher dans le rapport
    public void setComparedFiles(String refFileName, String newFileName) {
        this.refFileName = refFileName;
        this.newFileName = newFileName;
    }

    public void generateTextReport(List<Difference> differences) throws IOException {
        StringBuilder report = new StringBuilder();

        // Affichage des fichiers à comparer
        if (this.refFileName != null && this.newFileName != null) {
            report.append("Compared files:\n");
            report.append("  - Reference file: ").append(this.refFileName).append("\n");
            report.append("  - New file     : ").append(this.newFileName).append("\n\n");
        }

        // Regroupement par ID d'entité
        Map<String, List<Difference>> byEntityId = groupByEntityId(differences);

        // Calcul des objets identiques, ajoutés, supprimés
        Set<String> allRefIds = new HashSet<>();
        Set<String> allNewIds = new HashSet<>();
        //lire les fichiers JSON pour compter les objets
        int totalRef = 0;
        int totalNew = 0;
        try {
            if (this.refFileName != null && this.newFileName != null) {
                JsonArray refArray = com.google.gson.JsonParser.parseReader(new FileReader(this.refFileName)).getAsJsonArray();
                JsonArray newArray = com.google.gson.JsonParser.parseReader(new FileReader(this.newFileName)).getAsJsonArray();
                totalRef = refArray.size();
                totalNew = newArray.size();
                for (int i = 0; i < refArray.size(); i++) {
                    JsonObject obj = refArray.get(i).getAsJsonObject();
                    String id = obj.has("id") ? obj.get("id").getAsString() : String.valueOf(i);
                    allRefIds.add(id);
                }
                for (int i = 0; i < newArray.size(); i++) {
                    JsonObject obj = newArray.get(i).getAsJsonObject();
                    String id = obj.has("id") ? obj.get("id").getAsString() : String.valueOf(i);
                    allNewIds.add(id);
                }
            }
        } catch (Exception e) {
            
        }

        // Pour le vrai nombre d'identiques :
        // 1. Prendre l'ensemble des IDs présents dans les deux fichiers (intersection)
        // 2. Pour chaque ID, s'il n'a aucune différence (pas dans byEntityId), il est identique
        Set<String> allIds = new HashSet<>();
        allIds.addAll(allRefIds);
        allIds.addAll(allNewIds);
        int identiques = 0;
        for (String id : allRefIds) {
            if (allNewIds.contains(id) && (!byEntityId.containsKey(id) || byEntityId.get(id).isEmpty())) {
                identiques++;
            }
        }
        int ajouts = 0;
        int suppressions = 0;
        for (Difference diff : differences) {
            if (diff.getType() == ChangeType.ADDITION) ajouts++;
            if (diff.getType() == ChangeType.DELETION) suppressions++;
        }

        report.append("=== JSON Differences Report ===\n\n");
        report.append("=== Summary ===\n");
        report.append("Reference file: ").append(totalRef).append(" objects\n");
        report.append("New file: ").append(totalNew).append(" objects\n");
        report.append("Identical objects: ").append(identiques).append("\n");
        report.append("Objects added: ").append(ajouts).append("\n");
        report.append("Objects deleted: ").append(suppressions).append("\n\n");

        // Affichage détaillé des différences 
        for (String entityId : new TreeSet<>(byEntityId.keySet())) {
            List<Difference> entityDiffs = byEntityId.get(entityId);
            report.append("[Object (Security, Third party) ").append(entityId).append("]\n");

            Map<ChangeType, List<Difference>> diffsByType = new EnumMap<>(ChangeType.class);
            for (Difference diff : entityDiffs) {
                diffsByType.computeIfAbsent(diff.getType(), k -> new ArrayList<>()).add(diff);
            }

            for (ChangeType type : ChangeType.values()) {
                List<Difference> typedDiffs = diffsByType.get(type);
                if (typedDiffs != null && !typedDiffs.isEmpty()) {
                    switch (type) {
                        case ADDITION:
                            report.append("[Addition]\n");
                            break;
                        case MODIFICATION:
                            report.append("[Modification]\n");
                            break;
                        case DELETION:
                            report.append("[Deletion]\n");
                            break;
                    }


                    for (Difference diff : typedDiffs) {
                        report.append(" * Section: ").append(diff.getSection());
                        report.append(" | ").append(diff.getKey()).append("\n");
                        if (type != ChangeType.ADDITION)
                            report.append(" * Reference file value: ").append(diff.getOldValue()).append("\n");
                        if (type != ChangeType.DELETION)
                            report.append(" * New file value: ").append(diff.getNewValue()).append("\n");
                    }
                    report.append("\n");
                }
            }
        }

    // Sauvegarde du fichier (UTF-8)
    String fileName = baseName + "_" + timestamp + ".txt";
    Path filePath = Paths.get(outputFolder, fileName);
    Files.createDirectories(filePath.getParent());
    Files.write(filePath, report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    System.out.println();
    System.out.println("=== Json files comparison ===");
    System.out.println("Text (.txt) report generated: " + fileName);
    }

    public void generateExcelReport(List<Difference> differences) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Differences");

        // Création de l'en-tête
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Type", "Section", "KEY", "OLD VALUE", "NEW VALUE"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
        }

        // Création des styles
        CellStyle ajoutStyle = workbook.createCellStyle();
        ajoutStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        ajoutStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle suppressionStyle = workbook.createCellStyle();
        suppressionStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        suppressionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle modificationStyle = workbook.createCellStyle();
        modificationStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        modificationStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Remplissage des données
        int rowNum = 1;
        for (Difference diff : differences) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(diff.getEntityId());
            row.createCell(1).setCellValue(diff.getType().name());
            row.createCell(2).setCellValue(diff.getSection());
            row.createCell(3).setCellValue(diff.getKey());
            row.createCell(4).setCellValue(diff.getOldValue() != null ? diff.getOldValue() : "");
            row.createCell(5).setCellValue(diff.getNewValue() != null ? diff.getNewValue() : "");

            // Application du style selon le type de différence
            CellStyle style;
            switch (diff.getType()) {
                case ADDITION:
                    style = ajoutStyle;
                    break;
                case DELETION:
                    style = suppressionStyle;
                    break;
                case MODIFICATION:
                    style = modificationStyle;
                    break;
                default:
                    style = null;
            }

            // Application du style à toute la ligne
            for (int i = 0; i < 6; i++) {
                row.getCell(i).setCellStyle(style);
            }
        }

    // Sauvegarde du fichier
    String fileName = baseName + "_" + timestamp + ".xlsx";
    Path filePath = Paths.get(outputFolder, fileName);
    Files.createDirectories(filePath.getParent());

        try (OutputStream os = Files.newOutputStream(filePath)) {
            workbook.write(os);
        }

        workbook.close();
        System.out.println("Excel (.xlsx) report  generated : " + fileName);
    }

    private Map<String, List<Difference>> groupByEntityId(List<Difference> differences) {
        Map<String, List<Difference>> groups = new LinkedHashMap<>();

        for (Difference diff : differences) {
            if (!groups.containsKey(diff.getEntityId())) {
                groups.put(diff.getEntityId(), new ArrayList<>());
            }
            groups.get(diff.getEntityId()).add(diff);
        }

        return groups;
    }
    public void generateCsvReport(List<Difference> differences) throws IOException {
        String fileName = baseName + "_" + timestamp + ".csv";
        Path filePath = Paths.get(outputFolder, fileName);
        Files.createDirectories(filePath.getParent());

    try (BufferedWriter writer = Files.newBufferedWriter(filePath, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write("ID,Type,Section,Key,Old Value,New Value\n");
            for (Difference diff : differences) {
                writer.write(String.join(",", escapeCsv(new String[]{
                        diff.getEntityId(),
                        diff.getType().name(),
                        diff.getSection(),
                        diff.getKey(),
                        diff.getOldValue() != null ? diff.getOldValue() : "",
                        diff.getNewValue() != null ? diff.getNewValue() : ""
                })));
                writer.newLine();
            }
        }

        System.out.println("CSV (.csv) report generated : " + fileName);
    }

    private String[] escapeCsv(String[] champs) {
        String[] escaped = new String[champs.length];
        for (int i = 0; i < champs.length; i++) {
            String field = champs[i];
            if (field == null) {
                field = "";
            }
            if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
                field = "\"" + field.replace("\"", "\"\"") + "\"";
            }
            escaped[i] = field;
        }
        return escaped;
    }

}