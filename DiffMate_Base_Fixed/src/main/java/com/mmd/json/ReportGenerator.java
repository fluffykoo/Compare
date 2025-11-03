package com.mmd.json;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ReportGenerator {

    private final String outputFolder;
    private final String timestamp;
    private String baseName = "JSONReport";
    private String refFileName = null;
    private String newFileName = null;

    public ReportGenerator(String outputFolder, String timestamp, String baseName) {
        this.outputFolder = outputFolder;
        this.timestamp = timestamp;
        if (baseName != null && !baseName.isEmpty()) this.baseName = baseName;
    }

    public void setComparedFiles(String refFileName, String newFileName) {
        this.refFileName = refFileName;
        this.newFileName = newFileName;
    }

    // === TXT REPORT ===
    public void generateTextReport(List<Difference> differences) throws IOException {
        String fileName = Paths.get(outputFolder, baseName + "_" + timestamp + ".txt").toString();
        Files.createDirectories(Paths.get(outputFolder));
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(fileName), StandardCharsets.UTF_8)) {
            if (refFileName != null && newFileName != null) {
                w.write("Compared files:\n");
                w.write(" - Reference file : " + refFileName + "\n");
                w.write(" - New file       : " + newFileName + "\n\n");
            }
            Map<ChangeType, List<Difference>> byType = new EnumMap<>(ChangeType.class);
            for (Difference d : differences) {
                byType.computeIfAbsent(d.getType(), t -> new ArrayList<>()).add(d);
            }
            for (ChangeType t : ChangeType.values()) {
                List<Difference> list = byType.get(t);
                if (list == null || list.isEmpty()) continue;
                w.write("[" + t.name() + "]\n");
                for (Difference d : list) {
                    w.write("  * " + d.getEntityId() + " | " + d.getKey());
                    if (d.getOldValue() != null) w.write(" | ref=" + d.getOldValue());
                    if (d.getNewValue() != null) w.write(" | new=" + d.getNewValue());
                    w.write("\n");
                }
                w.write("\n");
            }
        }
    }

    // === CSV REPORT ===
    public void generateCsvReport(List<Difference> differences) throws IOException {
        String fileName = Paths.get(outputFolder, baseName + "_" + timestamp + ".csv").toString();
        Files.createDirectories(Paths.get(outputFolder));
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(fileName), StandardCharsets.UTF_8)) {
            w.write("ID,Type,Section,Key,Old Value,New Value\n");
            for (Difference d : differences) {
                w.write(escape(d.getEntityId())); w.write(",");
                w.write(d.getType().name()); w.write(",");
                w.write(escape(d.getSection())); w.write(",");
                w.write(escape(d.getKey())); w.write(",");
                w.write(escape(d.getOldValue())); w.write(",");
                w.write(escape(d.getNewValue())); w.write("\n");
            }
        }
    }

    // === EXCEL REPORT ===
    public void generateExcelReport(List<Difference> differences) throws IOException {
        String fileName = Paths.get(outputFolder, baseName + "_" + timestamp + ".xlsx").toString();
        Files.createDirectories(Paths.get(outputFolder));

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Differences");
        String[] headers = {"ID", "Type", "Section", "Key", "Old Value", "New Value"};

        // Header
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }

        // Data
        int rowNum = 1;
        for (Difference diff : differences) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(nullToEmpty(diff.getEntityId()));
            row.createCell(1).setCellValue(diff.getType().name());
            row.createCell(2).setCellValue(nullToEmpty(diff.getSection()));
            row.createCell(3).setCellValue(nullToEmpty(diff.getKey()));
            row.createCell(4).setCellValue(nullToEmpty(diff.getOldValue()));
            row.createCell(5).setCellValue(nullToEmpty(diff.getNewValue()));
        }

        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

        try (FileOutputStream out = new FileOutputStream(fileName)) {
            workbook.write(out);
        }
        workbook.close();
    }

    private String escape(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
