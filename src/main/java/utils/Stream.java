package utils;
import java.util.LinkedHashMap;

public class Stream {
    private final LinkedHashMap<String, StreamEntry> entries = new LinkedHashMap<>();

    public void addEntry(StreamEntry entry) {
        entries.put(entry.id(), entry);
    }

    public StreamEntry getEntry(String id) {
        return entries.get(id);
    }

    public boolean isEmpty() { return entries.isEmpty(); }
}