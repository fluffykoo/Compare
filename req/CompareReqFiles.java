package com.mmd.req;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CompareReqFiles {


    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 4) {
            ReqLogger.log("Usage: java CompareReqFiles <file1.req> <file2.req> [output_folder] [reportFileName]");
            return;
        }

        String file1 = args[0];
        String file2 = args[1];
        String outputFolder = (args.length >= 3) ? args[2] : ".";
        String reportFileName = (args.length == 4) ? args[3] : null;

        if (!new File(file1).exists() || !new File(file2).exists()) {
            ReqLogger.log("Error: One of the .req files does not exist.");
            return;
        }

        List<String> lines1 = Files.readAllLines(Paths.get(file1));
        List<String> lines2 = Files.readAllLines(Paths.get(file2));

        Map<String, List<String>> sections1 = ReqSectionParser.extractSections(lines1);
        Map<String, List<String>> sections2 = ReqSectionParser.extractSections(lines2);

        List<String> header1 = ReqUtils.clean(sections1.get("Header"));
        List<String> header2 = ReqUtils.clean(sections2.get("Header"));
        List<String> fields1 = ReqUtils.clean(sections1.get("Fields"));
        List<String> fields2 = ReqUtils.clean(sections2.get("Fields"));
        List<String> data1 = ReqUtils.clean(sections1.get("DATA"));
        List<String> data2 = ReqUtils.clean(sections2.get("DATA"));

        int added = 0, deleted = 0, modified = 0;
        List<String[]> xlsxRows = new ArrayList<>();
        StringBuilder rapportTxt = new StringBuilder();


        // HEADER - clé/valeur
        rapportTxt.append("-- Section: Header --\n");
        Map<String, String> map1 = ReqUtils.toMap(header1);
        Map<String, String> map2 = ReqUtils.toMap(header2);

        for (String key : map1.keySet()) {
            if (!map2.containsKey(key)) {
                rapportTxt.append("[Removed] ").append(key).append("=").append(map1.get(key)).append("\n");
                xlsxRows.add(new String[]{"DELETION", "Header", key, map1.get(key), ""});
                deleted++;
            } else if (!map1.get(key).equals(map2.get(key))) {
                rapportTxt.append("[Modified] ").append(key).append("\n")
                        .append("  Reference: ").append(map1.get(key)).append("\n")
                        .append("  New: ").append(map2.get(key)).append("\n");
                xlsxRows.add(new String[]{"MODIFICATION", "Header", key, map1.get(key), map2.get(key)});
                modified++;
            }
        }

        for (String key : map2.keySet()) {
            if (!map1.containsKey(key)) {
                rapportTxt.append("[Added] ").append(key).append("=").append(map2.get(key)).append("\n");
                xlsxRows.add(new String[]{"ADDITION", "Header", key, "", map2.get(key)});
                added++;
            }
        }

        // FIELDS
        rapportTxt.append("\n-- Section: Fields --\n");
        Set<String> setF1 = new LinkedHashSet<>(fields1);
        Set<String> setF2 = new LinkedHashSet<>(fields2);

        for (String line : setF1) {
            if (!setF2.contains(line)) {
                rapportTxt.append("[Removed] ").append(line).append("\n");
                xlsxRows.add(new String[]{"DELETION", "Fields", line, line, ""});
                deleted++;
            }
        }

        for (String line : setF2) {
            if (!setF1.contains(line)) {
                rapportTxt.append("[Added] ").append(line).append("\n");
                xlsxRows.add(new String[]{"ADDITION", "Fields", line, "", line});
                added++;
            }
        }

        // DATA
        rapportTxt.append("\n-- Section: DATA --\n");
        Set<String> setD1 = new LinkedHashSet<>(data1);
        Set<String> setD2 = new LinkedHashSet<>(data2);

        for (String line : setD1) {
            if (!setD2.contains(line)) {
                rapportTxt.append("[Removed] ").append(line).append("\n");
                xlsxRows.add(new String[]{"DELETION", "DATA", line, line, ""});
                deleted++;
            }
        }

        for (String line : setD2) {
            if (!setD1.contains(line)) {
                rapportTxt.append("[Added] ").append(line).append("\n");
                xlsxRows.add(new String[]{"ADDITION", "DATA", line, "", line});
                added++;
            }
        }
        // Comptage des lignes identiques
        int iso = 0;
        Set<String> isoHeader = new LinkedHashSet<>(header1);
        isoHeader.retainAll(header2);
        iso += isoHeader.size();

        Set<String> isoFields = new LinkedHashSet<>(fields1);
        isoFields.retainAll(fields2);
        iso += isoFields.size();

        Set<String> isoData = new LinkedHashSet<>(data1);
        isoData.retainAll(data2);
        iso += isoData.size();

    // header terminal
    System.out.println();
    System.out.println("=== Req files comparison ===");
    // Export des rapports
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    ReqLogger.exportReports(outputFolder, timestamp, file1, file2, rapportTxt.toString(), xlsxRows, reportFileName);

        // Summary
        System.out.println();
        System.out.println("\n=== Summary ===");
        System.out.println("Reference file: " + file1);
        System.out.println("Compared file : " + file2);
        System.out.println("Lines added   : " + added);
        System.out.println("Lines removed : " + deleted);
        System.out.println("Lines modified: " + modified);
        System.out.println("Lines identical: " + iso);
        System.out.println("Total diff    : " + (added + deleted + modified));


    }
}



