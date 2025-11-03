package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class JsonComparator {

    private final ConfigurationManager config;

    public JsonComparator(String configFile) throws IOException {
        this.config = new ConfigurationManager(configFile);
    }

    public ConfigurationManager getConfig() { return this.config; }

    public List<Difference> compare(String referenceFile, String newFile) throws IOException {
        JsonArray refArray = JsonParser.parseReader(new FileReader(referenceFile)).getAsJsonArray();
        JsonArray newArray = JsonParser.parseReader(new FileReader(newFile)).getAsJsonArray();

        Map<String, JsonObject> refMap = indexEntities(refArray);
        Map<String, JsonObject> newMap = indexEntities(newArray);

        List<Difference> differences = new ArrayList<>();

        // deletions + modifications
        for (String entityId : refMap.keySet()) {
            if (!newMap.containsKey(entityId)) {
                differences.add(new Difference(entityId, ChangeType.DELETION, "", "", null, null));
            } else {
                compareEntities(entityId, refMap.get(entityId), newMap.get(entityId), differences);
            }
        }
        // additions
        for (String entityId : newMap.keySet()) {
            if (!refMap.containsKey(entityId)) {
                differences.add(new Difference(entityId, ChangeType.ADDITION, "", "", null, null));
            }
        }
        return differences;
    }

    public Map<String, JsonObject> indexEntities(JsonArray array) {
        Map<String, JsonObject> map = new LinkedHashMap<>();
        for (JsonElement el : array) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            String id = extractCompositeId(obj, config.getPrimaryKeys(), config.getFallbackKey());
            if (id != null) {
                map.put(id, obj);
            }
        }
        return map;
    }

    private String extractCompositeId(JsonObject obj, List<String> primaryKeys, String fallbackKey) {
        if (primaryKeys != null && !primaryKeys.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean found = false;
            for (String key : primaryKeys) {
                if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
                String v = pickScalar(obj.get(key));
                if (v == null) return null;
                if (found) sb.append("|");
                sb.append(v);
                found = true;
            }
            if (found) return sb.toString();
        }
        // fallback key if defined
        if (fallbackKey != null && obj.has(fallbackKey)) {
            String v = pickScalar(obj.get(fallbackKey));
            if (v != null) return v;
        }
        return null;
    }

    private String pickScalar(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonPrimitive()) return e.getAsString();
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            for (String k : new String[] {"id","code","value","name","identifier"}) {
                if (o.has(k) && o.get(k).isJsonPrimitive()) return o.get(k).getAsString();
            }
            // single primitive value in object
            for (Map.Entry<String, JsonElement> en : o.entrySet()) {
                if (en.getValue().isJsonPrimitive()) return en.getValue().getAsString();
            }
            return null; // do not serialize whole object
        }
        if (e.isJsonArray()) {
            List<String> vals = new ArrayList<>();
            for (JsonElement it : e.getAsJsonArray()) {
                String s = pickScalar(it);
                if (s != null) vals.add(s);
            }
            if (!vals.isEmpty()) return String.join("|", vals);
            return null;
        }
        return null;
    }

    private void compareEntities(String entityId, JsonObject refObj, JsonObject newObj, List<Difference> differences) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(refObj.keySet());
        allKeys.addAll(newObj.keySet());

        for (String key : allKeys) {
            if (config.getIgnoredFields().getOrDefault("", Collections.emptyList()).contains(key)) continue;

            JsonElement v1 = refObj.get(key);
            JsonElement v2 = newObj.get(key);

            if (v1 == null && v2 != null) {
                differences.add(new Difference(entityId, ChangeType.ADDITION, "", key, null, pickForReport(v2)));
            } else if (v1 != null && v2 == null) {
                differences.add(new Difference(entityId, ChangeType.DELETION, "", key, pickForReport(v1), null));
            } else if (v1 != null && v2 != null && !equalsJson(v1, v2)) {
                differences.add(new Difference(entityId, ChangeType.MODIFICATION, "", key, pickForReport(v1), pickForReport(v2)));
            }
        }
    }

    private boolean equalsJson(JsonElement a, JsonElement b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private String pickForReport(JsonElement e) {
        String v = pickScalar(e);
        return v != null ? v : (e == null || e.isJsonNull() ? null : e.toString());
    }
}