package com.minimal.tvlauncher.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.google.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreferencesManagerTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var preferencesManager: PreferencesManager

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
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
        preferencesManager = PreferencesManager(context)
    }

    // ==========================================
    // 1. Default Values
    // ==========================================

    @Test
    fun defaultFavorites_isEmptyList() {
        assertThat(preferencesManager.getFavorites()).isEmpty()
    }

    @Test
    fun defaultHiddenApps_isEmptySet() {
        assertThat(preferencesManager.getHiddenApps()).isEmpty()
    }

    @Test
    fun defaultHighContrast_isFalse() {
        assertThat(preferencesManager.isHighContrast()).isFalse()
    }

    @Test
    fun defaultGridColumns_isSix() {
        assertThat(preferencesManager.getGridColumns()).isEqualTo(6)
    }

    @Test
    fun defaultIconShape_isSquircle() {
        assertThat(preferencesManager.getIconShape()).isEqualTo("squircle")
    }

    @Test
    fun defaultAudioFeedback_isTrue() {
        assertThat(preferencesManager.isAudioFeedbackEnabled()).isTrue()
    }

    // ==========================================
    // 2. Favorites Management & Insertion Order
    // ==========================================

    @Test
    fun addFavorite_singleApp_persistsAndReturnsTrueForIsFavorite() {
        val pkg = "com.google.android.youtube.tv"
        preferencesManager.addFavorite(pkg)

        assertThat(preferencesManager.isFavorite(pkg)).isTrue()
        assertThat(preferencesManager.getFavorites()).containsExactly(pkg)
    }

    @Test
    fun addFavorites_multipleApps_preservesExactInsertionOrder() {
        val app1 = "com.netflix.ninja"
        val app2 = "com.google.android.youtube.tv"
        val app3 = "com.amazon.amazonhdtv.android"

        preferencesManager.addFavorite(app1)
        preferencesManager.addFavorite(app2)
        preferencesManager.addFavorite(app3)

        assertThat(preferencesManager.getFavorites()).containsExactly(app1, app2, app3).inOrder()
    }

    @Test
    fun addFavorite_duplicateApp_doesNotCreateDuplicates() {
        val app1 = "com.netflix.ninja"
        val app2 = "com.google.android.youtube.tv"

        preferencesManager.addFavorite(app1)
        preferencesManager.addFavorite(app2)
        preferencesManager.addFavorite(app1) // duplicate

        val favorites = preferencesManager.getFavorites()
        assertThat(favorites).hasSize(2)
        assertThat(favorites.filter { it == app1 }).hasSize(1)
    }

    @Test
    fun removeFavorite_existingApp_removesAndPreservesRemainingOrder() {
        val app1 = "app.one"
        val app2 = "app.two"
        val app3 = "app.three"
        preferencesManager.addFavorite(app1)
        preferencesManager.addFavorite(app2)
        preferencesManager.addFavorite(app3)

        preferencesManager.removeFavorite(app2)

        assertThat(preferencesManager.isFavorite(app2)).isFalse()
        assertThat(preferencesManager.getFavorites()).containsExactly(app1, app3).inOrder()
    }

    @Test
    fun removeFavorite_nonExistentApp_isSafeNoOp() {
        preferencesManager.addFavorite("app.one")
        preferencesManager.removeFavorite("app.nonexistent")

        assertThat(preferencesManager.getFavorites()).containsExactly("app.one")
    }

    @Test
    fun removeFavorite_fromEmptyList_isSafeNoOp() {
        preferencesManager.removeFavorite("app.nonexistent")
        assertThat(preferencesManager.getFavorites()).isEmpty()
    }

    @Test
    fun setFavorites_overwritesEntireListInGivenOrder() {
        preferencesManager.addFavorite("old.app")
        val newList = listOf("new.app.c", "new.app.a", "new.app.b")

        preferencesManager.setFavorites(newList)

        assertThat(preferencesManager.getFavorites()).containsExactlyElementsIn(newList).inOrder()
        assertThat(preferencesManager.isFavorite("old.app")).isFalse()
    }

    @Test
    fun setFavorites_emptyList_clearsAllFavorites() {
        preferencesManager.addFavorite("app.one")
        preferencesManager.setFavorites(emptyList())

        assertThat(preferencesManager.getFavorites()).isEmpty()
        assertThat(preferencesManager.isFavorite("app.one")).isFalse()
    }

    @Test
    fun isFavorite_forUnfavoritedApp_returnsFalse() {
        assertThat(preferencesManager.isFavorite("com.unfavorited.app")).isFalse()
    }

    // ==========================================
    // 3. Hidden Apps Management
    // ==========================================

    @Test
    fun setAppHidden_true_addsToHiddenSet() {
        val pkg = "com.android.settings"
        preferencesManager.setAppHidden(pkg, true)

        assertThat(preferencesManager.isAppHidden(pkg)).isTrue()
        assertThat(preferencesManager.getHiddenApps()).contains(pkg)
    }

    @Test
    fun setAppHidden_false_removesFromHiddenSet() {
        val pkg = "com.android.settings"
        preferencesManager.setAppHidden(pkg, true)
        preferencesManager.setAppHidden(pkg, false)

        assertThat(preferencesManager.isAppHidden(pkg)).isFalse()
        assertThat(preferencesManager.getHiddenApps()).doesNotContain(pkg)
    }

    @Test
    fun setAppHidden_falseWhenNotHidden_isSafeNoOp() {
        preferencesManager.setAppHidden("com.not.hidden", false)
        assertThat(preferencesManager.getHiddenApps()).isEmpty()
    }

    @Test
    fun multipleHiddenApps_allPersistInSet() {
        preferencesManager.setAppHidden("app.a", true)
        preferencesManager.setAppHidden("app.b", true)
        preferencesManager.setAppHidden("app.c", true)

        assertThat(preferencesManager.getHiddenApps()).containsExactly("app.a", "app.b", "app.c")
    }

    // ==========================================
    // 4. Grid Columns & Bounds Clamping
    // ==========================================

    @Test
    fun setGridColumns_validValues_storeExact() {
        preferencesManager.setGridColumns(3)
        assertThat(preferencesManager.getGridColumns()).isEqualTo(3)

        preferencesManager.setGridColumns(4)
        assertThat(preferencesManager.getGridColumns()).isEqualTo(4)

        preferencesManager.setGridColumns(6)
        assertThat(preferencesManager.getGridColumns()).isEqualTo(6)
    }

    @Test
    fun setGridColumns_belowLowerBound_clampsToThree() {
        preferencesManager.setGridColumns(-1)
        assertThat(preferencesManager.getGridColumns()).isEqualTo(3)

        preferencesManager.setGridColumns(0)
        assertThat(preferencesManager.getGridColumns()).isEqualTo(3)

        preferencesManager.setGridColumns(2)
        assertThat(preferencesManager.getGridColumns()).isEqualTo(3)
    }

    @Test
    fun setGridColumns_aboveUpperBound_clampsToSix() {
        preferencesManager.setGridColumns(7)
        assertThat(preferencesManager.getGridColumns()).isEqualTo(6)

        preferencesManager.setGridColumns(100)
        assertThat(preferencesManager.getGridColumns()).isEqualTo(6)
    }

    // ==========================================
    // 5. High-Contrast Persistence
    // ==========================================

    @Test
    fun toggleHighContrast_persistsCorrectly() {
        preferencesManager.setHighContrast(true)
        assertThat(preferencesManager.isHighContrast()).isTrue()

        preferencesManager.setHighContrast(false)
        assertThat(preferencesManager.isHighContrast()).isFalse()
    }

    // ==========================================
    // 6. Icon Shape & Fallback
    // ==========================================

    @Test
    fun setIconShape_validShapes_storeCorrectly() {
        preferencesManager.setIconShape("circle")
        assertThat(preferencesManager.getIconShape()).isEqualTo("circle")

        preferencesManager.setIconShape("rounded_rect")
        assertThat(preferencesManager.getIconShape()).isEqualTo("rounded_rect")

        preferencesManager.setIconShape("squircle")
        assertThat(preferencesManager.getIconShape()).isEqualTo("squircle")
    }

    @Test
    fun setIconShape_invalidUnknownShape_fallsBackToSquircle() {
        preferencesManager.setIconShape("hexagon")
        assertThat(preferencesManager.getIconShape()).isEqualTo("squircle")
    }

    @Test
    fun setIconShape_emptyOrWhitespace_fallsBackToSquircle() {
        preferencesManager.setIconShape("")
        assertThat(preferencesManager.getIconShape()).isEqualTo("squircle")

        preferencesManager.setIconShape("   ")
        assertThat(preferencesManager.getIconShape()).isEqualTo("squircle")
    }

    @Test
    fun getIconShape_corruptedInSharedPreferences_fallsBackToSquircle() {
        sharedPreferences.edit().putString(KEY_ICON_SHAPE, "invalid_triangle").commit()
        assertThat(preferencesManager.getIconShape()).isEqualTo("squircle")
    }

    // ==========================================
    // 7. Audio Feedback Toggle
    // ==========================================

    @Test
    fun toggleAudioFeedback_persistsCorrectly() {
        preferencesManager.setAudioFeedbackEnabled(false)
        assertThat(preferencesManager.isAudioFeedbackEnabled()).isFalse()

        preferencesManager.setAudioFeedbackEnabled(true)
        assertThat(preferencesManager.isAudioFeedbackEnabled()).isTrue()
    }

    // ==========================================
    // 8. Corrupted SharedPreferences Recovery
    // ==========================================

    @Test
    fun corruptedFavorites_malformedJsonString_doesNotCrashAndReturnsEmptyList() {
        sharedPreferences.edit().putString(KEY_FAVORITES, "{not_a_valid_json_array").commit()

        val favorites = preferencesManager.getFavorites()
        assertThat(favorites).isNotNull()
        assertThat(favorites).isEmpty()
    }

    @Test
    fun corruptedFavorites_jsonObjectInsteadOfArray_returnsEmptyList() {
        sharedPreferences.edit().putString(KEY_FAVORITES, "{\"key\":\"value\"}").commit()

        assertThat(preferencesManager.getFavorites()).isEmpty()
    }

    @Test
    fun corruptedFavorites_recoveryAllowsSubsequentWrites() {
        sharedPreferences.edit().putString(KEY_FAVORITES, "corrupted_garbage_text").commit()

        preferencesManager.addFavorite("com.recovery.app")

        assertThat(preferencesManager.getFavorites()).containsExactly("com.recovery.app")
    }

    // ==========================================
    // 9. Re-instantiation / Reboot Persistence
    // ==========================================

    @Test
    fun reinstantiation_readsExactSavedValuesFromSamePreferences() {
        // Step 1: Write values via instance 1
        preferencesManager.addFavorite("com.fav.one")
        preferencesManager.addFavorite("com.fav.two")
        preferencesManager.setAppHidden("com.hidden.one", true)
        preferencesManager.setGridColumns(4)
        preferencesManager.setHighContrast(true)
        preferencesManager.setIconShape("circle")
        preferencesManager.setAudioFeedbackEnabled(false)

        // Step 2: Create a second independent instance backed by the same context / prefs
        val secondInstance = PreferencesManager(context)

        // Step 3: Assert all persisted values match
        assertThat(secondInstance.getFavorites()).containsExactly("com.fav.one", "com.fav.two").inOrder()
        assertThat(secondInstance.getHiddenApps()).containsExactly("com.hidden.one")
        assertThat(secondInstance.getGridColumns()).isEqualTo(4)
        assertThat(secondInstance.isHighContrast()).isTrue()
        assertThat(secondInstance.getIconShape()).isEqualTo("circle")
        assertThat(secondInstance.isAudioFeedbackEnabled()).isFalse()
    }
}
