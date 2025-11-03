package com.mmd.json;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportGenerator {
    private final Path outDir;
    private final String baseName;
    private final String timestamp;
    private String refFileName;
    private String newFileName;

    public ReportGenerator(String outFolder, String baseName) {
        this.outDir = Paths.get(outFolder);
        this.baseName = (baseName == null || baseName.isEmpty()) ? "JSONReport" : baseName;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    public void setComparedFiles(String ref, String neu) {
        this.refFileName = ref;
        this.newFileName = neu;
    }

    private Path file(String ext) throws IOException {
        Files.createDirectories(outDir);
        return outDir.resolve(baseName + "_" + timestamp + "." + ext);
    }

    // === TXT ===
    public void generateTextReport(List<Difference> diffs) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file("txt"), StandardCharsets.UTF_8)) {
            if (refFileName != null && newFileName != null) {
                w.write("Compared files:\n");
                w.write(" - Reference file : " + refFileName + "\n");
                w.write(" - New file       : " + newFileName + "\n\n");
            }
            Map<String, List<Difference>> byId = new TreeMap<>();
            for (Difference d : diffs) byId.computeIfAbsent(d.getEntityId(), k -> new ArrayList<>()).add(d);

            for (String id : byId.keySet()) {
                w.write("[Object] " + id + "\n");
                Map<ChangeType, List<Difference>> byType = new EnumMap<>(ChangeType.class);
                for (Difference d : byId.get(id))
                    byType.computeIfAbsent(d.getType(), t -> new ArrayList<>()).add(d);

                for (ChangeType t : ChangeType.values()) {
                    List<Difference> L = byType.get(t);
                    if (L == null || L.isEmpty()) continue;
                    w.write("[" + t + "]\n");
                    for (Difference d : L) {
                        w.write(" * Section: " + d.getSection() + " | Key: " + d.getKey() + "\n");
                        if (t == ChangeType.DELETION) {
                            w.write("   - Reference file value: " + d.getOldValue() + "\n");
                        } else if (t == ChangeType.ADDITION) {
                            w.write("   + New file value: " + d.getNewValue() + "\n");
                        } else {
                            w.write("   ~ Old: " + d.getOldValue() + "\n");
                            w.write("   ~ New: " + d.getNewValue() + "\n");
                        }
                    }
                    w.write("\n");
                }
                w.write("\n");
            }
        }
    }

    // === CSV ===
    public void generateCsvReport(List<Difference> diffs) throws IOException {
        String[] headers = { "ID", "Type", "Section", "Key", "Old Value", "New Value" };
        try (BufferedWriter w = Files.newBufferedWriter(file("csv"), StandardCharsets.UTF_8)) {
            w.write(String.join(";", headers)); w.newLine();
            for (Difference d : diffs) {
                String[] row = {
                        d.getEntityId(),
                        d.getType().name(),
                        d.getSection(),
                        d.getKey(),
                        safe(d.getOldValue()),
                        safe(d.getNewValue())
                };
                w.write(String.join(";", escapeCsv(row)));
                w.newLine();
            }
        }
    }

    // === XLSX ===
    public void generateExcelReport(List<Difference> diffs) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Differences");
            String[] headers = { "ID", "Type", "Section", "Key", "Old Value", "New Value" };

            // header
            Row h = sh.createRow(0);
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);

            // simple styles by type
            CellStyle add = wb.createCellStyle();
            CellStyle del = wb.createCellStyle();
            CellStyle mod = wb.createCellStyle();
            add.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex()); add.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            del.setFillForegroundColor(IndexedColors.ROSE.getIndex());        del.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            mod.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());mod.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            int r = 1;
            for (Difference d : diffs) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(d.getEntityId());
                row.createCell(1).setCellValue(d.getType().name());
                row.createCell(2).setCellValue(d.getSection());
                row.createCell(3).setCellValue(d.getKey());
                row.createCell(4).setCellValue(safe(d.getOldValue()));
                row.createCell(5).setCellValue(safe(d.getNewValue()));

                CellStyle st = switch (d.getType()) {
                    case ADDITION -> add;
                    case DELETION -> del;
                    default -> mod;
                };
                for (int c = 0; c < 6; c++) row.getCell(c).setCellStyle(st);
            }
            for (int c = 0; c < 6; c++) sh.autoSizeColumn(c);

            try (OutputStream os = Files.newOutputStream(file("xlsx"))) {
                wb.write(os);
            }
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String[] escapeCsv(String[] fields) {
        String[] out = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String f = fields[i] == null ? "" : fields[i];
            if (f.contains(";") || f.contains("\"") || f.contains("\n")) {
                f = "\"" + f.replace("\"", "\"\"") + "\"";
            }
            out[i] = f;
        }
        return out;
    }
}
