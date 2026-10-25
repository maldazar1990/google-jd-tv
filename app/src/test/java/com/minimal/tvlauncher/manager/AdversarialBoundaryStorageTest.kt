package com.minimal.tvlauncher.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Adversarial boundary condition and storage corruption stress suite for [PreferencesManager].
 *
 * Explicitly challenges:
 * 1. Storage corruption: non-JSON, truncated JSON, ClassCastExceptions on all keys, invalid icon shapes.
 * 2. Extreme boundary inputs: Int.MIN_VALUE, -9999, 0, 1, 2, 5, 7, 9999, Int.MAX_VALUE for grid columns.
 * 3. Blank, whitespace, and null-like strings for package names across all methods.
 * 4. Fault tolerance & recovery: graceful defaults and subsequent clean writes following corruption.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdversarialBoundaryStorageTest {

    private lateinit var context: Context
    private lateinit var rawPrefs: SharedPreferences
    private lateinit var prefsManager: PreferencesManager

    companion object {
        private const val PREFS_NAME = "tv_launcher_prefs"
        private const val KEY_FAVORITES = "pref_favorites"
        private const val KEY_HIDDEN_APPS = "pref_hidden_apps"
        private const val KEY_GRID_COLUMNS = "pref_grid_columns"
        private const val KEY_HIGH_CONTRAST = "pref_high_contrast"
        private const val KEY_ICON_SHAPE = "pref_icon_shape"
        private const val KEY_AUDIO_FEEDBACK = "pref_audio_feedback"
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        rawPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        rawPrefs.edit().clear().commit()
        prefsManager = PreferencesManager(context, isSyncMode = true)
    }

    // =========================================================================
    // 1. STORAGE CORRUPTION ATTACKS: Malformed Data in SharedPreferences
    // =========================================================================

    @Test
    fun corruptedFavorites_nonJsonArbitraryStrings_absorbsAndReturnsEmpty() {
        val malformedStrings = listOf(
            "not a json string at all",
            "{",
            "}",
            "[",
            "]",
            "[{",
            "}{",
            "[com.app.foo,",
            "null",
            "undefined",
            "true",
            "false",
            "12345",
            "NaN",
            "\"just_a_quoted_string\"",
            "{\"key\": \"value\"}",
            "<<<XML><data/></XML>>>",
            "\u0000\u0001\u0002binary_blob\uffff"
        )

        for (malformed in malformedStrings) {
            rawPrefs.edit().putString(KEY_FAVORITES, malformed).commit()
            val result = prefsManager.getFavorites()
            assertThat(result).isNotNull()
            assertThat(result).isEmpty()
        }
    }

    @Test
    fun corruptedFavorites_truncatedJsonArrays_absorbsAndReturnsEmpty() {
        val truncatedPayloads = listOf(
            "[",
            "[\"com.app.one\"",
            "[\"com.app.one\",",
            "[\"com.app.one\", \"com.app.two",
            "[\"com.app.one\", \"com.app.two\"] trailing garbage",
            "[\"com.app.one\", {",
            "[[[\"nested\""
        )

        for (truncated in truncatedPayloads) {
            rawPrefs.edit().putString(KEY_FAVORITES, truncated).commit()
            val result = prefsManager.getFavorites()
            assertThat(result).isNotNull()
            com.google.common.truth.Truth.assertWithMessage("Failed on payload: " + truncated).that(result).isEmpty()
        }
    }

    @Test
    fun corruptedFavorites_arrayWithInvalidAndBlankElements_filtersCleanly() {
        // Elements with blank strings or non-string representations should not crash
        val payload = "[\"\", \"   \", \"\\t\\n\", \"com.valid.app\", \"  \"]"
        rawPrefs.edit().putString(KEY_FAVORITES, payload).commit()

        val result = prefsManager.getFavorites()
        assertThat(result).containsExactly("com.valid.app")
    }

    @Test
    fun corruptedFavorites_cleanRecoveryAndSubsequentWrites() {
        // Step 1: Corrupt with truncated array
        rawPrefs.edit().putString(KEY_FAVORITES, "[\"corrupted_app_half_written").commit()
        assertThat(prefsManager.getFavorites()).isEmpty()

        // Step 2: Perform clean write
        prefsManager.addFavorite("com.app.recovered")

        // Step 3: Verify clean write persisted
        assertThat(prefsManager.getFavorites()).containsExactly("com.app.recovered")
        assertThat(prefsManager.isFavorite("com.app.recovered")).isTrue()

        // Step 4: Add another favorite to verify list append integrity
        prefsManager.addFavorite("com.app.second")
        assertThat(prefsManager.getFavorites()).containsExactly("com.app.recovered", "com.app.second").inOrder()
    }

    // =========================================================================
    // 2. STORAGE CORRUPTION ATTACKS: ClassCastException Triggers on All Keys
    // =========================================================================

    @Test
    fun classCastException_prefFavorites_absorbsExceptionAndAllowsCleanWrite() {
        // Store an Integer where a String is expected
        rawPrefs.edit().putInt(KEY_FAVORITES, 12345).commit()

        // getFavorites should absorb ClassCastException gracefully
        assertThat(prefsManager.getFavorites()).isEmpty()

        // Subsequent write should succeed and restore String type
        prefsManager.addFavorite("com.clean.after.cast")
        assertThat(prefsManager.getFavorites()).containsExactly("com.clean.after.cast")

        // Store a Boolean where a String is expected
        rawPrefs.edit().putBoolean(KEY_FAVORITES, true).commit()
        assertThat(prefsManager.getFavorites()).isEmpty()

        // Subsequent setFavorites should overwrite cleanly
        prefsManager.setFavorites(listOf("com.new.app"))
        assertThat(prefsManager.getFavorites()).containsExactly("com.new.app")
    }

    @Test
    fun classCastException_prefHiddenApps_absorbsExceptionAndAllowsCleanWrite() {
        // Store an Integer where a Set<String> is expected
        rawPrefs.edit().putInt(KEY_HIDDEN_APPS, 999).commit()

        // getHiddenApps should absorb ClassCastException and return empty set
        assertThat(prefsManager.getHiddenApps()).isEmpty()
        assertThat(prefsManager.isAppHidden("com.test")).isFalse()

        // Subsequent write should succeed and overwrite with Set<String>
        prefsManager.setAppHidden("com.hidden.clean", true)
        assertThat(prefsManager.getHiddenApps()).containsExactly("com.hidden.clean")
        assertThat(prefsManager.isAppHidden("com.hidden.clean")).isTrue()

        // Store a String where a Set<String> is expected
        rawPrefs.edit().putString(KEY_HIDDEN_APPS, "not_a_set").commit()
        assertThat(prefsManager.getHiddenApps()).isEmpty()

        // Clean unhide of non-existent does not crash
        prefsManager.setAppHidden("com.not.hidden", false)
    }

    @Test
    fun classCastException_prefHighContrast_absorbsExceptionAndAllowsCleanWrite() {
        // Store an Integer where a Boolean is expected
        rawPrefs.edit().putInt(KEY_HIGH_CONTRAST, 1).commit()

        // isHighContrast should absorb ClassCastException and return default (false)
        assertThat(prefsManager.isHighContrast()).isFalse()

        // Subsequent write should succeed
        prefsManager.setHighContrast(true)
        assertThat(prefsManager.isHighContrast()).isTrue()

        // Store a String where a Boolean is expected
        rawPrefs.edit().putString(KEY_HIGH_CONTRAST, "true").commit()
        assertThat(prefsManager.isHighContrast()).isFalse()

        // Clean write restores Boolean
        prefsManager.setHighContrast(false)
        assertThat(prefsManager.isHighContrast()).isFalse()
    }

    @Test
    fun classCastException_prefGridColumns_absorbsExceptionAndAllowsCleanWrite() {
        // Store a String where an Int is expected
        rawPrefs.edit().putString(KEY_GRID_COLUMNS, "6").commit()

        // getGridColumns should absorb ClassCastException and return default (6)
        assertThat(prefsManager.getGridColumns()).isEqualTo(6)

        // Subsequent write should succeed and overwrite with Int
        prefsManager.setGridColumns(4)
        assertThat(prefsManager.getGridColumns()).isEqualTo(4)

        // Store a Boolean where an Int is expected
        rawPrefs.edit().putBoolean(KEY_GRID_COLUMNS, true).commit()
        assertThat(prefsManager.getGridColumns()).isEqualTo(6)

        // Clean write restores Int
        prefsManager.setGridColumns(3)
        assertThat(prefsManager.getGridColumns()).isEqualTo(3)
    }

    @Test
    fun classCastException_prefIconShape_absorbsExceptionAndAllowsCleanWrite() {
        // Store an Integer where a String is expected
        rawPrefs.edit().putInt(KEY_ICON_SHAPE, 42).commit()

        // getIconShape should absorb ClassCastException and return default ("squircle")
        assertThat(prefsManager.getIconShape()).isEqualTo("squircle")

        // Subsequent write should succeed and overwrite with String
        prefsManager.setIconShape("circle")
        assertThat(prefsManager.getIconShape()).isEqualTo("circle")

        // Store a Boolean where a String is expected
        rawPrefs.edit().putBoolean(KEY_ICON_SHAPE, false).commit()
        assertThat(prefsManager.getIconShape()).isEqualTo("squircle")

        // Clean write restores valid shape
        prefsManager.setIconShape("rounded_rect")
        assertThat(prefsManager.getIconShape()).isEqualTo("rounded_rect")
    }

    @Test
    fun classCastException_prefAudioFeedback_absorbsExceptionAndAllowsCleanWrite() {
        // Store an Integer where a Boolean is expected
        rawPrefs.edit().putInt(KEY_AUDIO_FEEDBACK, 0).commit()

        // isAudioFeedbackEnabled should absorb ClassCastException and return default (true)
        assertThat(prefsManager.isAudioFeedbackEnabled()).isTrue()

        // Subsequent write should succeed
        prefsManager.setAudioFeedbackEnabled(false)
        assertThat(prefsManager.isAudioFeedbackEnabled()).isFalse()

        // Store a String where a Boolean is expected
        rawPrefs.edit().putString(KEY_AUDIO_FEEDBACK, "false").commit()
        assertThat(prefsManager.isAudioFeedbackEnabled()).isTrue()

        // Clean write restores Boolean
        prefsManager.setAudioFeedbackEnabled(true)
        assertThat(prefsManager.isAudioFeedbackEnabled()).isTrue()
    }

    // =========================================================================
    // 3. STORAGE CORRUPTION ATTACKS: Corrupted & Adversarial Icon Shapes
    // =========================================================================

    @Test
    fun iconShape_corruptedStringsInStorage_allFallbackToSquircle() {
        val corruptedShapes = listOf(
            "🎉🚀👾🔥👑",                        // Emojis
            "\u0000\u0007\r\n\t",                   // Control characters
            "\u0080\u00ff\ufffe\uffff",             // Binary / non-printable
            "a".repeat(5000),                       // Oversized buffer
            "SQUIRCLE", "Squircle", "SqUiRcLe",     // Case sensitivity check
            "CIRCLE", "Circle",
            "ROUNDED_RECT", "Rounded_Rect",
            "triangle", "hexagon", "diamond", "star",
            "none", "null", "undefined", "0", "-1",
            "", " ", "   ", "\t\t\n"
        )

        for (corrupted in corruptedShapes) {
            rawPrefs.edit().putString(KEY_ICON_SHAPE, corrupted).commit()
            assertThat(prefsManager.getIconShape()).isEqualTo("squircle")
        }
    }

    @Test
    fun iconShape_adversarialInputViaSetter_normalizesToSquircle() {
        val adversarialInputs = listOf(
            "🎉🚀👾",
            "\u0000\t\n",
            "diamond",
            "HEXAGON",
            "SQUIRCLE",
            "",
            "   "
        )

        for (input in adversarialInputs) {
            prefsManager.setIconShape(input)
            // Stored and retrieved value must be safe default "squircle"
            assertThat(prefsManager.getIconShape()).isEqualTo("squircle")
            assertThat(rawPrefs.getString(KEY_ICON_SHAPE, null)).isEqualTo("squircle")
        }
    }

    // =========================================================================
    // 4. EXTREME BOUNDARY INPUTS: Grid Column Clamping
    // =========================================================================

    @Test
    fun gridColumns_extremeValuesViaSetter_strictlyClampedToThreeToSix() {
        val testCases = listOf(
            Int.MIN_VALUE to 3,
            -999999 to 3,
            -9999 to 3,
            -1 to 3,
            0 to 3,
            1 to 3,
            2 to 3,
            3 to 3,
            4 to 4,
            5 to 5,
            6 to 6,
            7 to 6,
            8 to 6,
            9999 to 6,
            999999 to 6,
            Int.MAX_VALUE to 6
        )

        for ((input, expected) in testCases) {
            prefsManager.setGridColumns(input)
            val actual = prefsManager.getGridColumns()
            assertThat(actual).isEqualTo(expected)
            assertThat(actual).isAtLeast(3)
            assertThat(actual).isAtMost(6)
        }
    }

    @Test
    fun gridColumns_corruptedOutofBoundsInStorage_strictlyClampedOnRead() {
        val rawValues = listOf(
            Int.MIN_VALUE to 3,
            -9999 to 3,
            -1 to 3,
            0 to 3,
            1 to 3,
            2 to 3,
            7 to 6,
            9999 to 6,
            Int.MAX_VALUE to 6
        )

        for ((rawInput, expectedClamped) in rawValues) {
            rawPrefs.edit().putInt(KEY_GRID_COLUMNS, rawInput).commit()
            val actual = prefsManager.getGridColumns()
            assertThat(actual).isEqualTo(expectedClamped)
            assertThat(actual).isAtLeast(3)
            assertThat(actual).isAtMost(6)
        }
    }

    // =========================================================================
    // 5. EXTREME BOUNDARY INPUTS: Blank, Whitespace & Null-like Package Names
    // =========================================================================

    @Test
    fun packageNames_blankAndWhitespace_rejectedByAllPersistenceMethods() {
        val blankInputs = listOf("", " ", "   ", "\t", "\n", "\r\n", " \t \n ")

        for (blank in blankInputs) {
            // 1. addFavorite should reject blank strings
            prefsManager.addFavorite(blank)
            assertThat(prefsManager.isFavorite(blank)).isFalse()

            // 2. removeFavorite should reject blank strings safely
            prefsManager.removeFavorite(blank)

            // 3. isFavorite should evaluate false
            assertThat(prefsManager.isFavorite(blank)).isFalse()

            // 4. setAppHidden should reject blank strings
            prefsManager.setAppHidden(blank, true)
            assertThat(prefsManager.isAppHidden(blank)).isFalse()

            prefsManager.setAppHidden(blank, false)
            assertThat(prefsManager.isAppHidden(blank)).isFalse()
        }

        // Favorites and hidden apps must remain completely empty
        assertThat(prefsManager.getFavorites()).isEmpty()
        assertThat(prefsManager.getHiddenApps()).isEmpty()
    }

    @Test
    fun setFavorites_listContainingBlanks_stripsBlanksPreservesValidEntries() {
        val mixedList = listOf(
            "",
            "   ",
            "com.netflix.ninja",
            "\t",
            "com.google.android.youtube.tv",
            "\n",
            "com.amazon.amazonhdtv.android",
            "   \r\n   "
        )

        prefsManager.setFavorites(mixedList)

        assertThat(prefsManager.getFavorites()).containsExactly(
            "com.netflix.ninja",
            "com.google.android.youtube.tv",
            "com.amazon.amazonhdtv.android"
        ).inOrder()
    }

    @Test
    fun packageNames_specialCharactersAndPunctuation_handledProperly() {
        // Valid package naming with numbers, underscores, and dots
        val packages = listOf(
            "com.app_1.v2",
            "tv.app.sub_channel",
            "org.test_123.sample"
        )

        for (pkg in packages) {
            prefsManager.addFavorite(pkg)
            assertThat(prefsManager.isFavorite(pkg)).isTrue()
            prefsManager.setAppHidden(pkg, true)
            assertThat(prefsManager.isAppHidden(pkg)).isTrue()
        }

        assertThat(prefsManager.getFavorites()).containsExactlyElementsIn(packages).inOrder()
        assertThat(prefsManager.getHiddenApps()).containsExactlyElementsIn(packages)
    }

    // =========================================================================
    // 6. CLEAR / RESET INTEGRITY
    // =========================================================================

    @Test
    fun clear_wipesAllKeysAndRestoresDefaults() {
        prefsManager.addFavorite("com.app.fav")
        prefsManager.setAppHidden("com.app.hidden", true)
        prefsManager.setGridColumns(4)
        prefsManager.setHighContrast(true)
        prefsManager.setIconShape("circle")
        prefsManager.setAudioFeedbackEnabled(false)

        val clearSuccess = prefsManager.clear()
        assertThat(clearSuccess).isTrue()

        // All should be back to defaults
        assertThat(prefsManager.getFavorites()).isEmpty()
        assertThat(prefsManager.getHiddenApps()).isEmpty()
        assertThat(prefsManager.getGridColumns()).isEqualTo(6)
        assertThat(prefsManager.isHighContrast()).isFalse()
        assertThat(prefsManager.getIconShape()).isEqualTo("squircle")
        assertThat(prefsManager.isAudioFeedbackEnabled()).isTrue()
    }
}
