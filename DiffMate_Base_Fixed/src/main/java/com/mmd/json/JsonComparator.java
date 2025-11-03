package com.mmd.json;

import com.google.gson.*;
import java.io.Reader;
import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Compare deux fichiers JSON.
 * - Racine = tableau : chaque élément est une entité.
 * - Racine = objet   : entité unique "ROOT".
 * ID d’entité = primary_key (avec éventuelle sous-clé), sinon fallback_key
 * cherché en profondeur.
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
                compareEntities("ROOT", "", refEl.getAsJsonObject(), newEl.getAsJsonObject(), diffs);
            } else {
                // types différents en racine
                diffs.add(new Difference("ROOT", ChangeType.MODIFICATION, "", "(root)",
                        stringify(refEl), stringify(newEl)));
            }
            return diffs;
        }
    }

    // === Indexation d’un tableau par ID (publique pour displaySummary) ===
    public Map<String, JsonObject> indexEntities(JsonArray arr) {
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
                compareEntities(id, "", refMap.get(id), newMap.get(id), diffs);
            }
        }
        // ajouts
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

        // champs ignorés pour la section courante
        Map<String, List<String>> ignored = config.getIgnoredFields();
        List<String> ignoredInSection = ignored.getOrDefault(section, Collections.<String>emptyList());

        for (String key : allKeys) {
            if (ignoredInSection.contains(key)) continue;

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
            if (v1 == null) continue; // les deux null n’arrivent pas ici

            if (v1.isJsonObject() && v2.isJsonObject()) {
                // descente de section
                String nextSection = section.isEmpty() ? key : section + "." + key;
                compareEntities(entityId, nextSection, v1.getAsJsonObject(), v2.getAsJsonObject(), diffs);
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

    private void compareJsonArraysByKey(String entityId, String section,
                                        JsonArray refArr, JsonArray newArr, List<Difference> diffs) {
        // tableaux de primitives -> diff ensembliste
        if (isPrimitiveArray(refArr) && isPrimitiveArray(newArr)) {
            Set<String> A = toStringSet(refArr);
            Set<String> B = toStringSet(newArr);
            for (String x : diffOnly(A, B))
                diffs.add(new Difference(entityId, ChangeType.DELETION, section, "(item)", x, null));
            for (String x : diffOnly(B, A))
                diffs.add(new Difference(entityId, ChangeType.ADDITION, section, "(item)", null, x));
            return;
        }

        // tableaux d’objets -> indexation par clés de sous-section si dispo
        List<String> keys = config.getSubSectionKeys(section);
        if (keys == null || keys.isEmpty()) {
            // fallback: comparer ensemblistement via toString()
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
                compareEntities(entityId, section, A.get(k), B.get(k), diffs);
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
            String nextSection = section.isEmpty() ? key : section + "." + key;
            compareEntities(entityId, nextSection, new JsonObject(), newVal.getAsJsonObject(), diffs);
        } else if (newVal.isJsonArray()) {
            String nextSection = section.isEmpty() ? key : section + "." + key;
            compareJsonArraysByKey(entityId, nextSection, new JsonArray(), newVal.getAsJsonArray(), diffs);
        } else {
            diffs.add(new Difference(entityId, ChangeType.ADDITION, section, key, null, stringify(newVal)));
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
        return parts.isEmpty() ? null : join(parts, "|");
    }

    // concat sans String.join pour rester très compatible
    private static String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private String extractEntityId(JsonObject obj) {
        // 1) primary_key
        String id = lookupByKeyName(obj, config.getPrimaryKey(), config.getFallbackKey());
        if (id != null) return id;
        // 2) fallback_key global cherché en profondeur
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

    // Version Java 8: pas de stream().toList()
    private String deepFindValueByKey(JsonObject obj, String keyName) {
        if (keyName == null) return null;
        Deque<JsonElement> stack = new ArrayDeque<JsonElement>();
        stack.push(obj);
        while (!stack.isEmpty()) {
            JsonElement cur = stack.pop();
            if (cur.isJsonObject()) {
                JsonObject o = cur.getAsJsonObject();
                if (o.has(keyName) && o.get(keyName).isJsonPrimitive())
                    return o.get(keyName).getAsString();
                for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                    stack.push(e.getValue());
                }
            } else if (cur.isJsonArray()) {
                JsonArray a = cur.getAsJsonArray();
                for (JsonElement v : a) stack.push(v);
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
