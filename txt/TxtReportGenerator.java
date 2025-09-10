package com.mmd.txt;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TxtReportGenerator {
    private final boolean afficherDansTerminal;
    private String txtFilePath;
    private String xlsxFilePath;
    private String csvFilePath;

    public TxtReportGenerator(boolean afficherDansTerminal) {
        this.afficherDansTerminal = afficherDansTerminal;
    }

    private void afficher(String ligne) {
        if (afficherDansTerminal) System.out.println(ligne);
    }

    public void generateReports(String dossierRapport, StringBuilder rapportTexte, List<String[]> xlsxData, TxtSimpleConfigReader ConfigReader) throws IOException {
        String horodatage = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Files.createDirectories(Paths.get(dossierRapport));
        String baseName = ConfigReader.getReportFileName();
        String txtPath = Paths.get(dossierRapport, baseName + "_" + horodatage + ".txt").toString();
        txtFilePath = txtPath;
        Files.write(Paths.get(txtPath), rapportTexte.toString().getBytes());

        afficher("* Text (.txt) report saved to : " + txtPath);

        if (afficherDansTerminal) {
            afficher("\n=== Full Text Report ===");
            for (String ligne : rapportTexte.toString().split("\n")) {
                System.out.println(ligne);
            }
            afficher("");
            afficher("* Text (.txt) report generated : " + txtPath);
        } else {
            long totalAjout = 0;
            long totalSupp = 0;
            long totalModif = 0;
            for (String[] l : xlsxData) {
                if ("AJOUT".equals(l[0])) totalAjout++;
                else if ("DELETION".equals(l[0])) totalSupp++;
                else if ("MODIFICATION".equals(l[0])) totalModif++;
            }

            long totalIdentique = 0;
            BufferedReader reader = new BufferedReader(new StringReader(rapportTexte.toString()));
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                if (ligne.startsWith("[UNCHANGED]")) totalIdentique++;
            }

            afficher("");
            afficher("=== Summary ===");
            afficher("* Identical rows     : " + totalIdentique);
            afficher("* Modified rows      : " + totalModif);
            afficher("* Added rows         : " + totalAjout);
            afficher("* Deleted rows       : " + totalSupp);
        }

        exportToXlsx(dossierRapport, horodatage, xlsxData, ConfigReader);
        exportToCsv(dossierRapport, horodatage, xlsxData, ConfigReader);
    }

    private void exportToXlsx(String dossierRapport, String horodatage, List<String[]> xlsxData, TxtSimpleConfigReader ConfigReader) throws IOException{
        Files.createDirectories(Paths.get(dossierRapport));
        String baseName = ConfigReader.getReportFileName();

        String nomFichier = baseName +  "_" + horodatage + ".xlsx";
        Path cheminFichier = Paths.get(dossierRapport, nomFichier);
        xlsxFilePath = cheminFichier.toString();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Differences");

        String[] titres = {"Type", "Key", "Changed field", "Old value", "New value"};
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row header = sheet.createRow(0);
        for (int i = 0; i < titres.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(titres[i]);
            cell.setCellStyle(headerStyle);
        }

        CellStyle ajoutStyle = workbook.createCellStyle();
        ajoutStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        ajoutStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle suppStyle = workbook.createCellStyle();
        suppStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        suppStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle modifStyle = workbook.createCellStyle();
        modifStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        modifStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int rowNum = 1;
        for (String[] ligne : xlsxData) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < ligne.length; i++) {
                row.createCell(i).setCellValue(ligne[i] != null ? ligne[i] : "");
            }

            Cell typeCell = row.getCell(0);
            if (typeCell != null) {
                String type = typeCell.getStringCellValue().toUpperCase();
                if ("AJOUT".equals(type) || "ADD".equals(type)) {
                    typeCell.setCellStyle(ajoutStyle);
                } else if ("SUPPRESSION".equals(type) || "DELETION".equals(type)) {
                    typeCell.setCellStyle(suppStyle);
                } else if ("MODIFICATION".equals(type)) {
                    typeCell.setCellStyle(modifStyle);
                }
            }
        }

        try (FileOutputStream fileOut = new FileOutputStream(cheminFichier.toFile())) {
            workbook.write(fileOut);
        }
        workbook.close();

        afficher("* .xlsx report generated in " + dossierRapport + " : " + nomFichier);
        afficher("* Excel (.xlsx) report generated : " + cheminFichier);
    }

    private void exportToCsv(String dossierRapport, String horodatage, List<String[]> data, TxtSimpleConfigReader ConfigReader) throws IOException{
        String baseName = ConfigReader.getReportFileName();
        String nomFichier = baseName + "_"+  horodatage + ".csv";
        Path chemin = Paths.get(dossierRapport, nomFichier);
        csvFilePath = chemin.toString();

        try (BufferedWriter writer = Files.newBufferedWriter(chemin)) {
            writer.write("Type,Key,Changed field,Old value,New value\n");
            for (String[] ligne : data) {
                writer.write(String.join(",", escapeCsv(ligne)));
                writer.newLine();
            }
        }

        afficher("* CSV (.csv) report generated : " + csvFilePath);
    }

    private String[] escapeCsv(String[] champs) {
        String[] res = new String[champs.length];
        for (int i = 0; i < champs.length; i++) {
            String champ = champs[i] != null ? champs[i] : "";
            if (champ.contains(",") || champ.contains("\"") || champ.contains("\n")) {
                champ = champ.replace("\"", "\"\"");
                champ = '"' + champ + '"';
            }
            res[i] = champ;
        }
        return res;
    }

    public String getTxtFilePath() {
        return txtFilePath;
    }

    public String getXlsxFilePath() {
        return xlsxFilePath;
    }

    public String getCsvFilePath() {
        return csvFilePath;
    }
}
