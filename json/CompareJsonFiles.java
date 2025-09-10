package com.mmd.json;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import com.mmd.json.ConfigurationManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

public class CompareJsonFiles {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println(
                    "Usage: java CompareJsonFiles <refFile> <newFile> "
                            + "<outputFolder> <configFile>");
            return;
        }
        String refFile   = args[0];
        String newFile   = args[1];
        String outFolder = args[2];
        String cfgFile   = args[3];

    JsonComparator comp = new JsonComparator(cfgFile);
    List<Difference> diffs = comp.compare(refFile, newFile);

    String ts = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    ConfigurationManager configManager = comp.getConfig();
    String baseName = configManager.getReportFileName();
    ReportGenerator gen = new ReportGenerator(outFolder, ts, baseName);
    gen.setComparedFiles(refFile, newFile);
    gen.generateTextReport(diffs);
    gen.generateExcelReport(diffs);
    gen.generateCsvReport(diffs);

        System.out.println("\nIgnored fields:");
        Map<String, List<String>> ignored = comp.getConfig().getIgnoredFields();
        for (Map.Entry<String, List<String>> entry : ignored.entrySet()) {
            System.out.print("  - " + entry.getKey() + " : ");
            for (String field : entry.getValue()) {
                System.out.print(field + " ");
            }
            System.out.println();
        }

    displaySummary(diffs, comp.indexEntities(
            JsonParser.parseReader(new InputStreamReader(new FileInputStream(refFile), java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonArray()),
        comp.indexEntities(
            JsonParser.parseReader(new InputStreamReader(new FileInputStream(newFile), java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonArray()));
    }

    private static void displaySummary(List<Difference> diffs,
                                       Map<String, JsonObject> refMap,
                                       Map<String, JsonObject> newMap) {
        int totalRef = refMap.size();
        int totalNew = newMap.size();
        int isoCount = 0, added = 0, deleted = 0;

        for (String id : refMap.keySet()) {
            if (!newMap.containsKey(id)) deleted++;
            else if (diffs.stream()
                    .noneMatch(d -> d.getEntityId().equals(id)
                            && d.getType() == ChangeType.MODIFICATION)) {
                isoCount++;
            }
        }
        for (String id : newMap.keySet()) {
            if (!refMap.containsKey(id)) added++;
        }

        System.out.println("\n=== Summary ===");
        System.out.printf("Reference file: %d objects%n", totalRef);
        System.out.printf("New file: %d objects%n%n", totalNew);
        System.out.printf("Iso objects: %d%n", isoCount);
        System.out.printf("Objects added: %d%n", added);
        System.out.printf("Objects deleted: %d%n", deleted);
    }
}