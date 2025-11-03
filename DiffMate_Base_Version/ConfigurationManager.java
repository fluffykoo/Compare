package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class ConfigurationManager {
    private List<String> primaryKeys;
    private String fallbackKey;
    private JsonObject ignoredFields;

    public ConfigurationManager(String configFile) throws IOException {
        JsonObject config = JsonParser.parseReader(new FileReader(configFile)).getAsJsonObject();
        this.primaryKeys = new ArrayList<>();
        JsonElement pkElem = config.get("primary_key");
        if (pkElem.isJsonArray()) {
            for (JsonElement e : pkElem.getAsJsonArray()) primaryKeys.add(e.getAsString());
        } else {
            primaryKeys.add(pkElem.getAsString());
        }
        this.fallbackKey = config.has("fallback_key") ? config.get("fallback_key").getAsString() : null;
        this.ignoredFields = config.has("ignored_fields") ? config.get("ignored_fields").getAsJsonObject() : new JsonObject();
    }

    public List<String> getPrimaryKeys() { return primaryKeys; }
    public String getFallbackKey() { return fallbackKey; }
    public JsonObject getIgnoredFields() { return ignoredFields; }
}
