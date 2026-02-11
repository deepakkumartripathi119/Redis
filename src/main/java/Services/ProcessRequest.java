package Services;

import utils.GlobeStore;
import utils.Stream;
import utils.StreamEntry;

import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class ProcessRequest {

    public static String processEcho(String[] chunks) {
        if (chunks.length >= 2) {
            String data = chunks[1];
            return "$" + data.length() + "\r\n" + data + "\r\n";
        }
        return "$-1\r\n";
    }

    public static String processSet(String[] chunks) {
        if (chunks.length >= 3) {
            String key = chunks[1];
            String val = chunks[2];
            GlobeStore.data.put(key, val);

            if (chunks.length >= 5 && chunks[3].equalsIgnoreCase("PX")) {
                String exp = chunks[4];
                long millSec = Long.parseLong(exp);
                GlobeStore.expTime.put(key, Instant.now().plusMillis(millSec));
            } else if (chunks.length >= 5 && chunks[3].equalsIgnoreCase("EX")) {
                String exp = chunks[4];
                long sec = Long.parseLong(exp);
                GlobeStore.expTime.put(key, Instant.now().plusSeconds(sec));
            }
            GlobeStore.typeOfData.computeIfAbsent(key, k -> "string");
            return "+OK\r\n";
        }
        return "$-1\r\n";
    }

    public static String processGet(String[] chunks) {
        if (chunks.length >= 2) {
            String key = chunks[1];
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
        return processPush(chunks, false);
    }

    public static String processLPush(String[] chunks) {
        return processPush(chunks, true);
    }

    private static String processPush(String[] chunks, boolean leftPush) {
        if (chunks.length >= 3) {
            String list = chunks[1];
            String size = "";
            ArrayDeque<String> myList = GlobeStore.rPushList.computeIfAbsent(list, k -> new ArrayDeque<>());
            for (int i = 2; i < chunks.length; i++) {
                if (leftPush) {
                    myList.addFirst(chunks[i]);
                } else {
                    myList.add(chunks[i]);
                }
            }
            size = String.valueOf(myList.size());
            GlobeStore.typeOfData.computeIfAbsent(list, k -> "list");
            notifyBLpopWaiters(list);
            return ":" + size + "\r\n";
        }
        return "$-1\r\n";
    }

    public static String processLrange(String[] chunks) {
        if (chunks.length >= 4) {
            String list = chunks[1];

            ArrayDeque<String> myList = GlobeStore.rPushList.get(list);
            if (myList == null) return "*0\r\n";
            int l = Integer.parseInt(chunks[2]);
            int r = Integer.parseInt(chunks[3]);

            if (l < 0) l = myList.size() + l;
            if (r < 0) r = myList.size() + r;
            if (l > r) return "*0\r\n";
            if (l < 0) l = 0;
            if (r < 0) r = 0;
            r = Integer.min(r, myList.size() - 1);

            StringBuilder output = new StringBuilder();
            output.append("*").append(r - l + 1).append("\r\n");
            List<String> arr = new ArrayList<>(myList);

            for (int i = l; i <= r; i++) {
                String val = arr.get(i);
                output.append("$").append(val.length()).append("\r\n").append(val).append("\r\n");
            }
            return output.toString();
        }
        return "*0\r\n";
    }

    public static String processLlen(String[] chunks) {
        if (chunks.length >= 2) {
            String list = chunks[1];
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
        if (chunks.length >= 2) {
            String list = chunks[1];
            ArrayDeque<String> myList = GlobeStore.rPushList.get(list);
            if (myList == null || myList.isEmpty()) {
                return "$-1\r\n";
            }
            StringBuilder output = new StringBuilder();
            int count = 1;
            if (chunks.length >= 3) {
                count = Integer.parseInt(chunks[2]);
                output.append("*").append(count).append("\r\n");
            }
            int size = myList.size();
            for (int i = 0; i < Integer.min(size, count); i++) {
                String first = myList.getFirst();
                myList.removeFirst();
                output.append("$").append(first.length()).append("\r\n").append(first).append("\r\n");
            }
            return output.toString();
        }
        return "$-1\r\n";
    }

    public static String processBLpop(String[] chunks) throws InterruptedException {
        if (chunks.length >= 3) {
            String list = chunks[1];
            double wait = Double.parseDouble(chunks[2]);
            long exp = (long) (wait * 1000);

            GlobeStore.Ticket ticket = new GlobeStore.Ticket();

            GlobeStore.BLpopClients.computeIfAbsent(list, k -> new LinkedBlockingQueue<>()).add(ticket);

            notifyBLpopWaiters(list);

            synchronized (ticket) {
                if (exp == 0) {
                    while (!ticket.isDone) {
                        ticket.wait();
                    }
                } else {
                    long deadline = System.currentTimeMillis() + exp;
                    while (!ticket.isDone) {
                        long timeLeft = deadline - System.currentTimeMillis();
                        if (timeLeft <= 0) break;
                        ticket.wait(timeLeft);
                    }
                }
            }

            if (ticket.value != null) {
                return "*2\r\n$" + list.length() + "\r\n" + list + "\r\n$" + ticket.value.length() + "\r\n" + ticket.value + "\r\n";
            } else {
                GlobeStore.BLpopClients.get(list).remove(ticket);
                return "*-1\r\n";
            }
        }
        return "$-1\r\n";
    }

    public static String processType(String[] chunks) {
        if (chunks.length >= 2) {
            String list = chunks[1];
            String type = GlobeStore.typeOfData.get(list);
            if (type != null) {
                return "+" + type + "\r\n";
            }
            return "+none\r\n";
        }
        return "$-1\r\n";
    }

    public static String processXAdd(String[] chunks) {
        String streamList = chunks[1];
        String id = chunks[2];

        Stream stream = GlobeStore.streamMap.computeIfAbsent(streamList, k -> new Stream());

        String[] ids;
        if (id.equals("*")) {
            ids = new String[]{String.valueOf(System.currentTimeMillis()), "*"};
        } else {
            ids = id.split("-");
        }

        if (ids.length > 1 && ids[1].equals("*")) {
            StreamEntry lastEntry = stream.getLastEntry();
            if (lastEntry == null) {
                ids[1] = (ids[0].equals("0")) ? "1" : "0";
            } else {
                String[] lastIdParts = lastEntry.getId().split("-");
                long lmill = Long.parseLong(lastIdParts[0]);
                long lcnt = Long.parseLong(lastIdParts[1]);
                long currentMill = Long.parseLong(ids[0]);

                if (currentMill == lmill) {
                    ids[1] = String.valueOf(lcnt + 1);
                } else {
                    ids[1] = "0";
                }
            }
        }

        String finalId = ids[0] + "-" + ids[1];

        int errorType = validateStreamId(finalId, streamList);
        if (errorType == 1) {
            return "-ERR The ID specified in XADD must be greater than 0-0\r\n";
        } else if (errorType == 2) {
            return "-ERR The ID specified in XADD is equal or smaller than the target stream top item\r\n";
        }

        ArrayList<String> data = new ArrayList<>(chunks.length - 3);
        for (int i = 3; i < chunks.length; i++) {
            data.add(chunks[i]);
        }

        StreamEntry entry = new StreamEntry(finalId, data);
        stream.addEntry(entry);
        GlobeStore.typeOfData.put(streamList, "stream");

        return "$" + finalId.length() + "\r\n" + finalId + "\r\n";
    }

    private static int validateStreamId(String id, String streamList) {
        String[] ids = id.split("-");
        long millis = Long.parseLong(ids[0]);
        long count = Long.parseLong(ids[1]);

        if (millis == 0 && count == 0) return 1;

        Stream stream = GlobeStore.streamMap.get(streamList);
        if (stream == null || stream.getLastEntry() == null) return 0;

        String[] lastEntryId = stream.getLastEntry().getId().split("-");
        long millLast = Long.parseLong(lastEntryId[0]);
        long cntLast = Long.parseLong(lastEntryId[1]);

        if (millLast > millis) return 2;
        if (millLast == millis && cntLast >= count) return 2;

        return 0;
    }

    private static void notifyBLpopWaiters(String list) {
        LinkedBlockingQueue<GlobeStore.Ticket> clients = GlobeStore.BLpopClients.get(list);
        if (clients == null || clients.isEmpty()) return;

        ArrayDeque<String> myList = GlobeStore.rPushList.get(list);
        if (myList == null) return;

        while (!clients.isEmpty()) {
            synchronized (myList) {
                if (myList.isEmpty()) break;
                GlobeStore.Ticket client = clients.poll();
                if (client == null) break;
                String value = myList.removeFirst();
                synchronized (client) {
                    client.value = value;
                    client.isDone = true;
                    client.notify();
                }
            }
        }
    }
}
