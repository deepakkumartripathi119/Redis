import java.sql.SQLOutput;
import java.time.Instant;
import java.util.HashMap;

public class RequestParser {
    public String requestParser(String input) {
        if (!input.startsWith("*")) {
            return "error: expected '*'\r\n";
        }
        String[] chunks = input.split("\r\n");
        return requestProcessor(chunks);
    }

    public String requestProcessor(String[] chunks) {
        if (chunks.length >= 5 && chunks[2].equalsIgnoreCase("ECHO")) {
            String data = chunks[4];
            return ("$" + data.length() + "\r\n" + data + "\r\n");
        } else if (chunks[2].equalsIgnoreCase("PING")) {
            return ("+PONG\r\n");
        } else if (chunks.length >= 7 && chunks[2].equalsIgnoreCase("SET")) {
            String key = chunks[4];
            String val = chunks[6];
            GlobeStore.data.put(key, val);
            if (chunks.length >= 11 && chunks[8].equalsIgnoreCase("PX")) {
                String exp = chunks[10];
                long millSec = Long.parseLong(exp);
                GlobeStore.expTime.put(key, Instant.now().plusMillis(millSec));
            }
            else if(chunks.length >= 11 && chunks[8].equalsIgnoreCase("EX"))
            {
                String exp = chunks[10];
                long sec = Long.parseLong(exp);
                GlobeStore.expTime.put(key, Instant.now().plusSeconds(sec));
            }
            return ("+OK\r\n");
        } else if (chunks.length >= 5 && chunks[2].equalsIgnoreCase("GET")) {
            String key = chunks[4];
            String val = GlobeStore.data.get(key);
            if (val.isEmpty()) return "$-1\r\n";
            Instant expiry = GlobeStore.expTime.get(key);
            if(expiry!=null&&Instant.now().isAfter(expiry))
            {
                GlobeStore.expTime.remove(key);
                return "$-1\r\n";
            }
            return ("$" + val.length() + "\r\n" + val + "\r\n");
        }
        return "$-1\r\n";
    }
}
