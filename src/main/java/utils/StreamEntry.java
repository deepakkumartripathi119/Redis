package utils;

import java.util.ArrayList;
import java.util.List;

public class StreamEntry {
    public String id;
    public ArrayList<String>body;

    public StreamEntry(String id, ArrayList<String> body) {
        this.id = id;
        this.body = body;
    }
    public String getId () {
        return id;
    }
    public List<String> getBody () {
        return body;
    }
}
