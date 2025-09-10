package com.mmd;

import com.mmd.json.CompareJsonFiles;
import com.mmd.req.CompareReqFiles;
import com.mmd.txt.CompareTxtFiles;

import java.io.File;

public class CompareLauncher {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage :");
            System.out.println("  For JSON :  json <file1> <file2> <reportfolder> <config.json>");
            System.out.println("  For REQ  :  req <file1> <file2> <reportfolder>");
            System.out.println("  For TXT  :  txt <file1> <file2> <reportfolder> <config.json> [terminal]");
            return;
        }

        String type = args[0].toLowerCase();
        String file1 = args[1];
        String file2 = args[2];

        if (!new File(file1).exists() || !new File(file2).exists()) {
            System.out.println("Error: One of the files does not exist.");
            return;
        }

        switch (type) {
            case "json":
                if (args.length < 4) {
                    System.out.println("Error: file missing.");
                    return;
                }
                String jsonFolder = args[3];
                String config = args.length >= 5 ? args[4] : "config.json";
                CompareJsonFiles.main(new String[]{file1, file2, jsonFolder, config});
                break;

            case "req":
                if (args.length < 4) {
                    System.out.println("Error: file missing.");
                    return;
                }
                String reqFolder = args[3];
                String reportFileName = args.length >= 5 ? args[4] : null;
                if (reportFileName != null) {
                    CompareReqFiles.main(new String[]{file1, file2, reqFolder, reportFileName});
                } else {
                    CompareReqFiles.main(new String[]{file1, file2, reqFolder});
                }
                break;

            case "txt":
                if (args.length < 5) {
                    System.out.println("Error: indexCol or file missing.");
                    return;
                }
                String txtFolder = args[3];
                String indexCol = args[4];
                String terminal = args.length >= 6 ? args[5] : "";
                CompareTxtFiles.main(new String[]{file1, file2, txtFolder, indexCol, terminal});
                break;

            default:
                System.out.println("Unrecognized type : " + type);
        }
    }
}