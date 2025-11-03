package com.mmd.json;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportGenerator {

    private final String outputFolder;
    private final String baseName;
    private final String timestamp;

    public ReportGenerator(String outputFolder, String baseName) {
        this.outputFolder = outputFolder;
        this.baseName = baseName;
        this.timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }

    // =====================================================
    // TXT
    // =====================================================
    public void generateTextReport(List<Difference> diffs) throws IOException {
        Path out = Paths.get(outputFolder, baseName + "_" + timestamp + ".txt");
        try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            for (Difference d : diffs) {
                writer.write("[Object] " + d.getEntityId() + "\n");
                writer.write("[" + d.getType().name() + "]\n");
                writer.write("* Section: " + d.getSection() + " | Key: " + d.getField() + "\n");
                if (d.getType() == ChangeType.MODIFICATION) {
                    writer.write("~ Old: " + safe(d.getOldValue()) + "\n");
                    writer.write("~ New: " + safe(d.getNewValue()) + "\n");
                } else if (d.getType() == ChangeType.ADDITION) {
                    writer.write("+ New Value: " + safe(d.getNewValue()) + "\n");
                } else if (d.getType() == ChangeType.DELETION) {
                    writer.write("- Reference file value: " + safe(d.getOldValue()) + "\n");
                } else if (d.getType() == ChangeType.IGNORED) {
                    writer.write("~ Ignored field from config\n");
                }
                writer.write("\n");
            }
        }
        System.out.println("[TXT] Report written → " + out.toAbsolutePath());
    }

    // =====================================================
    // CSV (sans IGNORED)
    // =====================================================
    public void generateCsvReport(List<Difference> diffs) throws IOException {
        Path out = Paths.get(outputFolder, baseName + "_" + timestamp + ".csv");
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("Type;EntityId;Section;Field;OldValue;NewValue\n");
            for (Difference d : diffs) {
                if (d.getType() == ChangeType.IGNORED) continue; // exclusion
                w.write(String.join(";",
                        d.getType().toString(),
                        nullToEmpty(d.getEntityId()),
                        nullToEmpty(d.getSection()),
                        nullToEmpty(d.getField()),
                        nullToEmpty(d.getOldValue()),
                        nullToEmpty(d.getNewValue())
                ));
                w.write("\n");
            }
        }
        System.out.println("[CSV] Report written → " + out.toAbsolutePath());
    }

    // =====================================================
    // EXCEL (sans IGNORED)
    // =====================================================
    public void generateExcelReport(List<Difference> diffs) throws IOException {
        Path out = Paths.get(outputFolder, baseName + "_" + timestamp + ".xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Differences");

            // En-tête
            CellStyle headerStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Styles colorés
            Map<ChangeType, CellStyle> styles = new EnumMap<>(ChangeType.class);
            styles.put(ChangeType.ADDITION, createStyle(wb, IndexedColors.LIGHT_GREEN));
            styles.put(ChangeType.MODIFICATION, createStyle(wb, IndexedColors.LIGHT_YELLOW));
            styles.put(ChangeType.DELETION, createStyle(wb, IndexedColors.ROSE));

            // Header row
            Row header = sheet.createRow(0);
            String[] headers = {"Type", "EntityId", "Section", "Field", "OldValue", "NewValue"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // Contenu
            int rowIdx = 1;
            for (Difference d : diffs) {
                if (d.getType() == ChangeType.IGNORED) continue;

                Row r = sheet.createRow(rowIdx++);
                CellStyle st = styles.getOrDefault(d.getType(), wb.createCellStyle());
                fillCell(r, 0, d.getType().toString(), st);
                fillCell(r, 1, d.getEntityId(), st);
                fillCell(r, 2, d.getSection(), st);
                fillCell(r, 3, d.getField(), st);
                fillCell(r, 4, d.getOldValue(), st);
                fillCell(r, 5, d.getNewValue(), st);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            try (OutputStream os = Files.newOutputStream(out)) {
                wb.write(os);
            }
        }
        System.out.println("[XLSX] Report written → " + out.toAbsolutePath());
    }

    // =====================================================
    // Helpers
    // =====================================================
    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.replace("\n", " ").replace("\r", " ");
    }

    private static void fillCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static CellStyle createStyle(Workbook wb, IndexedColors color) {
        CellStyle st = wb.createCellStyle();
        st.setFillForegroundColor(color.getIndex());
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return st;
    }
}
