package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Lit le fichier de configuration et expose:
 *  - primary_key             (ex: "commonData")
 *  - fallback_key            (ex: "mmdCode")
 *  - subSectionKeys.section -> [keys]
 *  - ignored_fields.section  -> [keys]
 *
 * Le fichier est cherché d'abord dans le dossier du JAR,
 * sinon on prend le chemin passé par l'utilisateur.
 */
public class ConfigurationManager {

    private final String primaryKey;
    private final String fallbackKey;
    private final JsonObject subSectionKeys;
    private final JsonObject ignoredFields;

    public ConfigurationManager(String configPath) throws IOException {
        File cfg = resolveConfig(configPath);
        try (Reader r = new InputStreamReader(new FileInputStream(cfg), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();

            this.primaryKey   = root.has("primary_key")   ? root.get("primary_key").getAsString() : null;
            this.fallbackKey  = root.has("fallback_key")  ? root.get("fallback_key").getAsString() : null;
            this.subSectionKeys = root.has("subSectionKeys") && root.get("subSectionKeys").isJsonObject()
                                  ? root.get("subSectionKeys").getAsJsonObject()
                                  : new JsonObject();
            this.ignoredFields  = root.has("ignored_fields") && root.get("ignored_fields").isJsonObject()
                                  ? root.get("ignored_fields").getAsJsonObject()
                                  : new JsonObject();
        }
    }

    private static File resolveConfig(String userArg) {
        // 1) dossier du JAR
        try {
            File jarDir = new File(ConfigurationManager.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .getParentFile();
            File candidate = new File(jarDir, userArg);
            if (candidate.exists()) return candidate;
        } catch (Exception ignore) {}

        // 2) chemin tel quel
        return new File(userArg);
    }

    public String getPrimaryKey()  { return primaryKey; }
    public String getFallbackKey() { return fallbackKey; }

    /** Liste des clés d’identification pour une section de tableau */
    public List<String> getSubSectionKeys(String section) {
        if (subSectionKeys.has(section)) {
            JsonElement e = subSectionKeys.get(section);
            List<String> keys = new ArrayList<>();
            if (e.isJsonPrimitive()) {
                keys.add(e.getAsString());
            } else if (e.isJsonArray()) {
                for (JsonElement x : e.getAsJsonArray()) keys.add(x.getAsString());
            }
            return keys;
        }
        return Collections.emptyList();
    }

    /** Map section -> liste de champs à ignorer */
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
