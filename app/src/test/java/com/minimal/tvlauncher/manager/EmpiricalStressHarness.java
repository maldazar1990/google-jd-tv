package com.minimal.tvlauncher.manager;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EMPIRICAL CHALLENGER STRESS HARNESS
 * Milestone 1: Foundation & Core Data Layer
 *
 * Verifies:
 * 1. High-Concurrency Read-Modify-Write (10 threads)
 * 2. Rapid Burst & Scale Stress (500 items, heap memory, JSON latency)
 * 3. Adversarial Injections & Character Fidelity
 * 4. Multi-Instance Lock Boundaries
 * 5. Corrupted Data Fault Tolerance
 */
public class EmpiricalStressHarness {

    public static class TestSharedPreferences implements SharedPreferences {
        private final Map<String, Object> map = new ConcurrentHashMap<>();

        @Override
        public synchronized Map<String, ?> getAll() {
            return new HashMap<>(map);
        }

        @Override
        public synchronized String getString(String key, String defValue) {
            Object v = map.get(key);
            return (v instanceof String) ? (String) v : defValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public synchronized Set<String> getStringSet(String key, Set<String> defValues) {
            Object v = map.get(key);
            if (v instanceof Set) {
                return new HashSet<>((Set<String>) v);
            }
            return defValues;
        }

        @Override
        public synchronized int getInt(String key, int defValue) {
            Object v = map.get(key);
            return (v instanceof Integer) ? (Integer) v : defValue;
        }

        @Override
        public synchronized long getLong(String key, long defValue) {
            Object v = map.get(key);
            return (v instanceof Long) ? (Long) v : defValue;
        }

        @Override
        public synchronized float getFloat(String key, float defValue) {
            Object v = map.get(key);
            return (v instanceof Float) ? (Float) v : defValue;
        }

        @Override
        public synchronized boolean getBoolean(String key, boolean defValue) {
            Object v = map.get(key);
            return (v instanceof Boolean) ? (Boolean) v : defValue;
        }

        @Override
        public synchronized boolean contains(String key) {
            return map.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new TestEditor();
        }

        public class TestEditor implements Editor {
            private final Map<String, Object> temp = new HashMap<>();
            private final Set<String> removed = new HashSet<>();
            private boolean clearRequested = false;

            @Override
            public synchronized Editor putString(String key, String value) {
                temp.put(key, value);
                removed.remove(key);
                return this;
            }

            @Override
            public synchronized Editor putStringSet(String key, Set<String> values) {
                temp.put(key, values != null ? new HashSet<>(values) : null);
                removed.remove(key);
                return this;
            }

            @Override
            public synchronized Editor putInt(String key, int value) {
                temp.put(key, value);
                removed.remove(key);
                return this;
            }

            @Override
            public synchronized Editor putLong(String key, long value) {
                temp.put(key, value);
                removed.remove(key);
                return this;
            }

            @Override
            public synchronized Editor putFloat(String key, float value) {
                temp.put(key, value);
                removed.remove(key);
                return this;
            }

            @Override
            public synchronized Editor putBoolean(String key, boolean value) {
                temp.put(key, value);
                removed.remove(key);
                return this;
            }

            @Override
            public synchronized Editor remove(String key) {
                removed.add(key);
                temp.remove(key);
                return this;
            }

            @Override
            public synchronized Editor clear() {
                clearRequested = true;
                temp.clear();
                removed.clear();
                return this;
            }

            @Override
            public synchronized boolean commit() {
                synchronized (TestSharedPreferences.this) {
                    if (clearRequested) {
                        map.clear();
                    }
                    for (String r : removed) {
                        map.remove(r);
                    }
                    for (Map.Entry<String, Object> e : temp.entrySet()) {
                        if (e.getValue() != null) {
                            map.put(e.getKey(), e.getValue());
                        } else {
                            map.remove(e.getKey());
                        }
                    }
                }
                return true;
            }

            @Override
            public void apply() {
                commit();
            }
        }
    }

    public static class TestContext extends Context {
        private final Map<String, TestSharedPreferences> prefs = new ConcurrentHashMap<>();

