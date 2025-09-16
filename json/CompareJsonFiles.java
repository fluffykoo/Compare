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

        Map<String, JsonObject> refMap = comp.indexEntities(
            JsonParser.parseReader(
                new InputStreamReader(new FileInputStream(refFile), java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonArray());

        Map<String, JsonObject> newMap = comp.indexEntities(
            JsonParser.parseReader(
                new InputStreamReader(new FileInputStream(newFile), java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonArray());

        // Appel de la méthode qui calcule le résumé comme dans le rapport
        ReportGenerator.SummaryStats stats = gen.calculateSummaryStats(diffs, refMap.keySet(), newMap.keySet());

        System.out.println("\n=== Summary ===");
        System.out.printf("Reference file: %d objects%n", stats.totalRef);
        System.out.printf("New file: %d objects%n", stats.totalNew);
        System.out.printf("Identical objects: %d%n", stats.identical);
        System.out.printf("Objects added: %d%n", stats.added);
        System.out.printf("Objects deleted: %d%n", stats.deleted);
    }
}
