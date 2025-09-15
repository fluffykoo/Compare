package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ConfigurationManager {
    private String primaryKey;
    private String fallbackKey;
    private JsonObject subSectionKeys;
    private JsonObject ignoredFields;
    private String reportFileName;

    public ConfigurationManager(String configFile) throws IOException {
        // Résolution du chemin : absolu = on prend tel quel, sinon on cherche dans le dossier du script Java exécuté
        File configF = new File(configFile);
        if (!configF.isAbsolute()) {
            try {
                File scriptDir = new File(ConfigurationManager.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
                File candidate = new File(scriptDir, configFile);
                if (candidate.exists()) {
                    configF = candidate;
                }
            } catch (Exception e) {
                // Fallback : on ne change rien
            }
        }
        if (!configF.exists()) {
            throw new FileNotFoundException("Configuration file not found: " + configF.getAbsolutePath());
        }
        try (FileReader reader = new FileReader(configF)) {
            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
            this.primaryKey = config.get("primary_key").getAsString();
            this.fallbackKey = config.has("fallback_key")
                    ? config.get("fallback_key").getAsString()
                    : null;
            this.subSectionKeys = config.getAsJsonObject("subSectionKeys");
            this.ignoredFields = config.has("ignored_fields") && config.get("ignored_fields").isJsonObject()
                    ? config.getAsJsonObject("ignored_fields")
                    : new JsonObject();
            this.reportFileName = config.get("reportFileName").getAsString();
        } catch (Exception e) {
            throw new IOException("Error reading configuration file: " + configF.getAbsolutePath() + "\n" + e.getMessage(), e);
        }
        this.primaryKey = config.get("primary_key").getAsString();
        this.fallbackKey = config.has("fallback_key")
                ? config.get("fallback_key").getAsString()
                : null;
        this.subSectionKeys = config.getAsJsonObject("subSectionKeys");
        this.ignoredFields = config.has("ignored_fields") && config.get("ignored_fields").isJsonObject()
                ? config.getAsJsonObject("ignored_fields")
                : new JsonObject();
        this.reportFileName = config.get("reportFileName").getAsString();
    }

    public String getPrimaryKey() { return primaryKey; }
    public String getFallbackKey() { return fallbackKey; }
    public JsonObject getSubSectionKeys() { return subSectionKeys; }
    public String getReportFileName() { return reportFileName; }

    public List<String> getSubSectionKeys(String sectionName) {
        JsonElement keyElement = subSectionKeys.get(sectionName);

        if (keyElement == null || keyElement.isJsonNull()) {
            return new ArrayList<>();
        }

        List<String> keys = new ArrayList<>();

        if (keyElement.isJsonPrimitive()) {
            // clé simple
            keys.add(keyElement.getAsString());
        } else if (keyElement.isJsonArray()) {
            for (JsonElement elem : keyElement.getAsJsonArray()) {
                keys.add(elem.getAsString());
            }
        }

        return keys;
    }

    public List<String> getIgnoredFields(String section) {
        if (ignoredFields.has(section)) {
            JsonArray array = ignoredFields.getAsJsonArray(section);
            List<String> fields = new ArrayList<>();
            for (JsonElement elem : array) {
                fields.add(elem.getAsString());
            }
            return fields;
        }
        return new ArrayList<>();
    }

    public java.util.Map<String, List<String>> getIgnoredFields() {
        java.util.Map<String, List<String>> result = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, JsonElement> entry : ignoredFields.entrySet()) {
            List<String> fields = new ArrayList<>();
            for (JsonElement elem : entry.getValue().getAsJsonArray()) {
                fields.add(elem.getAsString());
            }
            result.put(entry.getKey(), fields);
        }
        return result;
    }


}