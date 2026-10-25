package com.minimal.tvlauncher.manager;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

/**
 * Empirical Adversarial Harness for Milestone 1 Persistence Layer.
 *
 * Implements strict ClassCastException throwing mock SharedPreferences to simulate
 * exact Android OS SharedPreferencesImpl behavior under storage corruption.
 */
public class AdversarialHarness {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("Starting Empirical Adversarial Harness for Milestone 1");
        System.out.println("=============================================================");

        // Section 1: Storage Corruption Attacks
        testCorruptedFavoritesNonJson();
        testCorruptedFavoritesTruncatedArrays();
        testCorruptedFavoritesSubsequentCleanWrites();
        testClassCastExceptionsOnAllKeys();
        testCorruptedIconShapesInStorageAndSetter();

        // Section 2: Extreme Boundary Inputs
        testGridColumnsExtremeValuesViaSetter();
        testGridColumnsExtremeValuesViaStorage();
        testPackageNamesBlankAndWhitespace();
        testPackageNamesSetFavoritesFilterBlanks();

        // Section 3: Re-instantiation & Reset
        testReinstantiationAndClear();

        System.out.println("=============================================================");
        System.out.println(String.format("Adversarial Results: %d run, %d passed, %d failed", testsRun, testsPassed, testsFailed));
        System.out.println("=============================================================");

