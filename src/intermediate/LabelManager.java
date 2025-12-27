package intermediate;

import java.util.HashMap;
import java.util.Map;

public class LabelManager {

    // brojaci po tipu labele
    // kljuc je labela
    private final Map<String, Integer> counters = new HashMap<>();

    // kreiranje nove labele
    public String newLabel(String hint) {
        String key = hint.toUpperCase();

        int count = counters.getOrDefault(key, 0);
        counters.put(key, count + 1);

        return key + "_" + count;
    }
}
