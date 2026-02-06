package utils;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class GlobeStore {
    // 1. 'public' means everyone can see it
    // 2. 'static' means there is only ONE copy for the whole program
    // 3. 'final' ensures you don't accidentally replace the map with a new one
    public static final ConcurrentHashMap<String, String> data = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Instant> expTime = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, ArrayDeque<String>> rPushList = new ConcurrentHashMap<>();
}