        @Override
        public Context getApplicationContext() {
            return this;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return prefs.computeIfAbsent(name, k -> new TestSharedPreferences());
        }

        public void clear() {
            prefs.clear();
        }
    }

    // =========================================================================
    // Empirical Test Runners
    // =========================================================================

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("  EMPIRICAL CHALLENGER STRESS SUITE: Milestone 1 Data Layer");
        System.out.println("=================================================================");

        int passed = 0;
        int failed = 0;

        try {
            testConcurrency10ThreadsZeroLoss();
            passed++;
            System.out.println("[PASS] Concurrency: 10 threads, zero lost updates, zero CME");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Concurrency 10 threads zero loss: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            testConcurrencyMixedReadModifyWrite();
            passed++;
            System.out.println("[PASS] Concurrency: Mixed read-modify-write across 10 threads");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Concurrency mixed RMW: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            testScale500BurstAndReorder();
            passed++;
            System.out.println("[PASS] Scale: 500 favorites burst, reorder, bounded heap & latency");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Scale 500 favorites: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            testAdversarialPayloads();
            passed++;
            System.out.println("[PASS] Adversarial: Special characters, JSON injection & unicode fidelity");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Adversarial payloads: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            testMultiInstanceChallenge();
            passed++;
            System.out.println("[PASS] Adversarial: Multi-instance lock boundaries evaluated");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Multi-instance challenge: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            testCorruptedJsonFaultTolerance();
            passed++;
            System.out.println("[PASS] Fault Tolerance: Corrupted JSON strings recovered gracefully");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Fault tolerance: " + t.getMessage());
            t.printStackTrace();
        }

