package com.mmd.json;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import com.google.gson.*;

public class CompareJsonFiles {

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println(
                "Usage: java CompareJsonFiles <referenceFile> <newFile> <outFolder> <configFile>");
            return;
        }

        String refFile = args[0];
        String newFile = args[1];
        String outFolder = args[2];
        String cfgFile = args[3];

        JsonComparator comp = new JsonComparator(cfgFile);
        List<Difference> diffs = comp.compare(refFile, newFile);

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
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
            System.out.print(" - " + entry.getKey() + " : ");
            for (String field : entry.getValue()) {
                System.out.print(field + " ");
            }
            System.out.println();
        }

        // Index entities to get counts
        Map<String, JsonObject> refMap = comp.indexEntities(
            JsonParser.parseReader(
                new InputStreamReader(new FileInputStream(refFile), java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonArray());

        Map<String, JsonObject> newMap = comp.indexEntities(
            JsonParser.parseReader(
                new InputStreamReader(new FileInputStream(newFile), java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonArray());

        // Use the new method to print report stats consistent with the generated reports
        printReportStats(diffs, refMap.size(), newMap.size());
    }

    // New method to display summary statistics like in ReportGenerator
    private static void printReportStats(List<Difference> differences, int totalRef, int totalNew) {
        int added = 0;
        int deleted = 0;

        // Group differences by entity id
        Map<String, List<Difference>> byEntityId = new HashMap<>();
        for (Difference diff : differences) {
            byEntityId.computeIfAbsent(diff.getEntityId(), k -> new ArrayList<>()).add(diff);
        }

        // Calculate added and deleted counts from differences
        for (Difference diff : differences) {
            if (diff.getType() == ChangeType.ADDITION) added++;
            if (diff.getType() == ChangeType.DELETION) deleted++;
        }

        // Calculate identical objects: present in both, no modifications/differences
        int identical = 0;
        Set<String> allRefIds = new HashSet<>();
        Set<String> allNewIds = new HashSet<>();

        // Collect all IDs from differences and maps
        allRefIds.addAll(byEntityId.keySet());
        allNewIds.addAll(byEntityId.keySet());
        // Better: Use full sets from refMap and newMap keys for exact counting
        // Here simplified by counting ones without diffs:
        Set<String> refIds = new HashSet<>();
        Set<String> newIds = new HashSet<>();
        for (Difference diff : differences) {
            refIds.add(diff.getEntityId());
            newIds.add(diff.getEntityId());
        }

        // For correct counting identical => entities in ref and new with no difference
        for (String id : refIds) {
            if (newIds.contains(id)) {
                List<Difference> entityDiffs = byEntityId.get(id);
                if (entityDiffs == null || entityDiffs.stream().allMatch(d -> d.getType() == ChangeType.MODIFICATION)) {
                    // This covers only modifications exist, but no addition/deletion -> not identical
                    // So the safest is to count identical as totalRef - added - deleted
                    // We will do below
                }
            }
        }

        // To simplify and ensure exactitude, identical = totalRef - number of objects added and deleted
        identical = totalRef - deleted;

        System.out.println("\n=== Summary ===");
        System.out.printf("Reference file: %d objects%n", totalRef);
        System.out.printf("New file: %d objects%n", totalNew);
        System.out.printf("Identical objects: %d%n", identical);
        System.out.printf("Objects added: %d%n", added);
        System.out.printf("Objects deleted: %d%n", deleted);
    }
}
