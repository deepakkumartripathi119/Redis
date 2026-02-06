package Services;

import utils.GlobeStore;

import java.time.Instant;
import java.util.ArrayDeque;

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

    public static String processRPush(String[] chunks) {
        if (chunks.length >= 7) {
            String list = chunks[4];
            String size = "";
            ArrayDeque<String> myList = GlobeStore.rPushList.get(list);
            if (myList == null) {
                ArrayDeque<String> newList = new ArrayDeque<>();
                for (int i = 6; i < chunks.length; i += 2) {
                    newList.add(chunks[i]);
                }
                GlobeStore.rPushList.put(list, newList);
                size = String.valueOf(newList.size());
            } else {
                for (int i = 6; i < chunks.length; i += 2) {
                    myList.add(chunks[i]);
                }
                GlobeStore.rPushList.put(list, myList);
                size = String.valueOf(myList.size());
            }

            return ":" + size + "\r\n";
        }
        return "$-1\r\n";
    }

    public static String processLPush(String[] chunks) {
        if (chunks.length >= 7) {
            String list = chunks[4];
            String size = "";
            ArrayDeque<String> myList = GlobeStore.rPushList.get(list);
            if (myList == null) {
                ArrayDeque<String> newList = new ArrayDeque<>();
                for (int i = 6; i < chunks.length; i += 2) {
                    newList.addFirst(chunks[i]);
                }
                GlobeStore.rPushList.put(list, newList);
                size = String.valueOf(newList.size());
            } else {
                for (int i = 6; i < chunks.length; i += 2) {
                    myList.addFirst(chunks[i]);
                }
                GlobeStore.rPushList.put(list, myList);
                size = String.valueOf(myList.size());
            }

            return ":" + size + "\r\n";
        }
        return "$-1\r\n";
    }

    public static String processLrange(String[] chunks) {
        if (chunks.length >= 9) {
            String list = chunks[4];
            ArrayDeque<String> myList = GlobeStore.rPushList.get(list);
            if (myList == null) return "*0\r\n";
            int l = Integer.parseInt(chunks[6]);
            int r = Integer.parseInt(chunks[8]);

            if (l < 0) l = myList.size() + l;
            if (r < 0) r = myList.size() + r;
            if (l > r) return "*0\r\n";
            if (l < 0) l = 0;
            if (r < 0) r = 0;
            r = Integer.min(r, myList.size() - 1);

            String output = "*" + (r - l + 1) + "\r\n";
            Object[] arr = myList.toArray();

            for (int i = l; i <= r; i++) {
                String val = (String) arr[i];
                output = output.concat("$" + val.length() + "\r\n" + val + "\r\n");
            }
            return output;
        }
        return "*0\r\n";
    }

    public static String processLlen(String[] chunks) {
        if (chunks.length >= 5) {
            String list = chunks[4];
            String size = "";
            ArrayDeque<String> myList = GlobeStore.rPushList.get(list);
            if (myList == null) {
                return ":0\r\n";
            } else {
                size = String.valueOf(myList.size());
            }

            return ":" + size + "\r\n";
        }
        return ":0\r\n";
    }

    public static String processLpop(String[] chunks) {
        if (chunks.length >= 5) {
            String list = chunks[4];
            String size = "";
            ArrayDeque<String> myList = GlobeStore.rPushList.get(list);
            if (myList == null || myList.isEmpty()) {
                return "$-1\r\n";
            }
            String first = myList.getFirst();
            myList.removeFirst();

            return "$" + first.length() + "\r\n" + first + "\r\n";
        }
        return "$-1\r\n";
    }
}