package Services;

import utils.GlobeStore;

import java.time.Instant;
import java.util.ArrayDeque;
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
        if (chunks.length >= 3) {
            String list = chunks[1];
            String size = "";
            ArrayDeque<String> myList = GlobeStore.rPushList.get(list);
            if (myList == null) {
                ArrayDeque<String> newList = new ArrayDeque<>();
                for (int i = 2; i < chunks.length; i++) {
                    newList.add(chunks[i]);
                }
                GlobeStore.rPushList.put(list, newList);
                size = String.valueOf(newList.size());
            } else {
                for (int i = 2; i < chunks.length; i++) {
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
        if (chunks.length >= 3) {
            String list = chunks[1];
            String size = "";
            ArrayDeque<String> myList = GlobeStore.rPushList.get(list);
            if (myList == null) {
                ArrayDeque<String> newList = new ArrayDeque<>();
                for (int i = 2; i < chunks.length; i++) {
                    newList.addFirst(chunks[i]);
                }
                GlobeStore.rPushList.put(list, newList);
                size = String.valueOf(newList.size());
            } else {
                for (int i = 2; i < chunks.length; i++) {
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
            String output = "";
            int count = 1;
            if (chunks.length >= 3) {
                count = Integer.parseInt(chunks[2]);
                output = output.concat("*" + count + "\r\n");
            }
            int size = myList.size();
            for (int i = 0; i < Integer.min(size, count); i++) {
                String first = myList.getFirst();
                myList.removeFirst();
                output = output.concat("$" + first.length() + "\r\n" + first + "\r\n");
            }
            return output;
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

            synchronized (GlobeStore.schedulers) {
                if (!GlobeStore.schedulers.containsKey(list)) {
                    createNewSchedulerAndRunIt(list);
                }
            }

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

    public static String processType(String[] chunks){
        if(chunks.length >= 2)
        {
            String list = chunks[1];
            if(GlobeStore.data.get(list)!=null)
            {
                return "+string\r\n";
            }
            return "+none\r\n";
        }
        return "$-1\r\n";
    }

    private static void createNewSchedulerAndRunIt(String list) {
        ScheduledExecutorService BlpopScheduler = Executors.newSingleThreadScheduledExecutor();
        GlobeStore.schedulers.put(list, BlpopScheduler);

        BlpopScheduler.scheduleWithFixedDelay(() -> {
            try {
                if (checkClientQueueEmptyAndCloseScheduler(list, BlpopScheduler)) {
                    return;
                }

                if (checkListCreatedOrNot(list)) {
                    ArrayDeque<String> myList = GlobeStore.rPushList.get(list);
                    synchronized (myList) {
                        if (!myList.isEmpty()) {
                            LinkedBlockingQueue<GlobeStore.Ticket> clients = GlobeStore.BLpopClients.get(list);
                            GlobeStore.Ticket client = clients.poll();

                            if (client != null) {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 10, TimeUnit.MILLISECONDS);
    }

    private static boolean checkClientQueueEmptyAndCloseScheduler(String list, ScheduledExecutorService BlpopScheduler) {
        synchronized (GlobeStore.schedulers) {
            LinkedBlockingQueue<GlobeStore.Ticket> clients = GlobeStore.BLpopClients.get(list);
            if (clients == null || clients.isEmpty()) {
                BlpopScheduler.shutdown();
                GlobeStore.schedulers.remove(list);
                return true;
            }
            return false;
        }
    }

    private static boolean checkListCreatedOrNot(String list) {
        ArrayDeque<String> myList = GlobeStore.rPushList.get(list);
        return myList != null;
    }
}
