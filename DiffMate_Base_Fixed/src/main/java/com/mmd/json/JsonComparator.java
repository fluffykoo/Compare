package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Compare deux fichiers JSON.
 * - Top-level Array => chaque élément est une entité.
 * - Top-level Object => entité unique "ROOT".
 * ID d’entité = valeur trouvée en priorité via primary_key,
 * sinon via fallback_key, en recherchant au besoin en profondeur.
 */
public class JsonComparator {

    private final ConfigurationManager config;

    public JsonComparator(String configFile) throws IOException {
        this.config = new ConfigurationManager(configFile);
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
                compareEntities("ROOT", refEl.getAsJsonObject(), newEl.getAsJsonObject(), diffs);
            } else {
                // types différents en racine
                diffs.add(new Difference("ROOT", ChangeType.MODIFICATION, "", "(root)",
                        stringify(refEl), stringify(newEl)));
            }
            return diffs;
        }
    }

    // === Indexation d’un tableau par ID ===
    private Map<String, JsonObject> indexEntities(JsonArray arr) {
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

        // suppressions et modifications
        for (String id : refMap.keySet()) {
            if (!newMap.containsKey(id)) {
                diffs.add(new Difference(id, ChangeType.DELETION, "", "", null, null));
            } else {
                compareEntities(id, refMap.get(id), newMap.get(id), diffs);
            }
        }
        // ajouts
        for (String id : newMap.keySet()) {
            if (!refMap.containsKey(id)) {
                diffs.add(new Difference(id, ChangeType.ADDITION, "", "", null, null));
            }
        }
    }

    // === Comparaison détaillée d’une entité (objets/arrays/propriétés) ===
    private void compareEntities(String entityId, JsonObject refObj, JsonObject newObj, List<Difference> diffs) {
        Set<String> keys = new TreeSet<>();
        keys.addAll(refObj.keySet());
        keys.addAll(newObj.keySet());

        Map<String, List<String>> ignored = config.getIgnoredFields();

        for (String key : keys) {
            // ignorer clés configurées par section
            if (ignored.getOrDefault(key, Collections.emptyList()).contains(key)) continue;

            JsonElement v1 = refObj.has(key) ? refObj.get(key) : null;
            JsonElement v2 = newObj.has(key) ? newObj.get(key) : null;

            if (v1 == null && v2 != null) {
                handleAddition(entityId, "", key, v2, diffs);
            } else if (v1 != null && v2 == null) {
                diffs.add(new Difference(entityId, ChangeType.DELETION, "", key, stringify(v1), null));
            } else if (v1 != null) {
                // même clé présente
                if (v1.isJsonObject() && v2.isJsonObject()) {
                    compareNestedObjects(entityId, key, v1.getAsJsonObject(), v2.getAsJsonObject(), diffs);
                } else if (v1.isJsonArray() && v2.isJsonArray()) {
                    compareJsonArraysByKey(entityId, key, v1.getAsJsonArray(), v2.getAsJsonArray(), diffs);
                } else {
                    if (!Objects.equals(stringify(v1), stringify(v2))) {
                        diffs.add(new Difference(entityId, ChangeType.MODIFICATION, "", key,
                                stringify(v1), stringify(v2)));
                    }
                }
            }
        }
    }

    private void compareNestedObjects(String entityId, String section,
                                      JsonObject refObj, JsonObject newObj, List<Difference> diffs) {
        Set<String> keys = new TreeSet<>();
        keys.addAll(refObj.keySet());
        keys.addAll(newObj.keySet());

        Map<String, List<String>> ignored = config.getIgnoredFields();
        List<String> ignoredInSection = ignored.getOrDefault(section, Collections.emptyList());

        for (String key : keys) {
            if (ignoredInSection.contains(key)) continue;

            JsonElement v1 = refObj.has(key) ? refObj.get(key) : null;
            JsonElement v2 = newObj.has(key) ? newObj.get(key) : null;

            if (v1 == null && v2 != null) {
                handleAddition(entityId, section, key, v2, diffs);
            } else if (v1 != null && v2 == null) {
                diffs.add(new Difference(entityId, ChangeType.DELETION, section, key, stringify(v1), null));
            } else if (v1 != null) {
                if (v1.isJsonObject() && v2.isJsonObject()) {
                    compareNestedObjects(entityId, section + pathSep(section) + key,
                            v1.getAsJsonObject(), v2.getAsJsonObject(), diffs);
                } else if (v1.isJsonArray() && v2.isJsonArray()) {
                    compareJsonArraysByKey(entityId, section + pathSep(section) + key,
                            v1.getAsJsonArray(), v2.getAsJsonArray(), diffs);
                } else {
                    if (!Objects.equals(stringify(v1), stringify(v2))) {
                        diffs.add(new Difference(entityId, ChangeType.MODIFICATION, section, key,
                                stringify(v1), stringify(v2)));
                    }
                }
            }
        }
    }

    private void compareJsonArraysByKey(String entityId, String section,
                                        JsonArray refArr, JsonArray newArr, List<Difference> diffs) {
        // Si ce sont des tableaux de primitives -> comparer ensembliste
        if (isPrimitiveArray(refArr) && isPrimitiveArray(newArr)) {
            Set<String> a = toStringSet(refArr);
            Set<String> b = toStringSet(newArr);
            for (String x : diffOnly(a, b)) {
                diffs.add(new Difference(entityId, ChangeType.DELETION, section, "(item)", x, null));
            }
            for (String x : diffOnly(b, a)) {
                diffs.add(new Difference(entityId, ChangeType.ADDITION, section, "(item)", null, x));
            }
            return;
        }

        // Tableaux d’objets -> on indexe par clé(s) configurée(s) pour la section
        List<String> keys = config.getSubSectionKeys(section);
        if (keys.isEmpty()) {
            // fallback: comparer par toString() de l’objet
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
            if (!B.containsKey(k)) {
                diffs.add(new Difference(entityId, ChangeType.DELETION, section, k, stringify(A.get(k)), null));
            } else {
                compareNestedObjects(entityId, section, A.get(k), B.get(k), diffs);
            }
        }
        for (String k : B.keySet()) {
            if (!A.containsKey(k)) {
                diffs.add(new Difference(entityId, ChangeType.ADDITION, section, k, null, stringify(B.get(k))));
            }
        }
    }

    private void handleAddition(String entityId, String section, String key,
                                JsonElement newVal, List<Difference> diffs) {
        if (newVal.isJsonObject()) {
            compareNestedObjects(entityId, section + pathSep(section) + key,
                    new JsonObject(), newVal.getAsJsonObject(), diffs);
        } else if (newVal.isJsonArray()) {
            compareJsonArraysByKey(entityId, section + pathSep(section) + key,
                    new JsonArray(), newVal.getAsJsonArray(), diffs);
        } else {
            diffs.add(new Difference(entityId, ChangeType.ADDITION, section, key, null, stringify(newVal)));
        }
    }

    // === Utilitaires ===

    private static String pathSep(String section) { return section.isEmpty() ? "" : "."; }

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

    private String extractEntityId(JsonObject obj) {
        // 1) primary_key
        String id = lookupByKeyName(obj, config.getPrimaryKey(), config.getFallbackKey());
        if (id != null) return id;
        // 2) fallback_key global
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
            // si l’objet contient un seul champ primitif, l’utiliser
            for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                if (e.getValue().isJsonPrimitive()) return e.getValue().getAsString();
            }
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

    private static String stringify(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonPrimitive()) return e.getAsJsonPrimitive().getAsString();
        return e.toString();
    }
}