        System.out.println("=================================================================");
        System.out.println("  TOTAL RESULTS: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("=================================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void testConcurrency10ThreadsZeroLoss() throws Exception {
        TestContext context = new TestContext();
        PreferencesManager manager = new PreferencesManager(context, "test_concurrency", true);

        int threadCount = 10;
        int itemsPerThread = 20;
        int expectedTotal = threadCount * itemsPerThread;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.execute(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < itemsPerThread; i++) {
                        String pkg = "com.stress.thread" + threadId + ".pkg" + i;
                        manager.addFavorite(pkg);
                    }
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        check(completed, "Concurrency test timed out after 15 seconds");
        check(errors.isEmpty(), "Unexpected exceptions during concurrency: " + errors);

        List<String> favorites = manager.getFavorites();
        check(favorites.size() == expectedTotal, "Expected exactly " + expectedTotal + " favorites, but got " + favorites.size());
        Set<String> uniqueFavorites = new HashSet<>(favorites);
        check(uniqueFavorites.size() == expectedTotal, "Expected zero duplicates in favorites, but found duplicate entries");
    }

    public static void testConcurrencyMixedReadModifyWrite() throws Exception {
        TestContext context = new TestContext();
        PreferencesManager manager = new PreferencesManager(context, "test_mixed", true);

        // Prepopulate
        for (int i = 0; i < 20; i++) {
            manager.addFavorite("com.init.fav" + i);
            manager.setAppHidden("com.init.hidden" + i, true);
        }

        int threadCount = 10;
        int iterations = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.execute(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations; i++) {
                        int mode = threadId % 5;
                        switch (mode) {
                            case 0:
                                String favPkg = "com.mix.t" + threadId + ".fav" + i;
                                manager.addFavorite(favPkg);
                                if (i % 2 == 0) manager.removeFavorite(favPkg);
                                break;
                            case 1:
                                String hiddenPkg = "com.mix.t" + threadId + ".hidden" + i;
                                manager.setAppHidden(hiddenPkg, true);
                                if (i % 2 == 0) manager.setAppHidden(hiddenPkg, false);
                                break;
                            case 2:
                                List<String> f = manager.getFavorites();
                                check(f != null, "getFavorites returned null");
                                boolean isFav = manager.isFavorite("com.init.fav" + (i % 20));
                                break;
                            case 3:
                                Set<String> h = manager.getHiddenApps();
                                check(h != null, "getHiddenApps returned null");
                                boolean isHid = manager.isAppHidden("com.init.hidden" + (i % 20));
                                break;
                            case 4:
                                manager.setGridColumns(i % 2 == 0 ? 3 : 6);
                                manager.setHighContrast(i % 2 == 0);
                                manager.setIconShape(i % 2 == 0 ? "circle" : "squircle");
                                manager.setAudioFeedbackEnabled(i % 2 == 0);
                                break;
                        }
                    }
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        check(completed, "Mixed RMW concurrency test timed out");
        check(errors.isEmpty(), "Unexpected exceptions in mixed RMW: " + errors);
    }

    public static void testScale500BurstAndReorder() {
        TestContext context = new TestContext();
        PreferencesManager manager = new PreferencesManager(context, "test_scale", true);

        int count = 500;
        List<String> list500 = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list500.add(String.format("com.scale.app.%04d", i));
        }

        // Measure heap before
        System.gc();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // 1. Rapid burst sequential add
        long startAdd = System.nanoTime();
        for (String pkg : list500) {
            manager.addFavorite(pkg);
        }
        long addDurationMs = (System.nanoTime() - startAdd) / 1_000_000;

        List<String> retrieved = manager.getFavorites();
        check(retrieved.size() == count, "Expected 500 items, got: " + retrieved.size());
        check(retrieved.equals(list500), "Insertion order of 500 burst items was not preserved!");

        // 2. Measure batch serialization time
        long startSet = System.nanoTime();
        manager.setFavorites(list500);
        long setDurationMs = (System.nanoTime() - startSet) / 1_000_000;
        check(setDurationMs < 100, "setFavorites(500) took too long: " + setDurationMs + "ms");

        // 3. Measure deserialization time
        long startGet = System.nanoTime();
        List<String> retrievedBatch = manager.getFavorites();
        long getDurationMs = (System.nanoTime() - startGet) / 1_000_000;
        check(getDurationMs < 50, "getFavorites() took too long: " + getDurationMs + "ms");

        // 4. Reorder: reverse
        List<String> reversed = new ArrayList<>(list500);
        Collections.reverse(reversed);
        long startReorder = System.nanoTime();
        manager.setFavorites(reversed);
        long reorderDurationMs = (System.nanoTime() - startReorder) / 1_000_000;
        check(reorderDurationMs < 100, "Reordering 500 items took too long: " + reorderDurationMs + "ms");

        List<String> retrievedReversed = manager.getFavorites();
        check(retrievedReversed.equals(reversed), "Reordered list did not match expected reversed order!");

        // 5. Measure serialized string length in bytes
        SharedPreferences sp = context.getSharedPreferences("test_scale", 0);
        String rawJson = sp.getString("pref_favorites", "");
        byte[] bytes = rawJson.getBytes(StandardCharsets.UTF_8);
        int jsonKiloBytes = bytes.length / 1024;
        check(bytes.length < 50 * 1024, "JSON size exceeds 50 KB: " + bytes.length + " bytes");

        // 6. Measure heap delta
        System.gc();
        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memDeltaBytes = Math.abs(memAfter - memBefore);
        long memDeltaKb = memDeltaBytes / 1024;

        System.out.printf("   [METRICS] 500 items: Add=%dms, Set=%dms, Get=%dms, Reorder=%dms, JSON=%d KB, HeapDelta=%d KB%n",
                addDurationMs, setDurationMs, getDurationMs, reorderDurationMs, jsonKiloBytes, memDeltaKb);

        // Strict memory limit: heap delta must be under 3 MB (well below 30 MB launcher ceiling)
        check(memDeltaBytes < 3 * 1024 * 1024, "Heap delta exceeded 3 MB: " + memDeltaBytes + " bytes");
    }

