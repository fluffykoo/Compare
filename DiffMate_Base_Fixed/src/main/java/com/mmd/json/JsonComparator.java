package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class JsonComparator {

    private final ConfigurationManager config;

    public JsonComparator(String configPath) throws IOException {
        this.config = new ConfigurationManager(configPath);
    }

    public List<Difference> compare(String refFile, String newFile) throws IOException {
        try (Reader refReader = Files.newBufferedReader(Paths.get(refFile));
             Reader newReader = Files.newBufferedReader(Paths.get(newFile))) {

            JsonElement refElement = JsonParser.parseReader(refReader);
            JsonElement newElement = JsonParser.parseReader(newReader);

            List<Difference> diffs = new ArrayList<>();

            if (refElement.isJsonArray() && newElement.isJsonArray()) {
                compareJsonArrays(refElement.getAsJsonArray(), newElement.getAsJsonArray(), diffs);
            } else if (refElement.isJsonObject() && newElement.isJsonObject()) {
                compareEntities("ROOT", refElement.getAsJsonObject(), newElement.getAsJsonObject(), diffs);
            }

            return diffs;
        }
    }

    // === Comparaison des tableaux d'entités ===
    private void compareJsonArrays(JsonArray refArray, JsonArray newArray, List<Difference> diffs) {
        List<String> primaryKeys = config.getPrimaryKeys();
        String fallbackKey = config.getFallbackKey();

        Map<String, JsonObject> refMap = indexEntities(refArray, primaryKeys, fallbackKey);
        Map<String, JsonObject> newMap = indexEntities(newArray, primaryKeys, fallbackKey);

        Set<String> allIds = new HashSet<>();
        allIds.addAll(refMap.keySet());
        allIds.addAll(newMap.keySet());

        for (String id : allIds) {
            JsonObject refObj = refMap.get(id);
            JsonObject newObj = newMap.get(id);

            if (refObj == null && newObj != null) {
                diffs.add(new Difference(id, ChangeType.ADDITION, "", "", null, newObj.toString()));
            } else if (refObj != null && newObj == null) {
                diffs.add(new Difference(id, ChangeType.DELETION, "", "", refObj.toString(), null));
            } else if (refObj != null && newObj != null) {
                compareEntities(id, refObj, newObj, diffs);
            }
        }
    }

    private Map<String, JsonObject> indexEntities(JsonArray array, List<String> primaryKeys, String fallbackKey) {
        Map<String, JsonObject> map = new LinkedHashMap<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject obj = element.getAsJsonObject();
            String id = extractCompositeId(obj, primaryKeys, fallbackKey);
            if (id != null) map.put(id, obj);
        }
        return map;
    }

    private String extractCompositeId(JsonObject obj, List<String> primaryKeys, String fallbackKey) {
        StringBuilder sb = new StringBuilder();
        boolean found = false;

        for (String key : primaryKeys) {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                if (found) sb.append('|');
                sb.append(obj.get(key).getAsString());
                found = true;
            }
        }

        if (!found && fallbackKey != null && obj.has(fallbackKey) && obj.get(fallbackKey).isJsonPrimitive()) {
            sb.append(obj.get(fallbackKey).getAsString());
            found = true;
        }

        return found ? sb.toString() : null;
    }

    // === Comparaison d'une entité complète ===
    private void compareEntities(String entityId, JsonObject refObj, JsonObject newObj, List<Difference> diffs) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(refObj.keySet());
        allKeys.addAll(newObj.keySet());

        for (String key : allKeys) {
            List<String> ignored = config.getIgnoredFields("").getOrDefault("", Collections.emptyList());
            if (ignored.contains(key)) continue;

            JsonElement refVal = refObj.has(key) ? refObj.get(key) : null;
            JsonElement newVal = newObj.has(key) ? newObj.get(key) : null;

            if (refVal == null && newVal != null) {
                handleAddition(entityId, "", key, newVal, diffs);
            } else if (refVal != null && newVal == null) {
                handleDeletion(entityId, "", key, refVal, diffs);
            } else if (refVal != null && newVal != null) {
                handleModification(entityId, "", key, refVal, newVal, diffs);
            }
        }
    }

    // === Gestion des changements ===
    private void handleAddition(String entityId, String section, String key, JsonElement newVal, List<Difference> diffs) {
        if (newVal.isJsonObject()) {
            compareNestedObjects(entityId, key, new JsonObject(), newVal.getAsJsonObject(), diffs);
        } else if (newVal.isJsonArray()) {
            compareJsonArraysByKey(entityId, key, config.getSubSectionKeys().get(key),
                    new JsonArray(), newVal.getAsJsonArray(), diffs);
        } else {
            diffs.add(new Difference(entityId, ChangeType.ADDITION, section, key, null, newVal.getAsString()));
        }
    }

    private void handleDeletion(String entityId, String section, String key, JsonElement refVal, List<Difference> diffs) {
        if (refVal.isJsonObject()) {
            compareNestedObjects(entityId, key, refVal.getAsJsonObject(), new JsonObject(), diffs);
        } else if (refVal.isJsonArray()) {
            compareJsonArraysByKey(entityId, key, config.getSubSectionKeys().get(key),
                    refVal.getAsJsonArray(), new JsonArray(), diffs);
        } else {
            diffs.add(new Difference(entityId, ChangeType.DELETION, section, key, refVal.getAsString(), null));
        }
    }

    private void handleModification(String entityId, String section, String key,
                                    JsonElement refVal, JsonElement newVal, List<Difference> diffs) {
        if (refVal.isJsonObject() && newVal.isJsonObject()) {
            compareNestedObjects(entityId, key, refVal.getAsJsonObject(), newVal.getAsJsonObject(), diffs);
        } else if (refVal.isJsonArray() && newVal.isJsonArray()) {
            compareJsonArraysByKey(entityId, key, config.getSubSectionKeys().get(key),
                    refVal.getAsJsonArray(), newVal.getAsJsonArray(), diffs);
        } else if (refVal.isJsonPrimitive() && newVal.isJsonPrimitive()) {
            String s1 = refVal.getAsString();
            String s2 = newVal.getAsString();
            if (!Objects.equals(s1, s2)) {
                diffs.add(new Difference(entityId, ChangeType.MODIFICATION, section, key, s1, s2));
            }
        } else if (!refVal.equals(newVal)) {
            diffs.add(new Difference(entityId, ChangeType.MODIFICATION, section, key, refVal.toString(), newVal.toString()));
        }
    }

    // === Comparaison récursive d'objets imbriqués ===
    private void compareNestedObjects(String entityId, String section,
                                      JsonObject ref, JsonObject nov,
                                      List<Difference> diffs) {
        Set<String> keys = new HashSet<>();
        keys.addAll(ref.keySet());
        keys.addAll(nov.keySet());

        for (String k : keys) {
            JsonElement v1 = ref.has(k) ? ref.get(k) : null;
            JsonElement v2 = nov.has(k) ? nov.get(k) : null;
            String path = section.isEmpty() ? k : section + "." + k;

            if (v1 == null && v2 != null) {
                handleAddition(entityId, section, path, v2, diffs);
            } else if (v1 != null && v2 == null) {
                handleDeletion(entityId, section, path, v1, diffs);
            } else if (v1 != null && v2 != null) {
                handleModification(entityId, section, path, v1, v2, diffs);
            }
        }
    }

    // === Comparaison de tableaux d’objets ===
    private void compareJsonArraysByKey(String entityId, String section,
                                        List<String> subKeys,
                                        JsonArray refArray,
                                        JsonArray newArray,
                                        List<Difference> diffs) {
        if (subKeys == null || subKeys.isEmpty()) {
            if (!refArray.toString().equals(newArray.toString())) {
                diffs.add(new Difference(entityId, ChangeType.MODIFICATION, section, "array",
                        refArray.toString(), newArray.toString()));
            }
            return;
        }

        Map<String, JsonObject> refMap = mapArrayByKeys(refArray, subKeys);
        Map<String, JsonObject> newMap = mapArrayByKeys(newArray, subKeys);

        Set<String> allIds = new HashSet<>();
        allIds.addAll(refMap.keySet());
        allIds.addAll(newMap.keySet());

        for (String id : allIds) {
            JsonObject refObj = refMap.get(id);
            JsonObject newObj = newMap.get(id);

            if (refObj == null && newObj != null) {
                diffs.add(new Difference(entityId, ChangeType.ADDITION, section, id, null, newObj.toString()));
            } else if (refObj != null && newObj == null) {
                diffs.add(new Difference(entityId, ChangeType.DELETION, section, id, refObj.toString(), null));
            } else if (refObj != null && newObj != null) {
                compareNestedObjects(entityId, section + "[" + id + "]", refObj, newObj, diffs);
            }
        }
    }

    private Map<String, JsonObject> mapArrayByKeys(JsonArray array, List<String> keys) {
        Map<String, JsonObject> map = new LinkedHashMap<>();
        for (JsonElement e : array) {
            if (!e.isJsonObject()) continue;
            JsonObject obj = e.getAsJsonObject();
            String id = generateCompositeKey(obj, keys);
            if (id != null) map.put(id, obj);
        }
        return map;
    }

    private String generateCompositeKey(JsonObject obj, List<String> keys) {
        StringBuilder sb = new StringBuilder();
        boolean ok = false;
        for (String k : keys) {
            if (obj.has(k) && obj.get(k).isJsonPrimitive()) {
                if (ok) sb.append('|');
                sb.append(obj.get(k).getAsString());
                ok = true;
            }
        }
        return ok ? sb.toString() : null;
    }
}
