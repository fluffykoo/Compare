package com.mmd.json;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ReportGenerator {
    private String outputFolder;
    private String timestamp;
    private String baseName;
    private String refFileName;
    private String newFileName;

    public ReportGenerator(String outputFolder, String timestamp, String baseName) {
        this.outputFolder = outputFolder;
        this.timestamp = timestamp;
        this.baseName = baseName;
    }

    public void setComparedFiles(String refFileName, String newFileName) {
        this.refFileName = refFileName;
        this.newFileName = newFileName;
    }

    public void generateTextReport(List<Difference> differences) throws IOException {
        String fileName = outputFolder + File.separator + baseName + "_" + timestamp + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8))) {
            for (Difference diff : differences) {
                writer.write(diff.toString());
                writer.newLine();
            }
        }
    }

    public void generateCsvReport(List<Difference> differences) throws IOException {
        String fileName = outputFolder + File.separator + baseName + "_" + timestamp + ".csv";
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8))) {
            writer.write("ID;Type;Section;Key;OldValue;NewValue\n");
            for (Difference diff : differences) {
                writer.write(String.join(";",
                        diff.getEntityId(),
                        diff.getType().toString(),
                        diff.getSection(),
                        diff.getKey(),
                        diff.getOldValue(),
                        diff.getNewValue()
                ));
                writer.newLine();
            }
        }
    }

    public void generateExcelReport(List<Difference> differences) throws IOException {
        String fileName = outputFolder + File.separator + baseName + "_" + timestamp + ".xlsx";
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Differences");
            Row header = sheet.createRow(0);
            String[] headers = {"ID", "Type", "Section", "Key", "Old Value", "New Value"};
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);

            int rowNum = 1;
            for (Difference diff : differences) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(diff.getEntityId());
                row.createCell(1).setCellValue(diff.getType().toString());
                row.createCell(2).setCellValue(diff.getSection());
                row.createCell(3).setCellValue(diff.getKey());
                row.createCell(4).setCellValue(diff.getOldValue());
                row.createCell(5).setCellValue(diff.getNewValue());
            }

            try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
                workbook.write(fileOut);
            }
        }
    }
}
