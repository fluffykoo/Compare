package com.mmd.json;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import com.google.gson.*;

public class CompareJsonFiles {

    public static void main(String[] args) throws Exception {
        if (args.length < 4 || args.length > 5) {
            System.out.println(
                "Usage: java CompareJsonFiles <refFile> <newFile> <outputFolder> <configFile> [reportFileName]"
            );
            return;
        }

        String refFile = args[0];
        String newFile = args[1];
        String outFolder = args[2];
        String cfgFile = args[3];
        String reportFileName = args.length == 5 ? args[4] : "Report";

        JsonComparator comp = new JsonComparator(cfgFile);
        List<Difference> diffs = comp.compare(refFile, newFile);

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ReportGenerator gen = new ReportGenerator(outFolder, ts, reportFileName);
        gen.setComparedFiles(refFile, newFile);
        gen.generateTextReport(diffs);
        gen.generateCsvReport(diffs);
        gen.generateExcelReport(diffs);

        System.out.println("\n=== Ignored fields from config ===");
        comp.getConfig().getIgnoredFields().forEach((section, fields) ->
            System.out.println("Section: " + section + " → " + fields)
        );
        System.out.println("==================================");

        displaySummary(
            diffs,
            comp.indexEntities(
                JsonParser.parseReader(new FileReader(refFile)).getAsJsonArray()
            ),
            comp.indexEntities(
                JsonParser.parseReader(new FileReader(newFile)).getAsJsonArray()
            )
        );
    }

    private static void displaySummary(List<Difference> diffs,
                                       Map<String, JsonObject> refMap,
                                       Map<String, JsonObject> newMap) {
        int totalRef = refMap.size();
        int totalNew = newMap.size();
        int isoCount = 0, added = 0, deleted = 0, modified = 0;

        for (String id : refMap.keySet()) {
            if (!newMap.containsKey(id)) {
                deleted++;
            } else {
                boolean hasRealModif = diffs.stream()
                    .anyMatch(d -> d.getEntityId().equals(id)
                            && d.getType() == ChangeType.MODIFICATION);
                boolean hasOnlyIgnored = diffs.stream()
                    .anyMatch(d -> d.getEntityId().equals(id)
                            && d.getType() == ChangeType.IGNORED);

                if (hasRealModif)
                    modified++;
                else if (hasOnlyIgnored || !hasRealModif)
                    isoCount++;
            }
        }

        for (String id : newMap.keySet()) {
            if (!refMap.containsKey(id)) added++;
        }

        System.out.println("\n=== Summary ===");
        System.out.printf("Reference file: %d objects%n", totalRef);
        System.out.printf("New file: %d objects%n", totalNew);
        System.out.printf("Iso objects: %d%n", isoCount);
        System.out.printf("Objects modified: %d%n", modified);
        System.out.printf("Objects added: %d%n", added);
        System.out.printf("Objects deleted: %d%n", deleted);
    }
}
