package org.futuresight.clevelandrtanextbustrain;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jacob on 5/20/16.
 */
public abstract class PersistentDataController {
    static String[] lines;
    static Map<String, Integer> lineIds = new HashMap<>();

    public static boolean linesStored() {
        return lines != null;
    }

    public static String[] getLines() {
        return lines;
    }

    public static void setLines(String[] l) {
        lines = l;
    }

    public static Map<String,Integer> getLineIdMap() {
        return lineIds;
    }
}
