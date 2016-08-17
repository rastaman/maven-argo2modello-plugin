package com.ubikproducts.maven.plugins.argo2modello;

import java.util.LinkedHashMap;
import java.util.Map;

public class MetricsRegistry {

    private Map<String, Double> counters = new LinkedHashMap<String, Double>();

    public void addCounter(String counterName) {
        counters.put(counterName, 0D);
    }

    public void increment(String counter) {
        counters.put(counter, counters.get(counter) + 1);
    }

    public double getValue(String counter) {
        if (!counters.containsKey(counter)) {
            throw new IllegalArgumentException("Counter '" + counter + "' has not been registered.");
        }
        return counters.get(counter);
    }

    public Map<String, Double> getCounters() {
        return counters;
    }
}
