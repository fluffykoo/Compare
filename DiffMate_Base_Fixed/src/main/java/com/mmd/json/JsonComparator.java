package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class JsonComparator {

    private final ConfigurationManager config;

    public JsonComparator(String configFile) throws IOException {
        this.config = new ConfigurationManager(configFile);
        System.out.println("=== Ignored fields loaded from config ===");
        config.getIgnoredFields().forEach((section, fields) ->
            System.out.println("Section: " + section + " → " + fields)
        );
        System.out.println("=========================================");
    }

    public ConfigurationManager getConfig() { return config; }

    // === Entrée principale ===
    public List<Difference> compare(String refFile, String newFile) throws IOException {
        try (Reader refR = Files.newBufferedReader(new File(refFile).toPath(), StandardCharsets.UTF_8);
             Reader newR = Files.newBufferedReader(new File(newFile).toPath(), StandardCharsets.UTF_8)) {

            JsonElement refEl = JsonParser.parseReader(refR);
            JsonElement newEl = JsonParser.parseReader(newR);

            List<Difference> diffs = new ArrayList<>();

            if (refEl.isJsonArray() && newEl.isJsonArray()) {
                compareJsonArrays(refEl.getAsJsonArray(), newEl.getAsJsonArray(), diffs);
            } else if (refEl.isJsonObject() && newEl.isJsonObject()) {
                compareEntities("ROOT", "", refEl.getAsJsonObject(), newEl.getAsJsonObject(), diffs);
            } else {
                diffs.add(new Difference("ROOT", ChangeType.MODIFICATION, "", "(root)",
                        stringify(refEl), stringify(newEl)));
            }
            return diffs;
        }
    }

    // === Indexation d’un tableau par ID ===
    Map<String, JsonObject> indexEntities(JsonArray arr) {
        Map<String, JsonObject> map = new LinkedHashMap<>();
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) continue;
            JsonObject obj = e.getAsJsonObject();
            String id = extractEntityId(obj);
            if (id != null) map.put(id, obj);
        }
        return map;
    }

    private void compareJsonArrays(JsonArray refArr, JsonArray newArr, List<Difference> diffs) {
        Map<String, JsonObject> refMap = indexEntities(refArr);
        Map<String, JsonObject> newMap = indexEntities(newArr);

        for (String id : refMap.keySet()) {
            if (!newMap.containsKey(id)) {
                diffs.add(new Difference(id, ChangeType.DELETION, "", "", null, null));
            } else {
                compareEntities(id, "", refMap.get(id), newMap.get(id), diffs);
            }
        }
        for (String id : newMap.keySet()) {
            if (!refMap.containsKey(id)) {
                diffs.add(new Difference(id, ChangeType.ADDITION, "", "", null, null));
            }
        }
    }

    // === Comparaison détaillée d’une entité ===
    private void compareEntities(String entityId, String section,
                                 JsonObject refObj, JsonObject newObj, List<Difference> diffs) {

        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(refObj.keySet());
        allKeys.addAll(newObj.keySet());

        Map<String, List<String>> ignored = config.getIgnoredFields();
        List<String> ignoredInSection = ignored.getOrDefault(section, Collections.emptyList());

        for (String key : allKeys) {
            boolean skip = false;

            if (ignoredInSection.contains(key)) {
                skip = true;
            } else {
                String qualifiedKey = section.isEmpty() ? key : section + "." + key;
                for (Map.Entry<String, List<String>> entry : ignored.entrySet()) {
                    if (entry.getValue().contains(qualifiedKey) || entry.getValue().contains(key)) {
                        skip = true;
                        break;
                    }
                }
            }

            if (skip) {
                diffs.add(new Difference(entityId, ChangeType.IGNORED, section, key, "(ignored)", "(ignored)"));
                continue;
            }

            JsonElement v1 = refObj.has(key) ? refObj.get(key) : null;
            JsonElement v2 = newObj.has(key) ? newObj.get(key) : null;

            if (v1 == null && v2 != null) {
                handleAddition(entityId, section, key, v2, diffs);
                continue;
            }
            if (v1 != null && v2 == null) {
                diffs.add(new Difference(entityId, ChangeType.DELETION, section, key, stringify(v1), null));
                continue;
            }
            if (v1 == null) continue;

            if (v1.isJsonObject() && v2.isJsonObject()) {
                String nextSection = section.isEmpty() ? key : section + "." + key;
                compareNestedObjects(entityId, nextSection, v1.getAsJsonObject(), v2.getAsJsonObject(), diffs);
            } else if (v1.isJsonArray() && v2.isJsonArray()) {
                String nextSection = section.isEmpty() ? key : section + "." + key;
                compareJsonArraysByKey(entityId, nextSection, v1.getAsJsonArray(), v2.getAsJsonArray(), diffs);
            } else {
                String s1 = stringify(v1);
                String s2 = stringify(v2);
                if (!Objects.equals(s1, s2)) {
                    diffs.add(new Difference(entityId, ChangeType.MODIFICATION, section, key, s1, s2));
                }
            }
        }
    }

    private void compareNestedObjects(String entityId, String section,
                                      JsonObject refObj, JsonObject newObj, List<Difference> diffs) {
        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(refObj.keySet());
        allKeys.addAll(newObj.keySet());

        Map<String, List<String>> ignored = config.getIgnoredFields();
        List<String> ignoredInSection = ignored.getOrDefault(section, Collections.emptyList());

        for (String key : allKeys) {
            boolean skip = false;

            if (ignoredInSection.contains(key)) {
                skip = true;
            } else {
                String qualifiedKey = section.isEmpty() ? key : section + "." + key;
                for (Map.Entry<String, List<String>> entry : ignored.entrySet()) {
                    if (entry.getValue().contains(qualifiedKey) || entry.getValue().contains(key)) {
                        skip = true;
                        break;
                    }
                }
            }

            if (skip) {
                diffs.add(new Difference(entityId, ChangeType.IGNORED, section, key, "(ignored)", "(ignored)"));
                continue;
            }

            JsonElement v1 = refObj.has(key) ? refObj.get(key) : null;
            JsonElement v2 = newObj.has(key) ? newObj.get(key) : null;

            if (v1 == null && v2 != null) {
                handleAddition(entityId, section, key, v2, diffs);
            } else if (v1 != null && v2 == null) {
                diffs.add(new Difference(entityId, ChangeType.DELETION, section, key, stringify(v1), null));
            } else if (v1 != null) {
                if (v1.isJsonObject() && v2.isJsonObject()) {
                    compareNestedObjects(entityId, section.isEmpty() ? key : section + "." + key,
                            v1.getAsJsonObject(), v2.getAsJsonObject(), diffs);
                } else if (v1.isJsonArray() && v2.isJsonArray()) {
                    compareJsonArraysByKey(entityId, section.isEmpty() ? key : section + "." + key,
                            v1.getAsJsonArray(), v2.getAsJsonArray(), diffs);
                } else {
                    String s1 = stringify(v1);
                    String s2 = stringify(v2);
                    if (!Objects.equals(s1, s2)) {
                        diffs.add(new Difference(entityId, ChangeType.MODIFICATION, section, key, s1, s2));
                    }
                }
            }
        }
    }

    // === Comparaison des tableaux (corrigée) ===
    private void compareJsonArraysByKey(String entityId, String section,
                                        JsonArray refArr, JsonArray newArr, List<Difference> diffs) {

        Map<String, List<String>> ignored = config.getIgnoredFields();
        List<String> ignoredInSection = ignored.getOrDefault(section, Collections.emptyList());

        // Cas 1 : tableaux de primitives
        if (isPrimitiveArray(refArr) && isPrimitiveArray(newArr)) {
            Set<String> a = toStringSet(refArr);
            Set<String> b = toStringSet(newArr);

            for (String x : diffOnly(a, b))
                if (!ignoredInSection.contains(x))
                    diffs.add(new Difference(entityId, ChangeType.DELETION, section, "(item)", x, null));

            for (String x : diffOnly(b, a))
                if (!ignoredInSection.contains(x))
                    diffs.add(new Difference(entityId, ChangeType.ADDITION, section, "(item)", null, x));
            return;
        }

        // Cas 2 : tableaux d’objets
        List<String> keys = config.getSubSectionKeys(section);
        if (keys.isEmpty()) {
            Set<String> A = toStringSet(refArr);
            Set<String> B = toStringSet(newArr);
            for (String x : diffOnly(A, B))
                diffs.add(new Difference(entityId, ChangeType.DELETION, section, "(object)", x, null));
            for (String x : diffOnly(B, A))
                diffs.add(new Difference(entityId, ChangeType.ADDITION, section, "(object)", null, x));
            return;
        }

        Map<String, JsonObject> A = indexByCompositeKey(refArr, keys);
        Map<String, JsonObject> B = indexByCompositeKey(newArr, keys);

        for (String k : A.keySet()) {
            boolean skip = ignoredInSection.contains(k);
            if (!skip) {
                String qualifiedKey = section.isEmpty() ? k : section + "." + k;
                for (Map.Entry<String, List<String>> entry : ignored.entrySet()) {
                    if (entry.getValue().contains(qualifiedKey) || entry.getValue().contains(k)) {
                        skip = true;
                        break;
                    }
                }
            }
            if (skip) {
                diffs.add(new Difference(entityId, ChangeType.IGNORED, section, k, "(ignored)", "(ignored)"));
                continue;
            }
            if (!B.containsKey(k)) {
                diffs.add(new Difference(entityId, ChangeType.DELETION, section, k, stringify(A.get(k)), null));
            } else {
                compareNestedObjects(entityId, section, A.get(k), B.get(k), diffs);
            }
        }

        for (String k : B.keySet()) {
            if (!A.containsKey(k)) {
                boolean skip = ignoredInSection.contains(k);
                if (!skip) {
                    String qualifiedKey = section.isEmpty() ? k : section + "." + k;
                    for (Map.Entry<String, List<String>> entry : ignored.entrySet()) {
                        if (entry.getValue().contains(qualifiedKey) || entry.getValue().contains(k)) {
                            skip = true;
                            break;
                        }
                    }
                }
                if (skip) {
                    diffs.add(new Difference(entityId, ChangeType.IGNORED, section, k, "(ignored)", "(ignored)"));
                    continue;
                }
                diffs.add(new Difference(entityId, ChangeType.ADDITION, section, k, null, stringify(B.get(k))));
            }
        }
    }

    // === Utilitaires ===
    private static boolean isPrimitiveArray(JsonArray arr) {
        for (JsonElement e : arr) if (!e.isJsonPrimitive()) return false;
        return true;
    }

    private static Set<String> toStringSet(JsonArray arr) {
        Set<String> s = new LinkedHashSet<>();
        for (JsonElement e : arr) s.add(stringify(e));
        return s;
    }

    private static Set<String> diffOnly(Set<String> a, Set<String> b) {
        Set<String> out = new LinkedHashSet<>(a);
        out.removeAll(b);
        return out;
    }

    private Map<String, JsonObject> indexByCompositeKey(JsonArray arr, List<String> keys) {
        Map<String, JsonObject> map = new LinkedHashMap<>();
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            String k = compositeValue(o, keys);
            if (k != null) map.put(k, o);
        }
        return map;
    }

    private String compositeValue(JsonObject obj, List<String> keys) {
        List<String> parts = new ArrayList<>();
        for (String k : keys) {
            JsonElement v = obj.get(k);
            if (v == null) return null;
            parts.add(stringify(v));
        }
        return String.join("|", parts);
    }

    private void handleAddition(String entityId, String section, String key,
                                JsonElement newVal, List<Difference> diffs) {
        diffs.add(new Difference(entityId, ChangeType.ADDITION, section, key, null, stringify(newVal)));
    }

    private static String stringify(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonPrimitive()) return e.getAsJsonPrimitive().getAsString();
        return e.toString();
    }

    private String extractEntityId(JsonObject obj) {
        String id = lookupByKeyName(obj, config.getPrimaryKey(), config.getFallbackKey());
        if (id != null) return id;
        return deepFindValueByKey(obj, config.getFallbackKey());
    }

    private String lookupByKeyName(JsonObject obj, String primary, String innerKey) {
        if (primary == null) return null;
        if (!obj.has(primary)) return null;

        JsonElement el = obj.get(primary);
        if (el.isJsonPrimitive()) return el.getAsString();
        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            if (innerKey != null && o.has(innerKey) && o.get(innerKey).isJsonPrimitive())
                return o.get(innerKey).getAsString();
        }
        return null;
    }

    private String deepFindValueByKey(JsonObject obj, String keyName) {
        if (keyName == null) return null;
        Deque<JsonElement> stack = new ArrayDeque<>();
        stack.push(obj);
        while (!stack.isEmpty()) {
            JsonElement cur = stack.pop();
            if (cur.isJsonObject()) {
                JsonObject o = cur.getAsJsonObject();
                if (o.has(keyName) && o.get(keyName).isJsonPrimitive())
                    return o.get(keyName).getAsString();
                for (JsonElement v : o.entrySet().stream().map(Map.Entry::getValue).toList())
                    stack.push(v);
            } else if (cur.isJsonArray()) {
                for (JsonElement v : cur.getAsJsonArray()) stack.push(v);
            }
        }
        return null;
    }
}
