package com.minimal.tvlauncher

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog
import com.minimal.tvlauncher.adapter.AppCardAdapter
import com.minimal.tvlauncher.model.AppModel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class MainActivityUiNavigationTest {

    private lateinit var controller: ActivityController<MainActivity>
    private lateinit var activity: MainActivity

    @Before
    fun setUp() {
        controller = Robolectric.buildActivity(MainActivity::class.java)
        activity = controller.setup().get()
        shadowOf(activity.mainLooper).idle()
    }

    @Test
    fun homeScreen_atRest_dockIsVisibleAndDrawerIsGone() {
        val dockShelf = activity.findViewById<FrameLayout>(R.id.dock_shelf)
        val drawerContainer = activity.findViewById<LinearLayout>(R.id.drawer_container)
        val controlCenterContainer = activity.findViewById<FrameLayout>(R.id.control_center_container)

        assertThat(dockShelf).isNotNull()
        assertThat(dockShelf.visibility).isEqualTo(View.VISIBLE)

        assertThat(drawerContainer).isNotNull()
        assertThat(drawerContainer.visibility).isEqualTo(View.GONE)

        assertThat(controlCenterContainer).isNotNull()
        assertThat(controlCenterContainer.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun settingsButton_click_opensControlCenter_andLocksBackgroundFocus() {
        val btnSettings = activity.findViewById<FrameLayout>(R.id.btn_settings)
        val controlCenterContainer = activity.findViewById<FrameLayout>(R.id.control_center_container)
        val recyclerFavorites = activity.findViewById<RecyclerView>(R.id.recycler_favorites)
        val recyclerDrawer = activity.findViewById<RecyclerView>(R.id.recycler_drawer)

        assertThat(controlCenterContainer.visibility).isEqualTo(View.GONE)
        assertThat(recyclerFavorites.descendantFocusability).isEqualTo(ViewGroup.FOCUS_AFTER_DESCENDANTS)

        btnSettings.performClick()
        shadowOf(activity.mainLooper).idle()

        assertThat(controlCenterContainer.visibility).isEqualTo(View.VISIBLE)

        // Modal focus lock on background apps
        assertThat(recyclerFavorites.descendantFocusability).isEqualTo(ViewGroup.FOCUS_BLOCK_DESCENDANTS)
        assertThat(recyclerDrawer.descendantFocusability).isEqualTo(ViewGroup.FOCUS_BLOCK_DESCENDANTS)

        val btnCcPower = activity.findViewById<View>(R.id.btn_cc_power)
        val btnCcWifi = activity.findViewById<View>(R.id.btn_cc_wifi)
        val btnCcRemoteBattery = activity.findViewById<View>(R.id.btn_cc_remote_battery)
        val btnCcAllSettings = activity.findViewById<View>(R.id.btn_cc_all_settings)
        val btnCcAccessibility = activity.findViewById<View>(R.id.btn_cc_accessibility)
        val btnCcClose = activity.findViewById<View>(R.id.btn_cc_close)

        assertThat(btnCcPower).isNotNull()
        assertThat(btnCcWifi).isNotNull()
        assertThat(btnCcRemoteBattery).isNotNull()
        assertThat(btnCcAllSettings).isNotNull()
        assertThat(btnCcAccessibility).isNotNull()
        assertThat(btnCcClose).isNotNull()

        // Focus given to first button (btnCcPower)
        assertThat(btnCcPower.isFocused).isTrue()

        // Check D-Pad chained navigation
        assertThat(btnCcPower.nextFocusDownId).isEqualTo(R.id.btn_cc_all_settings)
        assertThat(btnCcPower.nextFocusRightId).isEqualTo(R.id.btn_cc_wifi)
        assertThat(btnCcWifi.nextFocusDownId).isEqualTo(R.id.btn_cc_remote_battery)
        assertThat(btnCcWifi.nextFocusLeftId).isEqualTo(R.id.btn_cc_power)
        assertThat(btnCcRemoteBattery.nextFocusDownId).isEqualTo(R.id.btn_cc_close)
        assertThat(btnCcAllSettings.nextFocusRightId).isEqualTo(R.id.btn_cc_accessibility)
        assertThat(btnCcAccessibility.nextFocusRightId).isEqualTo(R.id.btn_cc_close)
    }

    @Test
    fun controlCenter_closeButton_closesControlCenter_andRestoresFocus() {
        val btnSettings = activity.findViewById<FrameLayout>(R.id.btn_settings)
        val controlCenterContainer = activity.findViewById<FrameLayout>(R.id.control_center_container)
        val recyclerFavorites = activity.findViewById<RecyclerView>(R.id.recycler_favorites)
        val recyclerDrawer = activity.findViewById<RecyclerView>(R.id.recycler_drawer)
        val btnCcClose = activity.findViewById<View>(R.id.btn_cc_close)

        btnSettings.performClick()
        shadowOf(activity.mainLooper).idle()
        assertThat(controlCenterContainer.visibility).isEqualTo(View.VISIBLE)
        assertThat(recyclerFavorites.descendantFocusability).isEqualTo(ViewGroup.FOCUS_BLOCK_DESCENDANTS)

        btnCcClose.performClick()
        shadowOf(activity.mainLooper).idle()

        assertThat(controlCenterContainer.visibility).isEqualTo(View.GONE)
        // Focus restored to apps and btnSettings focused
        assertThat(recyclerFavorites.descendantFocusability).isEqualTo(ViewGroup.FOCUS_AFTER_DESCENDANTS)
        assertThat(recyclerDrawer.descendantFocusability).isEqualTo(ViewGroup.FOCUS_AFTER_DESCENDANTS)
        assertThat(btnSettings.isFocused).isTrue()
    }

    @Test
    fun backKey_whenControlCenterOpen_closesControlCenterFirst_andRestoresFocus() {
        val btnSettings = activity.findViewById<FrameLayout>(R.id.btn_settings)
        val controlCenterContainer = activity.findViewById<FrameLayout>(R.id.control_center_container)
        val recyclerFavorites = activity.findViewById<RecyclerView>(R.id.recycler_favorites)

        btnSettings.performClick()
        shadowOf(activity.mainLooper).idle()
        assertThat(controlCenterContainer.visibility).isEqualTo(View.VISIBLE)

        val handled = activity.onKeyDown(KeyEvent.KEYCODE_BACK, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        shadowOf(activity.mainLooper).idle()

        assertThat(handled).isTrue()
        assertThat(controlCenterContainer.visibility).isEqualTo(View.GONE)
        assertThat(recyclerFavorites.descendantFocusability).isEqualTo(ViewGroup.FOCUS_AFTER_DESCENDANTS)
        assertThat(btnSettings.isFocused).isTrue()
    }

    @Test
    fun backKey_whenAtHomeOnDock_isConsumedWithoutFinishing() {
        val handled = activity.onKeyDown(KeyEvent.KEYCODE_BACK, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        assertThat(handled).isTrue()
        assertThat(activity.isFinishing).isFalse()
    }

    @Test
    fun controlCenter_allSettingsButton_closesControlCenterAndShowsDialog() {
        val btnSettings = activity.findViewById<FrameLayout>(R.id.btn_settings)
        val controlCenterContainer = activity.findViewById<FrameLayout>(R.id.control_center_container)
        val btnCcAllSettings = activity.findViewById<View>(R.id.btn_cc_all_settings)

        btnSettings.performClick()
        shadowOf(activity.mainLooper).idle()
        assertThat(controlCenterContainer.visibility).isEqualTo(View.VISIBLE)

        btnCcAllSettings.performClick()
        shadowOf(activity.mainLooper).idle()

        // Control Center must be closed cleanly
        assertThat(controlCenterContainer.visibility).isEqualTo(View.GONE)

        // Settings AlertDialog is shown
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertThat(dialog).isNotNull()
        assertThat(dialog.isShowing).isTrue()

        // Dismissing settings dialog restores interface cleanly without freeze
        dialog.dismiss()
        shadowOf(activity.mainLooper).idle()
        assertThat(btnSettings.isFocused).isTrue()
    }

    @Test
    fun dockAndDrawer_navigationAndAppLaunch() {
        val dockShelf = activity.findViewById<FrameLayout>(R.id.dock_shelf)
        val drawerContainer = activity.findViewById<LinearLayout>(R.id.drawer_container)
        val recyclerFavorites = activity.findViewById<RecyclerView>(R.id.recycler_favorites)
        val recyclerDrawer = activity.findViewById<RecyclerView>(R.id.recycler_drawer)

        val favAdapter = recyclerFavorites.adapter as AppCardAdapter
        val drwAdapter = recyclerDrawer.adapter as AppCardAdapter

        val testDockApps = listOf(
            AppModel(packageName = "com.netflix.ninja", activityName = "MainActivity", label = "Netflix", isLeanback = true, isFavorite = true),
            AppModel(packageName = "com.google.android.youtube.tv", activityName = "MainActivity", label = "YouTube", isLeanback = true, isFavorite = true)
        )
        val testDrawerApps = listOf(
            AppModel(packageName = "tv.twitch.android.app", activityName = "MainActivity", label = "Twitch", isLeanback = true, isFavorite = false),
            AppModel(packageName = "com.plexapp.android", activityName = "MainActivity", label = "Plex", isLeanback = true, isFavorite = false)
        )

        favAdapter.setApps(testDockApps)
        drwAdapter.setApps(testDrawerApps)
        shadowOf(activity.mainLooper).idle()

        recyclerFavorites.measure(1920, 300)
        recyclerFavorites.layout(0, 0, 1920, 300)
        recyclerDrawer.measure(1920, 800)
        recyclerDrawer.layout(0, 0, 1920, 800)

        assertThat(favAdapter.itemCount).isEqualTo(2)
        assertThat(drwAdapter.itemCount).isEqualTo(2)

        val dockHolder = recyclerFavorites.findViewHolderForAdapterPosition(0)
        assertThat(dockHolder).isNotNull()
        val dockCard = dockHolder!!.itemView.findViewById<View>(R.id.card_root)

        // App launch on click
        dockCard.performClick()
        shadowOf(activity.mainLooper).idle()
        // verify activity launched or handled safely
        assertThat(activity.isFinishing).isFalse()

        // Transition from dock to drawer via DPAD_DOWN
        dockCard.requestFocus()
        val handledDown = dockCard.dispatchKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN)
        )
        shadowOf(activity.mainLooper).idle()

        assertThat(handledDown).isTrue()
        assertThat(drawerContainer.visibility).isEqualTo(View.VISIBLE)

        // Return from drawer to dock via DPAD_UP
        val drawerHolder = recyclerDrawer.findViewHolderForAdapterPosition(0)
        assertThat(drawerHolder).isNotNull()
        val drawerCard = drawerHolder!!.itemView.findViewById<View>(R.id.card_root)
        drawerCard.requestFocus()
        val handledUp = drawerCard.dispatchKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP)
        )
        shadowOf(activity.mainLooper).idle()

        assertThat(handledUp).isTrue()
        assertThat(dockShelf.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun headerBar_hasNoWifiCapsule_andHasTopShelfBackdropLayer() {
        val netId = activity.resources.getIdentifier("widget_network_container", "id", activity.packageName)
        val imageTopShelfBackdrop = activity.findViewById<View>(R.id.image_top_shelf_backdrop)
        val imageWallpaper = activity.findViewById<View>(R.id.image_wallpaper)

        assertThat(netId).isEqualTo(0)
        assertThat(imageTopShelfBackdrop).isNotNull()
        assertThat(imageWallpaper).isNotNull()
    }
}