        if (testsFailed > 0) {
            System.err.println("FAILURES DETECTED:");
            for (String f : failures) {
                System.err.println(" - " + f);
            }
            System.exit(1);
        } else {
            System.out.println("ALL EMPIRICAL ADVERSARIAL STRESS TESTS PASSED!");
            System.exit(0);
        }
    }

    private static void check(boolean condition, String testName, String message) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + testName);
        } else {
            testsFailed++;
            String failMsg = testName + ": " + message;
            failures.add(failMsg);
            System.err.println("  [FAIL] " + failMsg);
        }
    }

    // =========================================================================
    // 1. Storage Corruption: Non-JSON and Truncated JSON
    // =========================================================================

    private static void testCorruptedFavoritesNonJson() {
        System.out.println("\n--- Testing Corrupted Favorites: Non-JSON Strings ---");
        String[] malformed = {
            "this is not json",
            "{",
            "}",
            "[",
            "]",
            "[{",
            "[com.app.foo,",
            "null",
            "[null]",
            "undefined",
            "true",
            "false",
            "12345",
            "\"just_a_string\"",
            "{\"key\":\"value\"}",
            "<<XML></XML>>",
            "\u0000\u0001binary\uffff"
        };

        for (String payload : malformed) {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_FAVORITES, payload);
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);

            List<String> favs = manager.getFavorites();
            check(favs != null && favs.isEmpty(), "corruptedFavorites_payload: " + escape(payload),
                  "Expected empty list but got: " + favs);
        }
    }

    private static void testCorruptedFavoritesTruncatedArrays() {
        System.out.println("\n--- Testing Corrupted Favorites: Truncated JSON Arrays ---");
        String[] truncated = {
            "[",
            "[\"com.netflix.ninja\"",
            "[\"com.netflix.ninja\",",
            "[\"com.netflix.ninja\", \"com.youtube",
            "[\"com.netflix.ninja\", \"com.youtube\"] trailing garbage"
        };

        for (String payload : truncated) {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_FAVORITES, payload);
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);

            List<String> favs = manager.getFavorites();
            check(favs != null && favs.isEmpty(), "corruptedFavorites_truncated: " + escape(payload),
                  "Expected empty list but got: " + favs);
        }
    }

    private static void testCorruptedFavoritesSubsequentCleanWrites() {
        System.out.println("\n--- Testing Clean Writes Recovery After Corruption ---");
        MockSharedPreferences mockPrefs = new MockSharedPreferences();
        mockPrefs.storage.put(PreferencesManager.PREF_FAVORITES, "[\"broken_half_payload");
        MockContext context = new MockContext(mockPrefs);
        PreferencesManager manager = new PreferencesManager(context);
        manager.isSyncMode = true;

        check(manager.getFavorites().isEmpty(), "corruptedFavorites_initialReadEmpty", "Must be empty initially");

        boolean addSuccess = manager.addFavorite("com.app.recovered", true);
        check(addSuccess, "corruptedFavorites_addFavoriteSuccess", "Clean write must succeed");
        check(manager.isFavorite("com.app.recovered"), "corruptedFavorites_isFavoriteTrue", "isFavorite must be true");

        List<String> favs = manager.getFavorites();
        check(favs.size() == 1 && "com.app.recovered".equals(favs.get(0)),
              "corruptedFavorites_persistedCleanly", "Must contain only recovered app, got: " + favs);

        manager.addFavorite("com.app.second", true);
        favs = manager.getFavorites();
        check(favs.size() == 2 && "com.app.recovered".equals(favs.get(0)) && "com.app.second".equals(favs.get(1)),
              "corruptedFavorites_secondAppAppendedInOrder", "Must preserve insertion order: " + favs);
    }

    // =========================================================================
    // 2. ClassCastException Triggers Across All Keys
    // =========================================================================

    private static void testClassCastExceptionsOnAllKeys() {
        System.out.println("\n--- Testing ClassCastException Absorption & Overwrite on All Keys ---");

        // Key: pref_favorites (Int instead of String)
        {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_FAVORITES, 99999);
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);
            manager.isSyncMode = true;

            check(manager.getFavorites().isEmpty(), "cce_prefFavorites_int_absorbs", "Should return empty list");
            manager.addFavorite("com.recovered.after.cce", true);
            check(manager.getFavorites().contains("com.recovered.after.cce"), "cce_prefFavorites_int_subsequentWriteSuccess", "Subsequent write should overwrite cleanly");
        }

        // Key: pref_hidden_apps (Int instead of Set<String>)
        {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_HIDDEN_APPS, 12345);
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);
            manager.isSyncMode = true;

            check(manager.getHiddenApps().isEmpty(), "cce_prefHiddenApps_int_absorbs", "Should return empty set");
            check(!manager.isAppHidden("com.test"), "cce_prefHiddenApps_int_isAppHiddenFalse", "isAppHidden should be false");
            manager.setAppHidden("com.recovered.hidden", true, true);
            check(manager.isAppHidden("com.recovered.hidden"), "cce_prefHiddenApps_int_subsequentWriteSuccess", "Subsequent write should store set cleanly");
        }

        // Key: pref_hidden_apps (String instead of Set<String>)
        {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_HIDDEN_APPS, "not_a_set");
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);
            manager.isSyncMode = true;

            check(manager.getHiddenApps().isEmpty(), "cce_prefHiddenApps_string_absorbs", "Should return empty set");
        }

        // Key: pref_high_contrast (Int instead of Boolean)
        {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_HIGH_CONTRAST, 1);
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);
            manager.isSyncMode = true;

            check(!manager.isHighContrast(), "cce_prefHighContrast_int_absorbs", "Should return false default");
            manager.setHighContrast(true, true);
            check(manager.isHighContrast(), "cce_prefHighContrast_int_subsequentWriteSuccess", "Subsequent write should store Boolean cleanly");
        }

        // Key: pref_high_contrast (String instead of Boolean)
        {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_HIGH_CONTRAST, "true");
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);
            manager.isSyncMode = true;

            check(!manager.isHighContrast(), "cce_prefHighContrast_string_absorbs", "Should return false default");
        }

        // Key: pref_grid_columns (String instead of Int)
        {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_GRID_COLUMNS, "6");
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);
            manager.isSyncMode = true;

            check(manager.getGridColumns() == 6, "cce_prefGridColumns_string_absorbs", "Should return default 6");
            manager.setGridColumns(4, true);
            check(manager.getGridColumns() == 4, "cce_prefGridColumns_string_subsequentWriteSuccess", "Subsequent write should store Int cleanly");
        }

        // Key: pref_grid_columns (Boolean instead of Int)
        {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_GRID_COLUMNS, true);
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);
            manager.isSyncMode = true;

            check(manager.getGridColumns() == 6, "cce_prefGridColumns_boolean_absorbs", "Should return default 6");
        }

        // Key: pref_icon_shape (Int instead of String)
        {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_ICON_SHAPE, 100);
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);
            manager.isSyncMode = true;

            check("squircle".equals(manager.getIconShape()), "cce_prefIconShape_int_absorbs", "Should return default squircle");
            manager.setIconShape("circle", true);
            check("circle".equals(manager.getIconShape()), "cce_prefIconShape_int_subsequentWriteSuccess", "Subsequent write should store circle cleanly");
        }

        // Key: pref_icon_shape (Boolean instead of String)
        {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_ICON_SHAPE, false);
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);
            manager.isSyncMode = true;

            check("squircle".equals(manager.getIconShape()), "cce_prefIconShape_boolean_absorbs", "Should return default squircle");
        }

        // Key: pref_audio_feedback (Int instead of Boolean)
        {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_AUDIO_FEEDBACK, 0);
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);
            manager.isSyncMode = true;

            check(manager.isAudioFeedbackEnabled(), "cce_prefAudioFeedback_int_absorbs", "Should return default true");
            manager.setAudioFeedbackEnabled(false, true);
            check(!manager.isAudioFeedbackEnabled(), "cce_prefAudioFeedback_int_subsequentWriteSuccess", "Subsequent write should store false cleanly");
        }

        // Key: pref_audio_feedback (String instead of Boolean)
        {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_AUDIO_FEEDBACK, "false");
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);
            manager.isSyncMode = true;

            check(manager.isAudioFeedbackEnabled(), "cce_prefAudioFeedback_string_absorbs", "Should return default true");
        }
    }

    // =========================================================================
    // 3. Corrupted & Adversarial Icon Shapes
    // =========================================================================

    private static void testCorruptedIconShapesInStorageAndSetter() {
        System.out.println("\n--- Testing Corrupted Icon Shapes ---");
        String[] corrupted = {
            "🎉🚀👾🔥👑",
            "\u0000\u0007\r\n\t",
            "\u0080\u00ff\ufffe\uffff",
            "SQUIRCLE",
            "Squircle",
            "CIRCLE",
            "ROUNDED_RECT",
            "triangle",
            "hexagon",
            "diamond",
            "none",
            "null",
            "undefined",
            "0",
            "-1",
            "",
            "   ",
            "\t\n"
        };

        for (String shape : corrupted) {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_ICON_SHAPE, shape);
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);

            check("squircle".equals(manager.getIconShape()), "corruptedIconShape_storage: " + escape(shape),
                  "Expected squircle fallback but got: " + manager.getIconShape());
        }

        for (String shape : corrupted) {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);
            manager.isSyncMode = true;

            manager.setIconShape(shape, true);
            check("squircle".equals(manager.getIconShape()), "corruptedIconShape_setter: " + escape(shape),
                  "Expected squircle fallback but got: " + manager.getIconShape());
            check("squircle".equals(mockPrefs.storage.get(PreferencesManager.PREF_ICON_SHAPE)),
                  "corruptedIconShape_persistedNormalized: " + escape(shape), "Stored value must be squircle");
        }
    }

    // =========================================================================
    // 4. Extreme Boundary Values for Grid Columns
    // =========================================================================

    private static void testGridColumnsExtremeValuesViaSetter() {
        System.out.println("\n--- Testing Grid Columns Clamping via Setter ---");
        int[][] testCases = {
            { Integer.MIN_VALUE, 3 },
            { -999999, 3 },
            { -9999, 3 },
            { -1, 3 },
            { 0, 3 },
            { 1, 3 },
            { 2, 3 },
            { 3, 3 },
            { 4, 4 },
            { 5, 5 },
            { 6, 6 },
            { 7, 6 },
            { 8, 6 },
            { 9999, 6 },
            { 999999, 6 },
            { Integer.MAX_VALUE, 6 }
        };

        for (int[] pair : testCases) {
            int input = pair[0];
            int expected = pair[1];

            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);
            manager.isSyncMode = true;

            manager.setGridColumns(input, true);
            int actual = manager.getGridColumns();
            check(actual == expected && actual >= 3 && actual <= 6,
                  "gridColumns_setter_input_" + input,
                  "Expected " + expected + " within [3..6], but got: " + actual);
        }
    }

    private static void testGridColumnsExtremeValuesViaStorage() {
        System.out.println("\n--- Testing Grid Columns Clamping via Direct Storage Injection ---");
        int[][] testCases = {
            { Integer.MIN_VALUE, 3 },
            { -9999, 3 },
            { -1, 3 },
            { 0, 3 },
            { 1, 3 },
            { 2, 3 },
            { 7, 6 },
            { 9999, 6 },
            { Integer.MAX_VALUE, 6 }
        };

        for (int[] pair : testCases) {
            int rawStorage = pair[0];
            int expectedClamped = pair[1];

            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            mockPrefs.storage.put(PreferencesManager.PREF_GRID_COLUMNS, rawStorage);
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);

            int actual = manager.getGridColumns();
            check(actual == expectedClamped && actual >= 3 && actual <= 6,
                  "gridColumns_storage_raw_" + rawStorage,
                  "Expected " + expectedClamped + " within [3..6], but got: " + actual);
        }
    }

    // =========================================================================
    // 5. Blank, Whitespace & Null-like Package Names
    // =========================================================================

    private static void testPackageNamesBlankAndWhitespace() {
        System.out.println("\n--- Testing Blank & Whitespace Package Names ---");
        String[] blanks = { "", " ", "   ", "\t", "\n", "\r\n", " \t \n " };

        for (String blank : blanks) {
            MockSharedPreferences mockPrefs = new MockSharedPreferences();
            MockContext context = new MockContext(mockPrefs);
            PreferencesManager manager = new PreferencesManager(context);
            manager.isSyncMode = true;

            boolean addRes = manager.addFavorite(blank, true);
            check(!addRes, "addFavorite_blank: " + escape(blank), "Must return false");
            check(!manager.isFavorite(blank), "isFavorite_blank: " + escape(blank), "Must be false");

            boolean remRes = manager.removeFavorite(blank, true);
            check(!remRes, "removeFavorite_blank: " + escape(blank), "Must return false");

            boolean hideRes = manager.setAppHidden(blank, true, true);
            check(!hideRes, "setAppHidden_blank: " + escape(blank), "Must return false");
            check(!manager.isAppHidden(blank), "isAppHidden_blank: " + escape(blank), "Must be false");

            check(manager.getFavorites().isEmpty(), "favoritesEmptyAfterBlank: " + escape(blank), "Favorites must remain empty");
            check(manager.getHiddenApps().isEmpty(), "hiddenEmptyAfterBlank: " + escape(blank), "Hidden apps must remain empty");
        }
    }

    private static void testPackageNamesSetFavoritesFilterBlanks() {
        System.out.println("\n--- Testing setFavorites Filters Blank Elements ---");
        MockSharedPreferences mockPrefs = new MockSharedPreferences();
        MockContext context = new MockContext(mockPrefs);
        PreferencesManager manager = new PreferencesManager(context);
        manager.isSyncMode = true;

        List<String> mixed = Arrays.asList(
            "",
            "   ",
            "com.netflix.ninja",
            "\t",
            "com.google.android.youtube.tv",
            "\n",
            "com.amazon.amazonhdtv.android",
            "   \r\n   "
        );

        manager.setFavorites(mixed, true);
        List<String> favs = manager.getFavorites();

        check(favs.size() == 3, "setFavorites_filteredCount", "Expected 3 elements, got: " + favs.size());
        check("com.netflix.ninja".equals(favs.get(0)) &&
              "com.google.android.youtube.tv".equals(favs.get(1)) &&
              "com.amazon.amazonhdtv.android".equals(favs.get(2)),
              "setFavorites_filteredOrder", "Expected exact order preserved: " + favs);
    }

    // =========================================================================
    // 6. Reinstantiation & Clear
    // =========================================================================

    private static void testReinstantiationAndClear() {
        System.out.println("\n--- Testing Reinstantiation & Clear Reset ---");
        MockSharedPreferences mockPrefs = new MockSharedPreferences();
        MockContext context = new MockContext(mockPrefs);
        PreferencesManager m1 = new PreferencesManager(context);
        m1.isSyncMode = true;

        m1.addFavorite("com.app.1", true);
        m1.addFavorite("com.app.2", true);
        m1.setAppHidden("com.app.hidden", true, true);
        m1.setGridColumns(4, true);
        m1.setHighContrast(true, true);
        m1.setIconShape("circle", true);
        m1.setAudioFeedbackEnabled(false, true);

        // Instance 2 reading same storage
        PreferencesManager m2 = new PreferencesManager(context);
        check(m2.getFavorites().size() == 2 && "com.app.1".equals(m2.getFavorites().get(0)) && "com.app.2".equals(m2.getFavorites().get(1)),
              "reinstantiation_favoritesOrder", "Favorites must match exactly");
        check(m2.getHiddenApps().contains("com.app.hidden"), "reinstantiation_hiddenApps", "Hidden apps must match");
        check(m2.getGridColumns() == 4, "reinstantiation_gridColumns", "Grid columns must match 4");
        check(m2.isHighContrast(), "reinstantiation_highContrast", "High contrast must be true");
        check("circle".equals(m2.getIconShape()), "reinstantiation_iconShape", "Icon shape must be circle");
        check(!m2.isAudioFeedbackEnabled(), "reinstantiation_audioFeedback", "Audio feedback must be false");

        // Clear wipes back to defaults
        boolean clearSuccess = m1.clear(true);
        check(clearSuccess, "clear_success", "Clear must return true");

        PreferencesManager m3 = new PreferencesManager(context);
        check(m3.getFavorites().isEmpty(), "clear_favoritesEmpty", "Favorites empty after clear");
        check(m3.getHiddenApps().isEmpty(), "clear_hiddenEmpty", "Hidden apps empty after clear");
        check(m3.getGridColumns() == 6, "clear_gridDefault", "Grid columns reset to 6");
        check(!m3.isHighContrast(), "clear_highContrastDefault", "High contrast reset to false");
        check("squircle".equals(m3.getIconShape()), "clear_iconShapeDefault", "Icon shape reset to squircle");
        check(m3.isAudioFeedbackEnabled(), "clear_audioFeedbackDefault", "Audio feedback reset to true");
    }

    private static String escape(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else if (c < 32 || c >= 127) sb.append(String.format("\\u%04x", (int) c));
            else sb.append(c);
        }
        return sb.toString();
    }

    // =========================================================================
    // STRICT MOCK IMPLEMENTATION (Simulates Android OS SharedPreferencesImpl)
    // =========================================================================

    private static class MockContext extends Context {
        private final SharedPreferences prefs;

        MockContext(SharedPreferences prefs) {
            this.prefs = prefs;
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return prefs;
        }
    }

    private static class MockSharedPreferences implements SharedPreferences {
        final Map<String, Object> storage = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(storage);
        }

        @Override
        public String getString(String key, String defValue) {
            Object v = storage.get(key);
            if (v == null) return defValue;
            if (!(v instanceof String)) {
                // Exact Android SharedPreferencesImpl behavior!
                throw new ClassCastException("Value at " + key + " is of type " + v.getClass().getName() + " and cannot be cast to java.lang.String");
            }
            return (String) v;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object v = storage.get(key);
            if (v == null) return defValues;
            if (!(v instanceof Set)) {
                // Exact Android SharedPreferencesImpl behavior!
                throw new ClassCastException("Value at " + key + " is of type " + v.getClass().getName() + " and cannot be cast to java.util.Set");
            }
            return new HashSet<>((Set<String>) v);
        }

        @Override
        public int getInt(String key, int defValue) {
            Object v = storage.get(key);
            if (v == null) return defValue;
            if (!(v instanceof Integer)) {
                // Exact Android SharedPreferencesImpl behavior!
                throw new ClassCastException("Value at " + key + " is of type " + v.getClass().getName() + " and cannot be cast to java.lang.Integer");
            }
            return (Integer) v;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object v = storage.get(key);
            if (v == null) return defValue;
            if (!(v instanceof Long)) {
                throw new ClassCastException("Value at " + key + " is of type " + v.getClass().getName() + " and cannot be cast to java.lang.Long");
            }
            return (Long) v;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object v = storage.get(key);
            if (v == null) return defValue;
            if (!(v instanceof Float)) {
                throw new ClassCastException("Value at " + key + " is of type " + v.getClass().getName() + " and cannot be cast to java.lang.Float");
            }
            return (Float) v;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object v = storage.get(key);
            if (v == null) return defValue;
            if (!(v instanceof Boolean)) {
                // Exact Android SharedPreferencesImpl behavior!
                throw new ClassCastException("Value at " + key + " is of type " + v.getClass().getName() + " and cannot be cast to java.lang.Boolean");
            }
            return (Boolean) v;
        }

        @Override
        public boolean contains(String key) {
            return storage.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new MockEditor(this);
        }
    }

    private static class MockEditor implements SharedPreferences.Editor {
        private final MockSharedPreferences parent;
        private final Map<String, Object> modified = new HashMap<>();
        private final Set<String> removed = new HashSet<>();
        private boolean clear = false;

        MockEditor(MockSharedPreferences parent) {
            this.parent = parent;
        }

        @Override
        public SharedPreferences.Editor putString(String key, String value) {
            modified.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String key, Set<String> values) {
            modified.put(key, values != null ? new HashSet<>(values) : null);
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) {
            modified.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) {
            modified.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) {
            modified.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) {
            modified.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
            removed.add(key);
            return this;
        }

        @Override
        public SharedPreferences.Editor clear() {
            clear = true;
            return this;
        }

        @Override
        public boolean commit() {
            synchronized (parent.storage) {
                if (clear) {
                    parent.storage.clear();
                }
                for (String r : removed) {
                    parent.storage.remove(r);
                }
                parent.storage.putAll(modified);
            }
            return true;
        }

        @Override
        public void apply() {
            commit();
        }
    }
}
