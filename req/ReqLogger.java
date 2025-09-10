package com.mmd.req;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ReqLogger {

    public static void log(String line) {
        System.out.println(line);
    }


    public static void exportReports(String folder, String timestamp, String refFile, String newFile,
                                     String fullTextReport, List<String[]> rows, String reportFileName) throws IOException {

        Files.createDirectories(Paths.get(folder));
        String baseName = (reportFileName != null && !reportFileName.trim().isEmpty()) ? reportFileName : "REQReport";
        String txtName = baseName + "_" + timestamp + ".txt";
        String csvName = baseName + "_" + timestamp + ".csv";
        String xlsxName = baseName + "_" + timestamp + ".xlsx";

        // TXT
        Path txtPath = Paths.get(folder, txtName);
        StringBuilder headerTxt = new StringBuilder();
        headerTxt.append("=== Req files comparison ===\n");
        headerTxt.append("Reference file : ").append(refFile).append("\n");
        headerTxt.append("Compared file  : ").append(newFile).append("\n\n");
        headerTxt.append(fullTextReport);

        Files.write(txtPath, headerTxt.toString().getBytes());

        // CSV
        Path csvPath = Paths.get(folder, csvName);
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath)) {
            writer.write("Type,Section,Key,Reference Value,New Value\n");
            for (String[] row : rows) {
                writer.write(String.join(",", escape(row[0]), escape(row[1]), escape(row[2]),
                        escape(row[3]), escape(row[4])));
                writer.write("\n");
            }
        }

        // XLSX
        Path xlsxPath = Paths.get(folder, xlsxName);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("REQ Comparison");
            Row header = sheet.createRow(0);
            String[] titles = {"Type", "Section", "Key", "Reference Value", "New Value"};
            for (int i = 0; i < titles.length; i++) {
                header.createCell(i).setCellValue(titles[i]);
            }

            int rowNum = 1;
            for (String[] row : rows) {
                Row xlsRow = sheet.createRow(rowNum++);
                for (int i = 0; i < row.length; i++) {
                    xlsRow.createCell(i).setCellValue(row[i]);
                }
            }

            try (FileOutputStream out = new FileOutputStream(xlsxPath.toFile())) {
                workbook.write(out);
            }
        }

        log("\nText report generated  : " + txtPath.toAbsolutePath());
        log("CSV report generated   : " + csvPath.toAbsolutePath());
        log("Excel report generated : " + xlsxPath.toAbsolutePath());
    }

    private static String escape(String value) {
        return value.replace("\"", "\"\"").replace(",", " ");
    }
}
