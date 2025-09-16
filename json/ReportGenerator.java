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

    public void setComparedFiles(String refFileName, String newFileName) {
        this.refFileName = refFileName;
        this.newFileName = newFileName;
    }

    public void generateTextReport(List<Difference> differences) throws IOException {
        StringBuilder report = new StringBuilder();
        if (this.refFileName != null && this.newFileName != null) {
            report.append("Compared files:\n");
            report.append(" - Reference file: ").append(this.refFileName).append("\n");
            report.append(" - New file : ").append(this.newFileName).append("\n\n");
        }

        Map<String, List<Difference>> byEntityId = groupByEntityId(differences);
        Set<String> allRefIds = new HashSet<>();
        Set<String> allNewIds = new HashSet<>();

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
            // ignore or log error
        }

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

        // Detailed differences can be appended here as needed...

        // Save the report file
        String fileName = baseName + "_" + timestamp + ".txt";
        Path filePath = Paths.get(outputFolder, fileName);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

        System.out.println("\n=== Json files comparison ===");
        System.out.println("Text (.txt) report generated: " + fileName);
    }

    // Generates other report formats (Excel, CSV) as in the original code
    // ... (omitted here for brevity, keep your existing implementations) ...

    // Group differences by entity ID
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

    // --- New additions ---

    public static class SummaryStats {
        public int identical;
        public int added;
        public int deleted;
        public int totalRef;
        public int totalNew;
    }

    public SummaryStats calculateSummaryStats(List<Difference> differences, Set<String> refIds, Set<String> newIds) {
        SummaryStats stats = new SummaryStats();
        stats.totalRef = refIds.size();
        stats.totalNew = newIds.size();

        Map<String, List<Difference>> byEntityId = groupByEntityId(differences);

        stats.added = 0;
        stats.deleted = 0;
        for (Difference diff : differences) {
            if (diff.getType() == ChangeType.ADDITION) stats.added++;
            else if (diff.getType() == ChangeType.DELETION) stats.deleted++;
        }

        stats.identical = 0;
        for (String id : refIds) {
            if (newIds.contains(id) && (!byEntityId.containsKey(id) || byEntityId.get(id).isEmpty())) {
                stats.identical++;
            }
        }

        return stats;
    }
}
