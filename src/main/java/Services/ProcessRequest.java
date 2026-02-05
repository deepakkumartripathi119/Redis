package Services;
import utils.GlobeStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ProcessRequest {
    public static String processEcho(String[] chunks) {
        if (chunks.length >= 5) {
            String data = chunks[4];
            return "$" + data.length() + "\r\n" + data + "\r\n";
        }
        return "$-1\r\n";
    }

    public static String processSet(String[] chunks) {
        if (chunks.length >= 7) {
            String key = chunks[4];
            String val = chunks[6];
            GlobeStore.data.put(key, val);

            if (chunks.length >= 11 && chunks[8].equalsIgnoreCase("PX")) {
                String exp = chunks[10];
                long millSec = Long.parseLong(exp);
                GlobeStore.expTime.put(key, Instant.now().plusMillis(millSec));
            } else if (chunks.length >= 11 && chunks[8].equalsIgnoreCase("EX")) {
                String exp = chunks[10];
                long sec = Long.parseLong(exp);
                GlobeStore.expTime.put(key, Instant.now().plusSeconds(sec));
            }
            return "+OK\r\n";
        }
        return "$-1\r\n";
    }

    public static String processGet(String[] chunks) {
        if (chunks.length >= 5) {
            String key = chunks[4];
            String val = GlobeStore.data.get(key);

            if (val == null) {
                return "$-1\r\n";
            }

            Instant expiry = GlobeStore.expTime.get(key);
            if (expiry != null && Instant.now().isAfter(expiry)) {
                GlobeStore.data.remove(key);
                GlobeStore.expTime.remove(key);
                return "$-1\r\n";
            }

            return "$" + val.length() + "\r\n" + val + "\r\n";
        }
        return "$-1\r\n";
    }
    public static String processRPush(String[] chunks)
    {
        if (chunks.length >= 7) {
            String list = chunks[4];
            String val = chunks[6];
            String size = "";
            ArrayList<String> myList=GlobeStore.rPushList.get(list);
            if(myList==null){
                ArrayList<String> newList = new ArrayList<String>();
                newList.add(val);
                GlobeStore.rPushList.put(list,newList);
                size = String.valueOf(newList.size());
            }
            else
            {
                myList.add(val);
                GlobeStore.rPushList.put(list,myList);
                size = String.valueOf(myList.size());
            }

            return ":"+size+"\r\n";
        }
        return "$-1\r\n";
    }
}
