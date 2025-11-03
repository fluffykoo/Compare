package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class ConfigurationManager {

    private final List<String> primaryKeys;
    private final String fallbackKey;
    private final JsonObject subSectionKeys;
    private final JsonObject ignoredFields;

    public ConfigurationManager(String configFile) throws IOException {
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject cfg = JsonParser.parseReader(reader).getAsJsonObject();
            // primary_key can be string or array
            List<String> pks = new ArrayList<>();
            if (cfg.has("primary_key")) {
                JsonElement pk = cfg.get("primary_key");
                if (pk.isJsonArray()) {
                    for (JsonElement e : pk.getAsJsonArray()) pks.add(e.getAsString());
                } else if (pk.isJsonPrimitive()) {
                    pks.add(pk.getAsString());
                }
            }
            this.primaryKeys = Collections.unmodifiableList(pks);
            this.fallbackKey = cfg.has("fallback_key") ? cfg.get("fallback_key").getAsString() : null;
            this.subSectionKeys = cfg.has("subSectionKeys") && cfg.get("subSectionKeys").isJsonObject()
                                    ? cfg.getAsJsonObject("subSectionKeys") : new JsonObject();
            this.ignoredFields = cfg.has("ignored_fields") && cfg.get("ignored_fields").isJsonObject()
                                    ? cfg.getAsJsonObject("ignored_fields") : new JsonObject();
        } catch (Exception e) {
            throw new IOException("Error reading configuration: " + configFile, e);
        }
    }

    public List<String> getPrimaryKeys() { return primaryKeys; }
    public String getFallbackKey() { return fallbackKey; }

    public List<String> getSubSectionKeys(String section) {
        if (subSectionKeys.has(section)) {
            JsonElement val = subSectionKeys.get(section);
            List<String> keys = new ArrayList<>();
            if (val.isJsonPrimitive()) {
                keys.add(val.getAsString());
            } else if (val.isJsonArray()) {
                for (JsonElement e : val.getAsJsonArray()) keys.add(e.getAsString());
            }
            return keys;
        }
        return Collections.emptyList();
    }

    public Map<String, List<String>> getIgnoredFields() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : ignoredFields.entrySet()) {
            List<String> list = new ArrayList<>();
            for (JsonElement v : e.getValue().getAsJsonArray()) list.add(v.getAsString());
            map.put(e.getKey(), list);
        }
        return map;
    }
}