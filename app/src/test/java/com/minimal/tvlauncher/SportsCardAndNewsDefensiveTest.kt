package com.minimal.tvlauncher

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.common.truth.Truth.assertThat
import com.minimal.tvlauncher.manager.PreferencesManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Defensive UI tests validating:
 * 1. Simultaneous coexistence of Google News ticker and Sports card widget.
 * 2. Strict focus immunity: Zero focus trap on D-pad navigation for both widgets.
 * 3. Dynamic hiding of widgets when drawer opens to maintain screen clarity.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class SportsCardAndNewsDefensiveTest {

    private lateinit var controller: ActivityController<MainActivity>
    private lateinit var activity: MainActivity
    private lateinit var prefsManager: PreferencesManager

    @Before
    fun setUp() {
        controller = Robolectric.buildActivity(MainActivity::class.java)
        activity = controller.setup().get()
        shadowOf(activity.mainLooper).idle()
        prefsManager = PreferencesManager(activity, isSyncMode = true)
    }

    // =========================================================================
    // 1. SIMULTANEOUS COEXISTENCE & VISIBILITY
    // =========================================================================

    @Test
    fun simultaneousVisibility_bothNewsAndSportsEnabled_coexistWithoutConflict() {
        prefsManager.setNewsWidgetEnabled(true)
        prefsManager.setSportsWidgetEnabled(true)

        val widgetNewsContainer = activity.findViewById<FrameLayout>(R.id.widget_news_container)
        val widgetSportsContainer = activity.findViewById<FrameLayout>(R.id.widget_sports_container)

        // Trigger refresh
        val refreshMethod = MainActivity::class.java.getDeclaredMethod("refreshWidgetsAsync")
        refreshMethod.isAccessible = true
        refreshMethod.invoke(activity)
        shadowOf(activity.mainLooper).idle()

        assertThat(widgetNewsContainer.visibility).isEqualTo(View.VISIBLE)
        assertThat(widgetSportsContainer.visibility).isEqualTo(View.VISIBLE)

        // Spatial layout check: News is square and positioned directly above Sports in top-right column
        val column = activity.findViewById<android.widget.LinearLayout>(R.id.dashboard_widgets_column)
        assertThat(column).isNotNull()
        val columnLp = column.layoutParams as FrameLayout.LayoutParams
        assertThat(columnLp.gravity and android.view.Gravity.TOP).isNotEqualTo(0)
        assertThat(columnLp.gravity and android.view.Gravity.END).isNotEqualTo(0)

        assertThat(column.indexOfChild(widgetNewsContainer)).isLessThan(column.indexOfChild(widgetSportsContainer))
    }

    @Test
    fun visibility_independentToggling_respectsIndividualPreferences() {
        val widgetNewsContainer = activity.findViewById<FrameLayout>(R.id.widget_news_container)
        val widgetSportsContainer = activity.findViewById<FrameLayout>(R.id.widget_sports_container)
        val refreshMethod = MainActivity::class.java.getDeclaredMethod("refreshWidgetsAsync").apply { isAccessible = true }

        // 1. Only Sports enabled
        prefsManager.setNewsWidgetEnabled(false)
        prefsManager.setSportsWidgetEnabled(true)
        refreshMethod.invoke(activity)
        shadowOf(activity.mainLooper).idle()
        assertThat(widgetNewsContainer.visibility).isEqualTo(View.GONE)
        assertThat(widgetSportsContainer.visibility).isEqualTo(View.VISIBLE)

        // 2. Only News enabled
        prefsManager.setNewsWidgetEnabled(true)
        prefsManager.setSportsWidgetEnabled(false)
        refreshMethod.invoke(activity)
        shadowOf(activity.mainLooper).idle()
        assertThat(widgetNewsContainer.visibility).isEqualTo(View.VISIBLE)
        assertThat(widgetSportsContainer.visibility).isEqualTo(View.GONE)

        // 3. Both disabled
        prefsManager.setNewsWidgetEnabled(false)
        prefsManager.setSportsWidgetEnabled(false)
        refreshMethod.invoke(activity)
        shadowOf(activity.mainLooper).idle()
        assertThat(widgetNewsContainer.visibility).isEqualTo(View.GONE)
        assertThat(widgetSportsContainer.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun drawerOpen_hidesBothWidgetsCleanly_toPreventVisualOcclusion() {
        prefsManager.setNewsWidgetEnabled(true)
        prefsManager.setSportsWidgetEnabled(true)

        val widgetNewsContainer = activity.findViewById<FrameLayout>(R.id.widget_news_container)
        val widgetSportsContainer = activity.findViewById<FrameLayout>(R.id.widget_sports_container)
        val openDrawerMethod = MainActivity::class.java.getDeclaredMethod("openDrawer").apply { isAccessible = true }
        val closeDrawerMethod = MainActivity::class.java.getDeclaredMethod("closeDrawer").apply { isAccessible = true }

        // When drawer opens, both widgets must be hidden (View.GONE)
        openDrawerMethod.invoke(activity)
        shadowOf(activity.mainLooper).idle()

        assertThat(widgetNewsContainer.visibility).isEqualTo(View.GONE)
        assertThat(widgetSportsContainer.visibility).isEqualTo(View.GONE)

        // When drawer closes, both widgets restore visibility
        closeDrawerMethod.invoke(activity)
        shadowOf(activity.mainLooper).idle()

        assertThat(widgetNewsContainer.visibility).isEqualTo(View.VISIBLE)
        assertThat(widgetSportsContainer.visibility).isEqualTo(View.VISIBLE)
    }

    // =========================================================================
    // 2. STRICT ZERO-FOCUS-TRAP GUARANTEE
    // =========================================================================

    @Test
    fun sportsWidget_isStrictlyNonFocusableAndNonClickable_noFocusTrap() {
        val widgetSportsContainer = activity.findViewById<FrameLayout>(R.id.widget_sports_container)
        val sportsCardRoot = activity.findViewById<View>(R.id.sports_card_view)
        val textSportsCardBadge = activity.findViewById<TextView>(R.id.text_sports_card_badge)
        val imageSportsHomeLogo = activity.findViewById<ImageView>(R.id.image_sports_home_logo)
        val textSportsHomeName = activity.findViewById<TextView>(R.id.text_sports_home_name)
        val textSportsHomeScore = activity.findViewById<TextView>(R.id.text_sports_home_score)
        val imageSportsAwayLogo = activity.findViewById<ImageView>(R.id.image_sports_away_logo)
        val textSportsAwayName = activity.findViewById<TextView>(R.id.text_sports_away_name)
        val textSportsAwayScore = activity.findViewById<TextView>(R.id.text_sports_away_score)
        val textSportsCardStatus = activity.findViewById<TextView>(R.id.text_sports_card_status)

        // Container integrity
        assertThat(widgetSportsContainer.isFocusable).isFalse()
        assertThat(widgetSportsContainer.isClickable).isFalse()
        assertThat(widgetSportsContainer.descendantFocusability).isEqualTo(ViewGroup.FOCUS_BLOCK_DESCENDANTS)

        // All child components inside the card must block focus
        val allChildViews = listOf(
            sportsCardRoot,
            textSportsCardBadge,
            imageSportsHomeLogo,
            textSportsHomeName,
            textSportsHomeScore,
            imageSportsAwayLogo,
            textSportsAwayName,
            textSportsAwayScore,
            textSportsCardStatus
        )

        for (v in allChildViews) {
            assertThat(v.isFocusable).isFalse()
            assertThat(v.isClickable).isFalse()
        }
    }

    @Test
    fun newsWidget_isStrictlyNonFocusableAndNonClickable() {
        val widgetNewsContainer = activity.findViewById<FrameLayout>(R.id.widget_news_container)
        val textWidgetNews = activity.findViewById<TextView>(R.id.text_widget_news)

        assertThat(widgetNewsContainer.isFocusable).isFalse()
        assertThat(widgetNewsContainer.isClickable).isFalse()
        assertThat(widgetNewsContainer.descendantFocusability).isEqualTo(ViewGroup.FOCUS_BLOCK_DESCENDANTS)

        assertThat(textWidgetNews.isFocusable).isFalse()
        assertThat(textWidgetNews.isClickable).isFalse()
        // Marquee animation requires isSelected = true
        assertThat(textWidgetNews.isSelected).isTrue()
    }

    @Test
    fun dpadNavigation_neverFocusesSportsOrNewsWidgets() {
        val widgetSportsContainer = activity.findViewById<FrameLayout>(R.id.widget_sports_container)
        val widgetNewsContainer = activity.findViewById<FrameLayout>(R.id.widget_news_container)
        val recyclerFavorites = activity.findViewById<RecyclerView>(R.id.recycler_favorites)

        // Request focus on dock
        recyclerFavorites.requestFocus()
        shadowOf(activity.mainLooper).idle()

        val keysToTest = listOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_LEFT
        )

        for (keyCode in keysToTest) {
            activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            shadowOf(activity.mainLooper).idle()

            // Verify neither widget ever captures focus
            assertThat(widgetSportsContainer.isFocused).isFalse()
            assertThat(widgetSportsContainer.hasFocus()).isFalse()
            assertThat(widgetNewsContainer.isFocused).isFalse()
            assertThat(widgetNewsContainer.hasFocus()).isFalse()
        }
    }
}
