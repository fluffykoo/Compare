package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class JsonComparator {
    private ConfigurationManager config;

    public JsonComparator(String configFile) throws IOException {
        this.config = new ConfigurationManager(configFile);
    }

    public ConfigurationManager getConfig() {
        return config;
    }

    public List<Difference> compare(String refFilePath, String newFilePath) throws IOException {
        JsonArray refArray = JsonParser.parseReader(new FileReader(refFilePath)).getAsJsonArray();
        JsonArray newArray = JsonParser.parseReader(new FileReader(newFilePath)).getAsJsonArray();

        Map<String, JsonObject> refMap = indexEntities(refArray);
        Map<String, JsonObject> newMap = indexEntities(newArray);

        List<Difference> differences = new ArrayList<>();

        for (String id : refMap.keySet()) {
            if (!newMap.containsKey(id)) {
                differences.add(new Difference(id, ChangeType.DELETION, "", "", refMap.get(id).toString(), null));
            } else {
                compareEntities(id, refMap.get(id), newMap.get(id), differences);
            }
        }

        for (String id : newMap.keySet()) {
            if (!refMap.containsKey(id)) {
                differences.add(new Difference(id, ChangeType.ADDITION, "", "", null, newMap.get(id).toString()));
            }
        }

        return differences;
    }

    public Map<String, JsonObject> indexEntities(JsonArray array) {
        Map<String, JsonObject> map = new LinkedHashMap<>();
        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();
            String id = extractCompositeId(obj, config.getPrimaryKeys(), config.getFallbackKey());
            if (id != null) map.put(id, obj);
        }
        return map;
    }

    private String extractCompositeId(JsonObject obj, List<String> keys, String fallbackKey) {
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (obj.has(key)) sb.append(obj.get(key).getAsString());
        }
        if (sb.length() == 0 && fallbackKey != null && obj.has(fallbackKey))
            sb.append(obj.get(fallbackKey).getAsString());
        return sb.length() > 0 ? sb.toString() : null;
    }

    private void compareEntities(String id, JsonObject refObj, JsonObject newObj, List<Difference> differences) {
        for (String key : refObj.keySet()) {
            if (!newObj.has(key)) {
                differences.add(new Difference(id, ChangeType.DELETION, "", key, refObj.get(key).toString(), null));
            } else if (!Objects.equals(refObj.get(key), newObj.get(key))) {
                differences.add(new Difference(id, ChangeType.MODIFICATION, "", key, refObj.get(key).toString(), newObj.get(key).toString()));
            }
        }

        for (String key : newObj.keySet()) {
            if (!refObj.has(key)) {
                differences.add(new Difference(id, ChangeType.ADDITION, "", key, null, newObj.get(key).toString()));
            }
        }
    }
}
