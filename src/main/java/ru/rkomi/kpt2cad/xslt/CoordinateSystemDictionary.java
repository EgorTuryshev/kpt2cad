package ru.rkomi.kpt2cad.xslt;

import ru.rkomi.kpt2cad.KnownTransformation;
import java.util.HashMap;
import java.util.Map;

public class CoordinateSystemDictionary {

    // Словарь для хранения соответствий кадастровых номеров и преобразований из KnownTransformation
    private final Map<String, KnownTransformation> coordinateSystemMap;

    public CoordinateSystemDictionary() {
        coordinateSystemMap = new HashMap<>();
        initializeDictionary();
    }

    private void initializeDictionary() {
        coordinateSystemMap.put("11:01", KnownTransformation.MSK11_Q4_WGS84);
        coordinateSystemMap.put("11:02", KnownTransformation.MSK11_Q4_WGS84);
        coordinateSystemMap.put("11:03", KnownTransformation.MSK11_Q4_WGS84);
        coordinateSystemMap.put("11:04", KnownTransformation.MSK11_Q4_WGS84);
        coordinateSystemMap.put("11:05", KnownTransformation.MSK11_Q4_WGS84);
        coordinateSystemMap.put("11:06", KnownTransformation.MSK11_Q4_WGS84);
        coordinateSystemMap.put("11:07", KnownTransformation.MSK11_Q5_WGS84);
        coordinateSystemMap.put("11:08", KnownTransformation.MSK11_Q4_WGS84);
        coordinateSystemMap.put("11:09", KnownTransformation.MSK11_Q4_WGS84);
        coordinateSystemMap.put("11:10", KnownTransformation.MSK11_Q4_WGS84);
        coordinateSystemMap.put("11:11", KnownTransformation.MSK11_Q5_WGS84);
        coordinateSystemMap.put("11:12", KnownTransformation.MSK11_Q5_WGS84);
        coordinateSystemMap.put("11:13", KnownTransformation.MSK11_Q4_WGS84);
        coordinateSystemMap.put("11:14", KnownTransformation.MSK11_Q5_WGS84);
        coordinateSystemMap.put("11:15", KnownTransformation.MSK11_Q5_WGS84);
        coordinateSystemMap.put("11:16", KnownTransformation.MSK11_Q6_WGS84);
        coordinateSystemMap.put("11:17", KnownTransformation.MSK11_Q5_WGS84);
        coordinateSystemMap.put("11:18", KnownTransformation.MSK11_Q6_WGS84);
        coordinateSystemMap.put("11:19", KnownTransformation.MSK11_Q5_WGS84);
        coordinateSystemMap.put("11:20", KnownTransformation.MSK11_Q5_WGS84);
    }

    public KnownTransformation getCoordinateSystem(String cadNumber) {
        return coordinateSystemMap.get(cadNumber);
    }
}
