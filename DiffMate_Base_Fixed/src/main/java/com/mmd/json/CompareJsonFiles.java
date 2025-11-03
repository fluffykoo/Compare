package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class CompareJsonFiles {

    public static void main(String[] args) throws Exception {
        if (args.length != 6 && args.length != 5) {
            System.out.println("Usage: java -jar DiffMate.jar JSON <refFile> <newFile> <rapportFolder> <configFile> <outputBaseName>");
            return;
        }
        // mode demandé: JSON
        int shift = args.length == 6 ? 1 : 0; // si tu gardes un wrapper qui passe "JSON"
        String refFile  = args[0 + shift];
        String newFile  = args[1 + shift];
        String outDir   = args[2 + shift];
        String cfgFile  = args[3 + shift];
        String baseName = args[4 + shift];

        JsonComparator comparator = new JsonComparator(cfgFile);
        List<Difference> diffs = comparator.compare(refFile, newFile);

        ReportGenerator gen = new ReportGenerator(outDir, baseName);
        gen.setComparedFiles(refFile, newFile);
        gen.generateTextReport(diffs);
        gen.generateCsvReport(diffs);
        gen.generateExcelReport(diffs);

        // résumé console
        displaySummary(diffs, indexEntities(refFile), indexEntities(newFile));
        System.out.println("All reports generated in folder: " + Paths.get(outDir).toAbsolutePath());
    }

    // ===== util pour résumé =====
    private static Map<String, JsonObject> indexEntities(String file) throws IOException {
        try (Reader rd = Files.newBufferedReader(Paths.get(file), StandardCharsets.UTF_8)) {
            JsonElement el = JsonParser.parseReader(rd);
            Map<String, JsonObject> map = new LinkedHashMap<>();
            if (el.isJsonArray()) {
                for (JsonElement e : el.getAsJsonArray()) {
                    if (e.isJsonObject()) {
                        // best-effort id -> mmdCode dans commonData ou "id" direct
                        JsonObject o = e.getAsJsonObject();
                        String id = null;
                        if (o.has("commonData") && o.get("commonData").isJsonObject()) {
                            JsonObject cd = o.getAsJsonObject("commonData");
                            if (cd.has("mmdCode") && cd.get("mmdCode").isJsonPrimitive())
                                id = cd.get("mmdCode").getAsString();
                        }
                        if (id == null && o.has("id") && o.get("id").isJsonPrimitive())
                            id = o.get("id").getAsString();
                        if (id == null) id = o.toString(); // fallback
                        map.put(id, o);
                    }
                }
            } else if (el.isJsonObject()) {
                map.put("ROOT", el.getAsJsonObject());
            }
            return map;
        }
    }

    private static void displaySummary(List<Difference> diffs,
                                       Map<String, JsonObject> refMap,
                                       Map<String, JsonObject> newMap) {
        int totalRef = refMap.size();
        int totalNew = newMap.size();

        int iso = 0, added = 0, deleted = 0, modified = 0;

        Set<String> ids = new LinkedHashSet<>();
        ids.addAll(refMap.keySet());
        ids.addAll(newMap.keySet());

        for (String id : ids) {
            boolean inRef = refMap.containsKey(id);
            boolean inNew = newMap.containsKey(id);

            if (inRef && !inNew) { deleted++; continue; }
            if (!inRef && inNew) { added++; continue; }

            boolean isMod = diffs.stream().anyMatch(d -> d.getEntityId().equals(id) &&
                                                         d.getType() == ChangeType.MODIFICATION);
            if (isMod) modified++; else iso++;
        }

        System.out.println("\n=== Summary ===");
        System.out.printf("Reference file: %d objects%n", totalRef);
        System.out.printf("New file: %d objects%n", totalNew);
        System.out.printf("Iso objects: %d%n", iso);
        System.out.printf("Objects modified: %d%n", modified);
        System.out.printf("Objects added: %d%n", added);
        System.out.printf("Objects deleted: %d%n", deleted);
    }
}
