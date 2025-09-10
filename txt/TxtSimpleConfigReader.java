package com.mmd.txt;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class TxtSimpleConfigReader {
    private List<Integer> indexCols;
    private Set<Integer> colonnesIgnorees;
    private String separator;
    // Par défaut : 1 (noms de colonnes sur la 1ère ligne, index 0)
    private int headerLineIndex = 1;
    private String reportFileName;

    public TxtSimpleConfigReader(String cheminConfig) throws IOException {
        try (FileReader reader = new FileReader(cheminConfig)) {
            Gson gson = new Gson();
            Map<String, Object> config = gson.fromJson(reader, Map.class);

            Object indexColObj = config.get("indexCol");
            this.indexCols = new ArrayList<>();
            if (indexColObj instanceof Double) {
                indexCols.add(((Double) indexColObj).intValue() - 1);
            } else if (indexColObj instanceof List) {
                List<Double> indices = (List<Double>) indexColObj;
                for (Double d : indices) {
                    indexCols.add(d.intValue() - 1);
                }
            } else {
                throw new IllegalArgumentException("'indexCol' must be an integer or list of integers.");
            }

            this.colonnesIgnorees = new HashSet<>();
            if (config.containsKey("ignoreColumns")) {
                List<Double> rawList = (List<Double>) config.get("ignoreColumns");
                for (Double d : rawList) {
                    colonnesIgnorees.add(d.intValue() - 1);
                }
            }

            this.separator = config.containsKey("separator") ? config.get("separator").toString() : "|";

            // Gestion intuitive de l'index de la ligne d'en-tête
            if (config.containsKey("headerLineIndex")) {
                Object headerObj = config.get("headerLineIndex");
                if (headerObj instanceof Double) {
                    int val = ((Double) headerObj).intValue();
                    if (val <= 0) {
                        // 0 ou négatif : pas d'entête, toutes les lignes sont des données
                        this.headerLineIndex = -1;
                    } else {
                        // 1 = 1ère ligne, 2 = 2ème, etc. (indexation humaine)
                        this.headerLineIndex = val - 1;
                    }
                }
            }
            if (config.containsKey("reportFileName")) {
                this.reportFileName = config.get("reportFileName").toString();
            } else {
                this.reportFileName = "rapport"; // valeur par default
            }
        }
    }

    public List<Integer> getIndexCols() {
        return indexCols;
    }

    public Set<Integer> getColonnesIgnorees() {
        return colonnesIgnorees;
    }

    public String getSeparator() {
        return separator;
    }

    public int getHeaderLineIndex() {
        return headerLineIndex;
    }
    public String getReportFileName() {
        return reportFileName;
    }
}