    public static void testAdversarialPayloads() {
        TestContext context = new TestContext();
        PreferencesManager manager = new PreferencesManager(context, "test_adversarial", true);

        List<String> payloads = Arrays.asList(
                "com.test.quote\"app",
                "com.test.backslash\\app",
                "com.test.slash/app",
                "com.test.payload[\"fake\", \"injection\"]",
                "com.test.json{\"key\":\"value\"}",
                "com.test.newline\napp",
                "com.test.unicode.🚀.tv",
                "com.test.spaces in name",
                "com.test.special!@#$%^&*()_+~`|}{[]:;?><,.",
                "   leading_and_trailing_spaces   "
        );

        for (String payload : payloads) {
            manager.addFavorite(payload);
            manager.setAppHidden(payload, true);
        }

        List<String> favorites = manager.getFavorites();
        Set<String> hidden = manager.getHiddenApps();

        for (String payload : payloads) {
            check(manager.isFavorite(payload), "Payload failed isFavorite: " + payload);
            check(manager.isAppHidden(payload), "Payload failed isAppHidden: " + payload);
        }

        check(favorites.equals(payloads), "Favorites list did not preserve exact string payloads and order!");
    }

    public static void testMultiInstanceChallenge() throws Exception {
        TestContext context = new TestContext();
        String prefsName = "test_multi_instance";

        PreferencesManager mgr1 = new PreferencesManager(context, prefsName, true);
        PreferencesManager mgr2 = new PreferencesManager(context, prefsName, true);

        // Test concurrent writes from two separate manager instances on the same file
        int items = 30;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        executor.execute(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < items; i++) {
                    mgr1.addFavorite("com.inst1.app" + i);
                }
            } catch (Throwable e) {
                errors.add(e);
            } finally {
                doneLatch.countDown();
            }
        });

        executor.execute(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < items; i++) {
                    mgr2.addFavorite("com.inst2.app" + i);
                }
            } catch (Throwable e) {
                errors.add(e);
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        check(errors.isEmpty(), "Exceptions during multi-instance access: " + errors);

        // Fresh instance reading result
        PreferencesManager fresh = new PreferencesManager(context, prefsName, true);
        List<String> finalFavs = fresh.getFavorites();

        long count1 = finalFavs.stream().filter(s -> s.startsWith("com.inst1")).count();
        long count2 = finalFavs.stream().filter(s -> s.startsWith("com.inst2")).count();

        System.out.printf("   [METRICS] Multi-Instance Concurrency: Instance1 saved %d/%d, Instance2 saved %d/%d (Total=%d)%n",
                count1, items, count2, items, finalFavs.size());

        // Note: Because PreferencesManager uses instance-level `lock = Any()`, cross-instance RMW can interleave
        // This is a valuable empirical finding to document in handoff!
    }

    public static void testCorruptedJsonFaultTolerance() {
        TestContext context = new TestContext();
        String prefsName = "test_corrupt";
        SharedPreferences rawPrefs = context.getSharedPreferences(prefsName, 0);

        // Case 1: Corrupted malformed JSON syntax
        rawPrefs.edit().putString("pref_favorites", "{not an array: true").commit();
        PreferencesManager manager = new PreferencesManager(context, prefsName, true);
        check(manager.getFavorites().isEmpty(), "Corrupted JSON should return empty list");

        // Subsequent write must succeed and recover
        manager.addFavorite("com.recovered.app");
        check(manager.getFavorites().size() == 1, "Failed to recover after corrupted JSON");
        check(manager.getFavorites().get(0).equals("com.recovered.app"), "Recovered item mismatch");

        // Case 2: Truncated JSON
        rawPrefs.edit().putString("pref_favorites", "[\"app1\", \"app2\"").commit();
        check(manager.getFavorites().isEmpty(), "Truncated JSON should return empty list");

        // Case 3: Empty string and whitespace
        rawPrefs.edit().putString("pref_favorites", "   ").commit();
        check(manager.getFavorites().isEmpty(), "Whitespace JSON should return empty list");

        // Case 4: Array containing null and non-string elements
        rawPrefs.edit().putString("pref_favorites", "[\"valid.app\", null, 123, \"another.app\"]").commit();
        List<String> recovered = manager.getFavorites();
        check(recovered.contains("valid.app"), "Valid string should be preserved");
        check(recovered.contains("another.app"), "Second valid string should be preserved");
    }
}
