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

    public List<Difference> compare(String referenceFile, String newFile) throws IOException {
        JsonArray refArray = JsonParser.parseReader(new FileReader(referenceFile)).getAsJsonArray();
        JsonArray newArray = JsonParser.parseReader(new FileReader(newFile)).getAsJsonArray();

        Map<String, JsonObject> refMap = indexEntities(refArray);
        Map<String, JsonObject> newMap = indexEntities(newArray);

        List<Difference> differences = new ArrayList<>();

        // Gestion des suppressions et modifications
        for (String entityId : refMap.keySet()) {
            if (!newMap.containsKey(entityId)) {
                differences.add(new Difference(entityId, ChangeType.DELETION, "", "", null, null));
            } else {
                compareEntities(entityId, refMap.get(entityId), newMap.get(entityId), differences);
            }
        }
        // Gestion des ajouts
        for (String entityId : newMap.keySet()) {
            if (!refMap.containsKey(entityId)) {
                differences.add(new Difference(entityId, ChangeType.ADDITION, "", "", null, null));
            }
        }
        return differences;
    }

    Map<String, JsonObject> indexEntities(JsonArray array) {
        Map<String, JsonObject> map = new LinkedHashMap<>();
        for (JsonElement elem : array) {
            JsonObject obj = elem.getAsJsonObject();
            String id = extractEntityId(obj);
            if (id != null) map.put(id, obj);
        }
        return map;
    }

    private String extractEntityId(JsonObject obj) {
        String id = extractIdByKey(obj, config.getPrimaryKey());
        if (id == null && config.getFallbackKey() != null) {
            id = extractIdByKey(obj, config.getFallbackKey());
        }
        return id;
    }

    private String extractIdByKey(JsonObject obj, String key) {
        if (!obj.has(key)) return null;
        JsonElement element = obj.get(key);
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonObject()) {
            JsonObject o = element.getAsJsonObject();
            if (!o.keySet().isEmpty()) {
                String firstKey = o.keySet().iterator().next();
                JsonElement value = o.get(firstKey);
                if (value.isJsonPrimitive()) return value.getAsString();
            }
        }
        return null;
    }

    // Logique principale de comparaison - similaire à l'original comparerSections
    private void compareEntities(String entityId, JsonObject ref, JsonObject nov, List<Difference> differences) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(ref.keySet());
        allKeys.addAll(nov.keySet());

        JsonObject subSectionKeys = config.getSubSectionKeys();

        for (String key : allKeys) {
            JsonElement refVal = ref.get(key);
            JsonElement novVal = nov.get(key);

            // Si la clé est une sous-section (tableau d'objets)
            if (subSectionKeys != null && subSectionKeys.has(key)) {
                List<String> subKeys = config.getSubSectionKeys(key);
                if (refVal != null && refVal.isJsonArray() && novVal != null && novVal.isJsonArray()) {
                    compareJsonArraysByKey(entityId, key, subKeys, refVal.getAsJsonArray(), novVal.getAsJsonArray(), differences);
                } else if (refVal != null && refVal.isJsonArray()) {
                    // Section supprimée
                    for (JsonElement elem : refVal.getAsJsonArray()) {
                        if (elem.isJsonObject()) {
                            JsonObject obj = elem.getAsJsonObject();
                            String subId = generateCompositeKey(obj, subKeys);
                            if (subId != null) {
                                differences.add(new Difference(entityId, ChangeType.DELETION, key, subId,
                                        elem.toString(), null));
                            }
                        }
                    }
                } else if (novVal != null && novVal.isJsonArray()) {
                    // Section ajoutée
                    for (JsonElement elem : novVal.getAsJsonArray()) {
                        if (elem.isJsonObject()) {
                            JsonObject obj = elem.getAsJsonObject();
                            String subId = generateCompositeKey(obj, subKeys);
                            if (subId != null) {
                                differences.add(new Difference(entityId, ChangeType.ADDITION, key, subId,
                                        null, elem.toString()));
                            }
                        }
                    }
                }
            }
            // Champ simple ou section classique
            else {
                List<String> ignoredKeys = config.getIgnoredFields(key);
                if (ignoredKeys.contains(key)) continue;
                if (refVal == null && novVal != null) {
                    differences.add(new Difference(entityId, ChangeType.ADDITION, key, key, null, novVal.toString()));
                } else if (refVal != null && novVal == null) {
                    differences.add(new Difference(entityId, ChangeType.DELETION, key, key, refVal.toString(), null));
                } else if (refVal != null && novVal != null && !refVal.equals(novVal)) {
                    if (refVal.isJsonPrimitive() && novVal.isJsonPrimitive()) {
                        String cleFinale = "Modified field : " + key;
                        differences.add(new Difference(entityId, ChangeType.MODIFICATION, key,
                                cleFinale, refVal.getAsString(), novVal.getAsString()));
                    } else if (refVal.isJsonObject() && novVal.isJsonObject()) {
                        // Comparaison récursive des objets imbriqués
                        compareNestedObjects(entityId, key, refVal.getAsJsonObject(), novVal.getAsJsonObject(), differences, subSectionKeys);
                    } else if (refVal.isJsonArray() && novVal.isJsonArray()) {
                        // Comparaison des tableaux sans clé spécifique
                        if (!refVal.equals(novVal)) {
                            differences.add(new Difference(entityId, ChangeType.MODIFICATION, key,
                                    "array", refVal.toString(), novVal.toString()));
                        }
                    } else {
                        // Types différents
                        differences.add(new Difference(entityId, ChangeType.MODIFICATION, key,
                                "mixed", refVal.toString(), novVal.toString()));
                    }
                }
            }
        }
    }

    // Comparaison des objets imbriqués - maintient la hiérarchie dans le chemin des clés
    private void compareNestedObjects(String entityId, String section, JsonObject ref, JsonObject nov,
                                      List<Difference> differences, JsonObject subSectionKeys) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(ref.keySet());
        allKeys.addAll(nov.keySet());

        List<String> ignored = config.getIgnoredFields(section);

        for (String key : allKeys) {
            if (ignored.contains(key)) continue;

            JsonElement refVal = ref.get(key);
            JsonElement novVal = nov.get(key);

            if (refVal == null && novVal != null) {
                differences.add(new Difference(entityId, ChangeType.ADDITION, section, key, null, novVal.toString()));
            } else if (refVal != null && novVal == null) {
                differences.add(new Difference(entityId, ChangeType.DELETION, section, key, refVal.toString(), null));
            } else if (refVal != null && novVal != null && !refVal.equals(novVal)) {
                // Cas des tableaux imbriqués
                if (refVal.isJsonArray() && novVal.isJsonArray()) {
                    if (subSectionKeys != null && subSectionKeys.has(key)) {
                        List<String> subKeys = config.getSubSectionKeys(key);
                        compareJsonArraysByKey(entityId, key, subKeys, refVal.getAsJsonArray(), novVal.getAsJsonArray(), differences);
                    } else {
                        if (!refVal.equals(novVal)) {
                            differences.add(new Difference(entityId, ChangeType.MODIFICATION, section,
                                    key, refVal.toString(), novVal.toString()));
                        }
                    }
                }
                // Cas des objets imbriqués
                else if (refVal.isJsonObject() && novVal.isJsonObject()) {
                    compareNestedObjects(entityId, section + "." + key, refVal.getAsJsonObject(),
                            novVal.getAsJsonObject(), differences, subSectionKeys);
                }
                // Cas des valeurs simples
                else {
                    differences.add(new Difference(entityId, ChangeType.MODIFICATION, section,
                            key, refVal.toString(), novVal.toString()));
                }
            }
        }
    }

    // Comparaison des tableaux par clé - support des clés composites
    private void compareJsonArraysByKey(String entityId, String section, List<String> subKeys,
                                        JsonArray refArray, JsonArray novArray, List<Difference> differences) {
        Map<String, JsonObject> mapRef = new HashMap<>();
        for (JsonElement elem : refArray) {
            if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                String key = generateCompositeKey(obj, subKeys);
                mapRef.put(key, obj);
            }
        }

        Map<String, JsonObject> mapNouv = new HashMap<>();
        for (JsonElement elem : novArray) {
            if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                String key = generateCompositeKey(obj, subKeys);
                mapNouv.put(key, obj);
            }
        }

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(mapRef.keySet());
        allKeys.addAll(mapNouv.keySet());

        List<String> ignored =  config.getIgnoredFields(section);

        for (String key : allKeys) {
            JsonObject objRef = mapRef.get(key);
            JsonObject objNouv = mapNouv.get(key);

            if (objRef == null) {
                differences.add(new Difference(entityId, ChangeType.ADDITION, section, key, null, objNouv.toString()));
            } else if (objNouv == null) {
                differences.add(new Difference(entityId, ChangeType.DELETION, section, key, objRef.toString(), null));
            } else {
                Set<String> fields = new HashSet<>();
                fields.addAll(objRef.keySet());
                fields.addAll(objNouv.keySet());



                for (String field : fields) {
                    if (ignored.contains(field)) continue;

                    JsonElement val1 = objRef.get(field);
                    JsonElement val2 = objNouv.get(field);

                    if (val1 == null && val2 != null) {
                        differences.add(new Difference(entityId, ChangeType.ADDITION, section,
                                key + "." + field, null, val2.toString()));
                    } else if (val1 != null && val2 == null) {
                        differences.add(new Difference(entityId, ChangeType.DELETION, section,
                                key + "." + field, val1.toString(), null));
                    } else if (val1 != null && val2 != null && !val1.equals(val2)) {
                        differences.add(new Difference(entityId, ChangeType.MODIFICATION, section,
                                key + "." + field, val1.toString(), val2.toString()));
                    }
                }
            }
        }
    }

    private String generateCompositeKey(JsonObject obj, List<String> keys) {
        return keys.stream()
                .map(k -> obj.has(k) && !obj.get(k).isJsonNull() ? obj.get(k).getAsString() : "")
                .collect(Collectors.joining("|"));
    }
    public ConfigurationManager getConfig() {
        return this.config;
    }
}