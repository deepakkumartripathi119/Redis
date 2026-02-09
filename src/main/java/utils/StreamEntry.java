package utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @param body We use a flat list: [Key1, Value1, Key2, Value2, ...] This allows ANY structure, ANY length, and keeps order perfectly.
 */
public record StreamEntry(String id, ArrayList<String> body) {
    public String getId() {
        return id;
    }
    public List<String> getBody() {
        return body;
    }
}
