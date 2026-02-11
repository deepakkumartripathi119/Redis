package utils;

import java.util.LinkedHashMap;

public class Stream {
    private final LinkedHashMap<String, StreamEntry> entries = new LinkedHashMap<>();
    private StreamEntry lastEntry;

    public void addEntry(StreamEntry entry) {
        entries.put(entry.getId(), entry);
        this.lastEntry = entry;
    }
    
    public StreamEntry getLastEntry(){return lastEntry;}

    public StreamEntry getEntry(String id) {
        return entries.get(id);
    }

    public LinkedHashMap<String, StreamEntry> getEntries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}