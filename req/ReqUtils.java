
package com.mmd.req;

import java.util.*;

public class ReqUtils {

    public static List<String> clean(List<String> lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (line != null && !line.trim().isEmpty()) {
                result.add(line.trim());
            }
        }
        return result;
    }

    public static Map<String, String> toMap(List<String> lines) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : lines) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                map.put(parts[0].trim(), parts[1].trim());
            }
        }
        return map;
    }

    public static Map<String, String> toMap(Set<String> lines) {
        return toMap(new ArrayList<>(lines));
    }
}
