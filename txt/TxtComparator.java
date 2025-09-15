package com.mmd.txt;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TxtComparator {
    private final boolean afficherDansTerminal;
    private final StringBuilder rapportTexte = new StringBuilder();
    private final List<String[]> xlsxData = new ArrayList<>();
    private int totalIdentique = 0;
    private final String delimiteur;

    public TxtComparator(boolean afficherDansTerminal, String delimiteur) {
        this.afficherDansTerminal = afficherDansTerminal;
        this.delimiteur = delimiteur;
    }

    public StringBuilder getRapportTexte() {
        return rapportTexte;
    }

    public List<String[]> getXlsxData() {
        return xlsxData;
    }

    public int getTotalIdentique() {
        return totalIdentique;
    }

    private void afficher(String ligne) {
        if (afficherDansTerminal) System.out.println(ligne);
        rapportTexte.append(ligne).append("\n");
    }



    public void runComparison(String fichier1, String fichier2, List<Integer> indexCles, Set<Integer> colonnesIgnorees, int headerLineIndex) throws IOException {
        List<String> lignesFichier1 = Files.readAllLines(Paths.get(fichier1));
        List<String> lignesFichier2 = Files.readAllLines(Paths.get(fichier2));

        // Gestion des fichiers vides, sans entête, ou avec entête technique
        String[] nomsColonnes;
        List<String> donneesRef;
        List<String> donneesNouv;
        if (lignesFichier1.isEmpty()) {
            // Fichier vide : noms de colonnes génériques, aucune donnée
            nomsColonnes = new String[]{"Column 1"};
            donneesRef = new java.util.ArrayList<>();
            donneesNouv = lignesFichier2.stream().filter(l -> !l.trim().isEmpty()).collect(Collectors.toList());
        } else if (headerLineIndex >= 0 && lignesFichier1.size() > headerLineIndex) {
            // On prend la ligne d'entête comme noms de colonnes
            String ligneEntete = lignesFichier1.get(headerLineIndex);
            nomsColonnes = ligneEntete.split(Pattern.quote(delimiteur));
            // Les données commencent après la ligne d'entête
            donneesRef = lignesFichier1.stream().skip(headerLineIndex + 1).filter(l -> !l.trim().isEmpty()).collect(Collectors.toList());
            donneesNouv = lignesFichier2.size() > headerLineIndex
                    ? lignesFichier2.stream().skip(headerLineIndex + 1).filter(l -> !l.trim().isEmpty()).collect(Collectors.toList())
                    : lignesFichier2.stream().filter(l -> !l.trim().isEmpty()).collect(Collectors.toList());
        } else {
            // Pas d'entête : toutes les lignes sont des données, noms de colonnes génériques
            int maxCols = 0;
            if (!lignesFichier1.isEmpty()) {
                String[] cols = lignesFichier1.get(lignesFichier1.size() - 1).split(Pattern.quote(delimiteur), -1);
                maxCols = cols.length;
            }
            nomsColonnes = new String[maxCols > 0 ? maxCols : 1];
            for (int i = 0; i < nomsColonnes.length; i++) {
                nomsColonnes[i] = "Column " + (i + 1);
            }
            donneesRef = lignesFichier1.stream().filter(l -> !l.trim().isEmpty()).collect(Collectors.toList());
            donneesNouv = lignesFichier2.stream().filter(l -> !l.trim().isEmpty()).collect(Collectors.toList());
        }

        Map<String, String> mapRef = toMap(donneesRef, indexCles);
        Map<String, String> mapNouv = toMap(donneesNouv, indexCles);

        Set<String> toutesLesCles = new HashSet<>();
        toutesLesCles.addAll(mapRef.keySet());
        toutesLesCles.addAll(mapNouv.keySet());



    for (String cle : toutesLesCles) {
            String ancienne = mapRef.get(cle);
            String nouvelle = mapNouv.get(cle);

            if (ancienne == null) {
                xlsxData.add(new String[]{"AJOUT", cle, "", "", nouvelle});
                afficher("[ADD] Key = " + cle);
                afficher("  New : " + nouvelle);
            } else if (nouvelle == null) {
                xlsxData.add(new String[]{"DELETION", cle, "", ancienne, ""});
                afficher("[DELETE] Key = " + cle);
                afficher("  Old : " + ancienne);
            } else if (!ancienne.equals(nouvelle)) {
                afficher("[MODIFIED] Key = " + cle);
                // Ajout d'une ligne MODIFICATION globale pour la clé
                xlsxData.add(new String[]{"MODIFICATION", cle, "", ancienne, nouvelle});
                List<String[]> differences = comparerColonnes(ancienne, nouvelle, nomsColonnes, cle, colonnesIgnorees);
                for (String[] ligne : differences) {
                    afficher("  * " + ligne[2] + " :");
                    afficher("    Old  : " + ligne[3]);
                    afficher("    New  : " + ligne[4]);
                }
                xlsxData.addAll(differences);
            } else {
                totalIdentique++;
            }
        }

        afficher("");
        afficher("=== Summary ===");
        afficher("Reference file : " + fichier1);
        afficher("New file       : " + fichier2);
        afficher("Total keys     : " + toutesLesCles.size());
        afficher("* Identical rows : " + totalIdentique);
        afficher("* Modified rows  : " + (int) xlsxData.stream().filter(l -> "MODIFICATION".equals(l[0])).map(l -> l[1]).distinct().count());
        afficher("* Added rows     : " + (int) xlsxData.stream().filter(l -> "AJOUT".equals(l[0])).count());
        afficher("* Deleted rows   : " + (int) xlsxData.stream().filter(l -> "DELETION".equals(l[0])).count());
    }

    private Map<String, String> toMap(List<String> lignes, List<Integer> indexCles) {
        return lignes.stream()
                .map(l -> l.split(Pattern.quote(delimiteur), -1))
                .filter(cols -> indexCles.stream().allMatch(i -> i < cols.length))
                .collect(Collectors.toMap(
                        cols -> indexCles.stream().map(i -> cols[i].trim()).collect(Collectors.joining("|")),
                        l -> String.join(delimiteur, l),
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    private List<String[]> comparerColonnes(String ancienne, String nouvelle, String[] noms, String cle, Set<Integer> colonnesIgnorees) {
        String[] oldCols = ancienne.split(Pattern.quote(delimiteur), -1);
        String[] newCols = nouvelle.split(Pattern.quote(delimiteur), -1);
        List<String[]> lignes = new ArrayList<>();

        for (int i = 0; i < Math.max(oldCols.length, newCols.length); i++) {
            if (colonnesIgnorees.contains(i)) continue;

            String v1 = i < oldCols.length ? oldCols[i].trim() : "";
            String v2 = i < newCols.length ? newCols[i].trim() : "";

            if (!v1.equals(v2)) {
                String nom = i < noms.length ? noms[i].trim() : "Column " + (i + 1);
                lignes.add(new String[]{"MODIFICATION", cle, nom, v1, v2});
            }
        }

        return lignes;
    }
}