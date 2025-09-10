
package com.mmd.req;

import java.util.*;

public class ReqSectionParser {

    public static Map<String, List<String>> extractSections(List<String> lines) {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        List<String> header = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        List<String> data = new ArrayList<>();

        boolean inFields = false, inData = false;

        for (String line : lines) {
            line = line.trim();
            if (line.equals("START-OF-FIELDS")) {
                inFields = true;
                continue;
            }
            if (line.equals("END-OF-FIELDS")) {
                inFields = false;
                continue;
            }
            if (line.equals("START-OF-DATA")) {
                inData = true;
                continue;
            }

            if (inData) {
                data.add(line);
            } else if (inFields) {
                fields.add(line);
            } else {
                header.add(line);
            }
        }

        sections.put("Header", header);
        sections.put("Fields", fields);
        sections.put("DATA", data);
        return sections;
    }
}

