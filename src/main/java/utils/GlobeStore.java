package utils;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

public class GlobeStore {
    // 1. 'public' means everyone can see it
    // 2. 'static' means there is only ONE copy for the whole program
    // 3. 'final' ensures you don't accidentally replace the map with a new one
    public static final ConcurrentHashMap<String, String> data = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, String> typeOfData = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Instant> expTime = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, ArrayDeque<String>> rPushList = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, LinkedBlockingQueue<Ticket>> BLpopClients = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, ScheduledExecutorService> schedulers = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Stream> streamMap = new ConcurrentHashMap<>();

    public static class Ticket {
        public String value = null;
        public boolean isDone = false;

        public Ticket(String value, boolean isDone) {
            this.value = value;
            this.isDone = isDone;
        }

        public Ticket() {
            String value = null;
            boolean isDone = false;
        }
    }
}
