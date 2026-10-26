package com.minimal.tvlauncher

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.minimal.tvlauncher.adapter.AppCardAdapter
import com.minimal.tvlauncher.config.AppConfig
import com.minimal.tvlauncher.manager.AppDiscoveryManager
import com.minimal.tvlauncher.manager.BingWallpaperManager
import com.minimal.tvlauncher.manager.BluetoothDeviceManager
import android.content.res.ColorStateList
import android.media.AudioManager
import com.minimal.tvlauncher.manager.DisplayResolutionManager
import com.minimal.tvlauncher.manager.IPreferencesManager
import com.minimal.tvlauncher.manager.NewsManager
import com.minimal.tvlauncher.manager.PreferencesManager
import com.minimal.tvlauncher.manager.SportsManager
import com.minimal.tvlauncher.manager.TopShelfManager
import com.minimal.tvlauncher.manager.WeatherManager
import com.minimal.tvlauncher.manager.WidgetManager
import com.minimal.tvlauncher.model.AppModel
import com.minimal.tvlauncher.platform.PlatformManager
import java.util.concurrent.Executors

class MainActivity : FragmentActivity() {

    companion object {
        const val DOCK_MAX_CAPACITY = AppConfig.Ui.DOCK_MAX_APPS
    }

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var discoveryManager: AppDiscoveryManager
    private lateinit var widgetManager: WidgetManager
    private lateinit var wallpaperManager: BingWallpaperManager
    private lateinit var newsManager: NewsManager
    private lateinit var weatherManager: WeatherManager
    private lateinit var sportsManager: SportsManager
    private lateinit var topShelfManager: TopShelfManager
    private lateinit var platformManager: PlatformManager
    private lateinit var displayResolutionManager: DisplayResolutionManager
    private lateinit var bluetoothDeviceManager: BluetoothDeviceManager

    private lateinit var mainRoot: FrameLayout
    private lateinit var imageWallpaper: ImageView
    private lateinit var imageTopShelfBackdrop: ImageView
    private lateinit var viewScrim: View

    private lateinit var dockShelf: FrameLayout
    private lateinit var recyclerFavorites: RecyclerView
    private lateinit var recyclerDrawer: RecyclerView
    private lateinit var drawerContainer: LinearLayout
    private lateinit var textDrawerTitle: TextView
    private lateinit var progressLoader: ProgressBar
    private lateinit var btnSettings: FrameLayout
    private lateinit var iconSettings: ImageView

    // Status Widgets
    private lateinit var widgetRamContainer: LinearLayout
    private lateinit var textWidgetRam: TextView
    private lateinit var widgetWeatherContainer: LinearLayout
    private lateinit var iconWidgetWeather: ImageView
    private lateinit var textWidgetWeather: TextView

    // News / Top Shelf
    private lateinit var widgetNewsContainer: FrameLayout
    private lateinit var textWidgetNews: TextView

    // Sports Square Card Widget
    private lateinit var widgetSportsContainer: FrameLayout
    private lateinit var textSportsCardBadge: TextView
    private lateinit var imageSportsHomeLogo: ImageView
    private lateinit var textSportsHomeName: TextView
    private lateinit var textSportsHomeScore: TextView
    private lateinit var imageSportsAwayLogo: ImageView
    private lateinit var textSportsAwayName: TextView
    private lateinit var textSportsAwayScore: TextView
    private lateinit var textSportsCardStatus: TextView

    // Control Center (Centre de Contrôle tvOS)
    private lateinit var controlCenterContainer: FrameLayout
    private lateinit var controlCenterPanel: LinearLayout
    private lateinit var btnCcPower: View
    private lateinit var btnCcCecPower: View
    private lateinit var btnCcWifi: View
    private lateinit var textCcWifiStatus: TextView
    private lateinit var btnCcRemoteBattery: View
    private lateinit var iconCcRemote: ImageView
    private lateinit var textCcRemoteTitle: TextView
    private lateinit var textCcRemoteStatus: TextView
    private lateinit var badgeCcRemoteLowBattery: View
    private lateinit var btnCcAllSettings: View
    private lateinit var btnCcAccessibility: View
    private lateinit var btnCcClose: View

    private var isDrawerOpen = false
    private var isControlCenterOpen = false
    private var lastDockFocusPosition = 0

    private lateinit var favoritesAdapter: AppCardAdapter
    private lateinit var drawerAdapter: AppCardAdapter
    private val backgroundExecutor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadAppsAsync()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferencesManager = PreferencesManager(this)
        discoveryManager = AppDiscoveryManager(this, preferencesManager)
        widgetManager = WidgetManager(this)
        wallpaperManager = BingWallpaperManager(this)
        newsManager = NewsManager()
        weatherManager = WeatherManager()
        sportsManager = SportsManager()
        topShelfManager = TopShelfManager(this)
        platformManager = PlatformManager(preferencesManager.getPlatformOverride())
        displayResolutionManager = DisplayResolutionManager(this)
        bluetoothDeviceManager = BluetoothDeviceManager(this)

        ensureDefaultWidgetPreferences()
        initViews()
        setupRecyclerViews()
        setupListeners()
        setupControlCenter()
        registerPackageReceiver()
    }

    override fun onResume() {
        super.onResume()
        platformManager = PlatformManager(preferencesManager.getPlatformOverride())
        applyThemeAndPreferences()
        loadAppsAsync()
        refreshWidgetsAsync()
        refreshWallpaperAsync()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(packageChangeReceiver)
        } catch (_: Exception) {
        }
        backgroundExecutor.shutdown()
    }

    private fun ensureDefaultWidgetPreferences() {
        val sharedPrefs = getSharedPreferences(PreferencesManager.PREFS_NAME, Context.MODE_PRIVATE)
        if (!sharedPrefs.contains("pref_tvos_layout_initialized")) {
            sharedPrefs.edit()
                .putBoolean("pref_tvos_layout_initialized", true)
                .putBoolean(PreferencesManager.PREF_BING_WALLPAPER, true)
                .putBoolean(PreferencesManager.PREF_WIDGET_RAM, true)
                .putBoolean(PreferencesManager.PREF_WIDGET_NETWORK, true)
                .putBoolean(PreferencesManager.PREF_WIDGET_WEATHER, true)
                .putBoolean(PreferencesManager.PREF_WIDGET_NEWS, true)
                .apply()
        }
    }

    private fun initViews() {
        mainRoot = findViewById(R.id.main_root)
        imageWallpaper = findViewById(R.id.image_wallpaper)
        imageTopShelfBackdrop = findViewById(R.id.image_top_shelf_backdrop)
        viewScrim = findViewById(R.id.view_scrim)

        dockShelf = findViewById(R.id.dock_shelf)
        recyclerFavorites = findViewById(R.id.recycler_favorites)
        recyclerDrawer = findViewById(R.id.recycler_drawer)
        drawerContainer = findViewById(R.id.drawer_container)
        textDrawerTitle = findViewById(R.id.text_drawer_title)
        progressLoader = findViewById(R.id.progress_loader)
        btnSettings = findViewById(R.id.btn_settings)
        iconSettings = findViewById(R.id.icon_settings)

        widgetRamContainer = findViewById(R.id.widget_ram_container)
        textWidgetRam = findViewById(R.id.text_widget_ram)
        widgetWeatherContainer = findViewById(R.id.widget_weather_container)
        iconWidgetWeather = findViewById(R.id.icon_widget_weather)
        textWidgetWeather = findViewById(R.id.text_widget_weather)

        widgetNewsContainer = findViewById(R.id.widget_news_container)
        textWidgetNews = findViewById(R.id.text_widget_news)
        textWidgetNews.isSelected = true

        widgetSportsContainer = findViewById(R.id.widget_sports_container)
        textSportsCardBadge = findViewById(R.id.text_sports_card_badge)
        imageSportsHomeLogo = findViewById(R.id.image_sports_home_logo)
        textSportsHomeName = findViewById(R.id.text_sports_home_name)
        textSportsHomeScore = findViewById(R.id.text_sports_home_score)
        imageSportsAwayLogo = findViewById(R.id.image_sports_away_logo)
        textSportsAwayName = findViewById(R.id.text_sports_away_name)
        textSportsAwayScore = findViewById(R.id.text_sports_away_score)
        textSportsCardStatus = findViewById(R.id.text_sports_card_status)

        // Control Center
        controlCenterContainer = findViewById(R.id.control_center_container)
        controlCenterPanel = findViewById(R.id.control_center_panel)
        btnCcPower = findViewById(R.id.btn_cc_power)
        btnCcCecPower = findViewById(R.id.btn_cc_cec_power)
        btnCcWifi = findViewById(R.id.btn_cc_wifi)
        textCcWifiStatus = findViewById(R.id.text_cc_wifi_status)
        btnCcRemoteBattery = findViewById(R.id.btn_cc_remote_battery)
        iconCcRemote = findViewById(R.id.icon_cc_remote)
        textCcRemoteTitle = findViewById(R.id.text_cc_remote_title)
        textCcRemoteStatus = findViewById(R.id.text_cc_remote_status)
        badgeCcRemoteLowBattery = findViewById(R.id.badge_cc_remote_low_battery)
        btnCcAllSettings = findViewById(R.id.btn_cc_all_settings)
        btnCcAccessibility = findViewById(R.id.btn_cc_accessibility)
        btnCcClose = findViewById(R.id.btn_cc_close)
    }

    private fun setupRecyclerViews() {
        recyclerFavorites.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val columns = preferencesManager.getDrawerColumns()
        recyclerDrawer.layoutManager = GridLayoutManager(this, columns)

        favoritesAdapter = AppCardAdapter(
            preferencesManager = preferencesManager,
            onAppClick = { app -> discoveryManager.launchApp(app) },
            onAppLongClick = { app, _ -> showAppContextMenu(app) },
            onAppFocus = { app, hasFocus ->
                if (hasFocus) {
                    val pos = recyclerFavorites.getChildAdapterPosition(recyclerFavorites.findFocus() ?: return@AppCardAdapter)
                    if (pos != RecyclerView.NO_POSITION) {
                        lastDockFocusPosition = pos
                    }
                }
                handleTopShelfHover(app.packageName, hasFocus)
            },
            nextFocusDownId = R.id.recycler_drawer,
            nextFocusUpId = R.id.btn_settings,
            onDpadKeyDown = { position, keyCode ->
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        lastDockFocusPosition = position
                        openDrawer()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        btnSettings.requestFocus()
                        true
                    }
                    else -> false
                }
            }
        )
        recyclerFavorites.adapter = favoritesAdapter

        drawerAdapter = AppCardAdapter(
            preferencesManager = preferencesManager,
            onAppClick = { app -> discoveryManager.launchApp(app) },
            onAppLongClick = { app, _ -> showAppContextMenu(app) },
            onAppFocus = { app, hasFocus ->
                handleTopShelfHover(app.packageName, hasFocus)
            },
            nextFocusUpId = R.id.recycler_favorites,
            onDpadKeyDown = { position, keyCode ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    val cols = preferencesManager.getDrawerColumns()
                    if (position < cols) {
                        closeDrawer()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        )
        recyclerDrawer.adapter = drawerAdapter
    }

    private fun setupListeners() {
        // Settings button animation and halo
        btnSettings.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                if (preferencesManager.isAudioFeedbackEnabled()) {
                    v.playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT)
                }
                v.animate()
                    .scaleX(1.25f)
                    .scaleY(1.25f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator(1.4f))
                    .start()
                v.elevation = 18f
                iconSettings.setColorFilter(Color.parseColor("#121824"))
            } else {
                v.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
                v.elevation = 4f
                iconSettings.setColorFilter(Color.WHITE)
            }
        }

        btnSettings.setOnClickListener {
            if (preferencesManager.isAudioFeedbackEnabled()) {
                it.playSoundEffect(SoundEffectConstants.CLICK)
            }
            openControlCenter()
        }

        btnSettings.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                focusDockItem()
                true
            } else {
                false
            }
        }

        // Informational only: News ticker and Sports card are strictly non-focusable & non-clickable
        widgetNewsContainer.isFocusable = false
        widgetNewsContainer.isClickable = false
        widgetSportsContainer.isFocusable = false
        widgetSportsContainer.isClickable = false
    }

    private fun setupControlCenter() {
        controlCenterContainer.setOnClickListener {
            closeControlCenter()
        }

        val ccButtons = listOf(
            btnCcPower,
            btnCcWifi,
            btnCcRemoteBattery,
            btnCcAllSettings,
            btnCcAccessibility,
            btnCcClose
        )

        ccButtons.forEach { btn ->
            btn.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    if (preferencesManager.isAudioFeedbackEnabled()) {
                        v.playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT)
                    }
                    v.animate().scaleX(1.04f).scaleY(1.04f).setDuration(160).start()
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start()
                }
            }
        }

        // 1. Veille : mise en sommeil directe
        btnCcPower.setOnClickListener {
            if (preferencesManager.isAudioFeedbackEnabled()) it.playSoundEffect(SoundEffectConstants.CLICK)
            displayResolutionManager.sendDeviceToSleep(this)
        }

        // 1b. Éteindre TV : HDMI-CEC Standby physique
        btnCcCecPower.setOnClickListener {
            if (preferencesManager.isAudioFeedbackEnabled()) it.playSoundEffect(SoundEffectConstants.CLICK)
            displayResolutionManager.sendTvCecStandby(this)
        }

        // 2. Wi-Fi
        btnCcWifi.setOnClickListener {
            if (preferencesManager.isAudioFeedbackEnabled()) it.playSoundEffect(SoundEffectConstants.CLICK)
            displayResolutionManager.openWifiSettings(this)
        }

        // 3. Télécommande / Manette : Réglages Télécommandes & Accessoires
        btnCcRemoteBattery.setOnClickListener {
            if (preferencesManager.isAudioFeedbackEnabled()) it.playSoundEffect(SoundEffectConstants.CLICK)
            displayResolutionManager.openBluetoothSettings(this)
        }

        // 4. Tous les Réglages Zen TV
        btnCcAllSettings.setOnClickListener {
            if (preferencesManager.isAudioFeedbackEnabled()) it.playSoundEffect(SoundEffectConstants.CLICK)
            closeControlCenter()
            showSettingsDialog()
        }

        // 5. Accessibilité
        btnCcAccessibility.setOnClickListener {
            if (preferencesManager.isAudioFeedbackEnabled()) it.playSoundEffect(SoundEffectConstants.CLICK)
            closeControlCenter()
            showAccessibilityAndSystemDialog()
        }

        // 6. Fermer
        btnCcClose.setOnClickListener {
            if (preferencesManager.isAudioFeedbackEnabled()) it.playSoundEffect(SoundEffectConstants.CLICK)
            closeControlCenter()
        }
    }

    private fun getFormattedWifiStatus(): String {
        val details = displayResolutionManager.getConnectedWifiDetails(this)
        return details.formattedStatus
    }

    private fun handleTopShelfHover(packageName: String, hasFocus: Boolean) {
        if (!hasFocus) {
            imageTopShelfBackdrop.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction {
                    imageTopShelfBackdrop.setImageDrawable(null)
                }
                .start()
            return
        }

        if (!preferencesManager.isTopShelfEnabled()) return

        backgroundExecutor.execute {
            val item = topShelfManager.getTopShelfItemForPackage(packageName)
            val bitmap = item?.posterUri?.let { uri -> topShelfManager.loadPosterBitmap(uri) }
            mainHandler.post {
                if (item != null) {
                    val epText = item.subtitle?.let { " - $it" } ?: ""
                    textWidgetNews.text = "▶ ${item.title}$epText"
                    textWidgetNews.isSelected = true
                    widgetNewsContainer.visibility = View.VISIBLE

                    if (bitmap != null) {
                        imageTopShelfBackdrop.setImageBitmap(bitmap)
                        imageTopShelfBackdrop.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                    } else if (item.posterUri != null) {
                        try {
                            imageTopShelfBackdrop.setImageURI(item.posterUri)
                            imageTopShelfBackdrop.animate()
                                .alpha(1f)
                                .setDuration(300)
                                .start()
                        } catch (_: Exception) {
                            imageTopShelfBackdrop.animate().alpha(0f).setDuration(200).start()
                        }
                    } else {
                        imageTopShelfBackdrop.animate().alpha(0f).setDuration(200).start()
                    }
                } else {
                    imageTopShelfBackdrop.animate().alpha(0f).setDuration(200).start()
                }
            }
        }
    }

    private fun openControlCenter() {
        isControlCenterOpen = true

        // Verrouillage modal du focus à l'ouverture du Centre de Contrôle
        recyclerFavorites.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        recyclerDrawer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        controlCenterContainer.visibility = View.VISIBLE
        controlCenterContainer.alpha = 0f
        controlCenterPanel.translationX = 420f

        textCcWifiStatus.text = getFormattedWifiStatus()

        // Détection de la télécommande / manette et niveau de batterie
        val remoteInfo = bluetoothDeviceManager.getConnectedRemoteOrGamepadInfo()
        textCcRemoteTitle.text = remoteInfo.name
        textCcRemoteStatus.text = if (remoteInfo.batteryPercent != null) {
            "${remoteInfo.batteryPercent}%"
        } else {
            "Connectée"
        }
        badgeCcRemoteLowBattery.visibility = if (remoteInfo.isLowBattery) View.VISIBLE else View.GONE

        controlCenterContainer.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        controlCenterPanel.animate()
            .translationX(0f)
            .setDuration(240)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .withEndAction {
                btnCcPower.requestFocus()
            }
            .start()

        btnCcPower.requestFocus()
    }

    private fun closeControlCenter() {
        if (!isControlCenterOpen) return
        isControlCenterOpen = false

        // Rétablir l'accessibilité du focus pour les listes d'applications
        recyclerFavorites.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        recyclerDrawer.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

        controlCenterContainer.animate()
            .alpha(0f)
            .setDuration(180)
            .start()

        controlCenterPanel.animate()
            .translationX(420f)
            .setDuration(200)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                controlCenterContainer.visibility = View.GONE
                btnSettings.requestFocus()
            }
            .start()

        btnSettings.requestFocus()
    }

    private fun openDrawer() {
        if (isDrawerOpen) return
        isDrawerOpen = true
        widgetNewsContainer.visibility = View.GONE
        widgetSportsContainer.visibility = View.GONE

        dockShelf.animate()
            .translationY(140f)
            .alpha(0f)
            .setDuration(180)
            .withEndAction {
                dockShelf.visibility = View.GONE
            }
            .start()

        drawerContainer.visibility = View.VISIBLE
        drawerContainer.alpha = 0f
        drawerContainer.translationY = 60f
        drawerContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                focusDrawerFirstItem()
            }
            .start()

        focusDrawerFirstItem()
    }

    private fun closeDrawer() {
        if (!isDrawerOpen) return
        isDrawerOpen = false

        drawerContainer.animate()
            .alpha(0f)
            .translationY(60f)
            .setDuration(180)
            .withEndAction {
                drawerContainer.visibility = View.GONE
            }
            .start()

        dockShelf.visibility = View.VISIBLE
        dockShelf.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                focusDockItem()
            }
            .start()

        if (preferencesManager.isNewsWidgetEnabled()) {
            widgetNewsContainer.visibility = View.VISIBLE
        }
        if (preferencesManager.isSportsWidgetEnabled()) {
            widgetSportsContainer.visibility = View.VISIBLE
        }

        focusDockItem()
    }

    private fun focusDrawerFirstItem() {
        recyclerDrawer.post {
            val holder = recyclerDrawer.findViewHolderForAdapterPosition(0)
            if (holder != null) {
                holder.itemView.requestFocus()
            } else {
                recyclerDrawer.scrollToPosition(0)
                recyclerDrawer.postDelayed({
                    recyclerDrawer.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                }, 60)
            }
        }
    }

    private fun focusDockItem() {
        recyclerFavorites.post {
            val targetPos = lastDockFocusPosition.coerceIn(0, (favoritesAdapter.itemCount - 1).coerceAtLeast(0))
            val holder = recyclerFavorites.findViewHolderForAdapterPosition(targetPos)
            if (holder != null) {
                holder.itemView.requestFocus()
            } else {
                recyclerFavorites.scrollToPosition(targetPos)
                recyclerFavorites.postDelayed({
                    recyclerFavorites.findViewHolderForAdapterPosition(targetPos)?.itemView?.requestFocus()
                }, 60)
            }
        }
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageChangeReceiver, filter)
    }

    private fun loadAppsAsync() {
        progressLoader.visibility = View.VISIBLE
        backgroundExecutor.execute {
            val allApps = discoveryManager.loadInstalledApps(includeHidden = false)
            val favoritePackageNames = preferencesManager.getFavorites()

            val explicitFavorites = favoritePackageNames.mapNotNull { pkg ->
                allApps.find { it.packageName == pkg }
            }

            val dockApps = mutableListOf<AppModel>()
            dockApps.addAll(explicitFavorites.take(DOCK_MAX_CAPACITY))

            if (dockApps.size < DOCK_MAX_CAPACITY) {
                val remainingSlots = DOCK_MAX_CAPACITY - dockApps.size
                val fillers = allApps.filter { app ->
                    !dockApps.any { it.packageName == app.packageName }
                }.take(remainingSlots)
                dockApps.addAll(fillers)
            }

            val drawerApps = allApps.filter { app ->
                !dockApps.any { it.packageName == app.packageName }
            }

            mainHandler.post {
                favoritesAdapter.setApps(dockApps)
                drawerAdapter.setApps(drawerApps)
                progressLoader.visibility = View.GONE

                if (!isDrawerOpen) {
                    drawerContainer.visibility = View.GONE
                    dockShelf.visibility = View.VISIBLE
                    dockShelf.translationY = 0f
                    dockShelf.alpha = 1f
                    focusDockItem()
                }
            }
        }
    }

    private fun refreshWidgetsAsync() {
        // 1. RAM widget
        val ramEnabled = preferencesManager.isRamWidgetEnabled()
        widgetRamContainer.visibility = if (ramEnabled) View.VISIBLE else View.GONE
        if (ramEnabled) {
            val ramText = widgetManager.formatRamFreeString()
            textWidgetRam.text = ramText
        }

        // Update Control Center Wi-Fi status
        textCcWifiStatus.text = getFormattedWifiStatus()

        // 3. Weather widget
        val weatherEnabled = preferencesManager.isWeatherWidgetEnabled()
        widgetWeatherContainer.visibility = if (weatherEnabled) View.VISIBLE else View.GONE

        // 4. News ticker marquee & Sports square card coexistence
        val newsEnabled = preferencesManager.isNewsWidgetEnabled()
        val sportsEnabled = preferencesManager.isSportsWidgetEnabled()

        widgetNewsContainer.visibility = if (newsEnabled && !isDrawerOpen) View.VISIBLE else View.GONE
        widgetSportsContainer.visibility = if (sportsEnabled && !isDrawerOpen) View.VISIBLE else View.GONE

        if (weatherEnabled || newsEnabled || sportsEnabled) {
            backgroundExecutor.execute {
                var weatherString: String? = null
                if (weatherEnabled) {
                    val isAuto = preferencesManager.isWeatherAutoLocation()
                    val lat: Double
                    val lon: Double
                    val cityName: String

                    if (isAuto) {
                        val geo = weatherManager.resolveIpLocation()
                        if (geo != null) {
                            lat = geo.latitude
                            lon = geo.longitude
                            cityName = geo.cityName
                            preferencesManager.setWeatherCoordinates(lat, lon)
                            preferencesManager.setWeatherCity(cityName)
                        } else {
                            lat = preferencesManager.getWeatherLatitude()
                            lon = preferencesManager.getWeatherLongitude()
                            cityName = preferencesManager.getWeatherCity()
                        }
                    } else {
                        val city = preferencesManager.getWeatherCity()
                        val geo = weatherManager.geocodeCity(city)
                        if (geo != null) {
                            lat = geo.latitude
                            lon = geo.longitude
                            cityName = geo.cityName
                            preferencesManager.setWeatherCoordinates(lat, lon)
                        } else {
                            lat = preferencesManager.getWeatherLatitude()
                            lon = preferencesManager.getWeatherLongitude()
                            cityName = city
                        }
                    }
                    weatherString = weatherManager.getWeatherText(lat, lon, cityName)
                }

                var newsText: String? = null
                if (newsEnabled) {
                    val topics = preferencesManager.getNewsTopics()
                    val sources = preferencesManager.getNewsSources()
                    val customKeyword = preferencesManager.getNewsCustomKeyword()
                    val legacyTopic = preferencesManager.getNewsTopic()
                    newsText = newsManager.getNewsTickerText(topics, sources, customKeyword, legacyTopic)
                }

                var sportsDetails: SportsManager.SportsMatchDetails? = null
                if (sportsEnabled) {
                    sportsDetails = sportsManager.getCurrentMatchDetails(
                        sport = preferencesManager.getSportsType(),
                        favoriteTeam = preferencesManager.getSportsFavoriteTeam()
                    )
                }

                mainHandler.post {
                    if (weatherEnabled && weatherString != null) {
                        textWidgetWeather.text = weatherString
                    }
                    if (newsEnabled && newsText != null) {
                        textWidgetNews.text = newsText.removePrefix("Google News : ").removePrefix("Google News: ").trim()
                        textWidgetNews.isSelected = true
                    }
                    if (sportsEnabled && sportsDetails != null) {
                        textSportsCardBadge.text = sportsDetails.sportBadge
                        imageSportsHomeLogo.setImageResource(sportsDetails.homeLogoRes)
                        textSportsHomeName.text = sportsDetails.homeTeam
                        textSportsHomeScore.text = sportsDetails.homeScore.toString()
                        imageSportsAwayLogo.setImageResource(sportsDetails.awayLogoRes)
                        textSportsAwayName.text = sportsDetails.awayTeam
                        textSportsAwayScore.text = sportsDetails.awayScore.toString()
                        textSportsCardStatus.text = sportsDetails.status
                    }
                }
            }
        }
    }

    private fun refreshWallpaperAsync() {
        val isHighContrast = preferencesManager.isHighContrast()
        if (isHighContrast) {
            imageWallpaper.visibility = View.GONE
            viewScrim.visibility = View.GONE
            mainRoot.setBackgroundColor(Color.BLACK)
            return
        }

        imageWallpaper.visibility = View.VISIBLE
        viewScrim.visibility = View.VISIBLE

        val source = preferencesManager.getWallpaperSource()

        when (source) {
            PreferencesManager.WALLPAPER_BING -> {
                backgroundExecutor.execute {
                    var bitmap = wallpaperManager.getCachedWallpaperBitmap()
                    if (bitmap == null) {
                        val downloaded = wallpaperManager.refreshWallpaperFromBing()
                        if (downloaded) {
                            bitmap = wallpaperManager.getCachedWallpaperBitmap()
                        }
                    }
                    mainHandler.post {
                        if (bitmap != null) {
                            imageWallpaper.setImageBitmap(bitmap)
                        } else {
                            imageWallpaper.setImageResource(R.drawable.bg_default_scenic)
                        }
                    }
                }
            }
            PreferencesManager.WALLPAPER_STORAGE -> {
                backgroundExecutor.execute {
                    val path = preferencesManager.getWallpaperStoragePath()
                    val bitmap = wallpaperManager.getBitmapFromStorage(path)
                    mainHandler.post {
                        if (bitmap != null) {
                            imageWallpaper.setImageBitmap(bitmap)
                        } else {
                            imageWallpaper.setImageResource(R.drawable.bg_default_scenic)
                        }
                    }
                }
            }
            PreferencesManager.WALLPAPER_GOOGLE_PHOTOS -> {
                backgroundExecutor.execute {
                    val url = preferencesManager.getGooglePhotosUrl()
                    val bitmap = wallpaperManager.getBitmapFromUrl(url)
                    mainHandler.post {
                        if (bitmap != null) {
                            imageWallpaper.setImageBitmap(bitmap)
                        } else {
                            imageWallpaper.setImageResource(R.drawable.bg_default_scenic)
                        }
                    }
                }
            }
            else -> {
                imageWallpaper.setImageResource(R.drawable.bg_default_scenic)
            }
        }
    }

    private fun applyThemeAndPreferences() {
        val isHighContrast = preferencesManager.isHighContrast()
        if (isHighContrast) {
            imageWallpaper.visibility = View.GONE
            viewScrim.visibility = View.GONE
            mainRoot.setBackgroundColor(Color.BLACK)
            btnSettings.setBackgroundResource(R.drawable.focus_border_high_contrast)
        } else {
            imageWallpaper.visibility = View.VISIBLE
            viewScrim.visibility = View.VISIBLE
            btnSettings.setBackgroundResource(R.drawable.bg_settings_pill_selector)
        }
    }

    private fun showAppContextMenu(app: AppModel) {
        val isFav = preferencesManager.isFavorite(app.packageName)
        val favLabel = if (isFav) getString(R.string.action_favorite_remove) else getString(R.string.action_favorite_add)

        val options = arrayOf(
            favLabel,
            getString(R.string.action_hide),
            getString(R.string.action_app_info)
        )

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(app.label)
            .setItems(options) { _: DialogInterface, which: Int ->
                when (which) {
                    0 -> {
                        if (isFav) {
                            preferencesManager.removeFavorite(app.packageName)
                        } else {
                            preferencesManager.addFavorite(app.packageName)
                        }
                        loadAppsAsync()
                    }
                    1 -> {
                        preferencesManager.setAppHidden(app.packageName, true)
                        loadAppsAsync()
                    }
                    2 -> {
                        openAppDetails(app.packageName)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showSettingsDialog() {
        val categories = arrayOf(
            "🎨 Apparence",
            "🧩 Widgets",
            "♿ Accessibilité & Système"
        )

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.settings_title)
            .setItems(categories) { _: DialogInterface, which: Int ->
                when (which) {
                    0 -> showAppearanceSettingsDialog()
                    1 -> showWidgetsSettingsDialog()
                    2 -> showAccessibilityAndSystemDialog()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener {
                btnSettings.requestFocus()
            }
            .show()
    }

    private fun showAppearanceSettingsDialog() {
        val wpLabel = when (preferencesManager.getWallpaperSource()) {
            PreferencesManager.WALLPAPER_BING -> "Bing Daily 4K"
            PreferencesManager.WALLPAPER_STORAGE -> "Stockage local TV"
            PreferencesManager.WALLPAPER_GOOGLE_PHOTOS -> "Google Photos"
            else -> "Alpin 4K (Défaut)"
        }
        val drawerCol = "${preferencesManager.getDrawerColumns()} colonnes"
        val shapeLabel = when (preferencesManager.getIconShape()) {
            PreferencesManager.SHAPE_CIRCLE -> "Cercle"
            PreferencesManager.SHAPE_ROUNDED_RECT -> "Rectangle arrondi"
            else -> "Squircle (Apple TV)"
        }
        val scrollModeLabel = if (preferencesManager.getScrollMode() == PreferencesManager.SCROLL_MODE_SMOOTH) "Fluide continu" else "Saut de page"

        val options = arrayOf(
            "🖼 Fond d'écran ($wpLabel)",
            "📐 Colonnes de la grille ($drawerCol)",
            "📐 Forme des icônes ($shapeLabel)",
            "↕️ Mode de défilement ($scrollModeLabel)"
        )

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("🎨 Apparence")
            .setItems(options) { _: DialogInterface, which: Int ->
                when (which) {
                    0 -> showWallpaperSourceDialog()
                    1 -> showDrawerColumnsDialog()
                    2 -> showIconShapeDialog()
                    3 -> {
                        val currentMode = preferencesManager.getScrollMode()
                        val newMode = if (currentMode == PreferencesManager.SCROLL_MODE_SMOOTH) PreferencesManager.SCROLL_MODE_PAGE else PreferencesManager.SCROLL_MODE_SMOOTH
                        preferencesManager.setScrollMode(newMode)
                        showAppearanceSettingsDialog()
                    }
                }
            }
            .setNegativeButton("Retour") { _, _ -> showSettingsDialog() }
            .show()
    }

    private fun showIconShapeDialog() {
        val shapes = arrayOf("Squircle (Style Apple TV)", "Cercle", "Rectangle arrondi")
        val shapeKeys = arrayOf(
            PreferencesManager.SHAPE_SQUIRCLE,
            PreferencesManager.SHAPE_CIRCLE,
            PreferencesManager.SHAPE_ROUNDED_RECT
        )
        val currentShape = preferencesManager.getIconShape()
        val currentIndex = shapeKeys.indexOf(currentShape).coerceAtLeast(0)

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Forme des Icônes")
            .setSingleChoiceItems(shapes, currentIndex) { dialog, which ->
                preferencesManager.setIconShape(shapeKeys[which])
                favoritesAdapter.notifyDataSetChanged()
                drawerAdapter.notifyDataSetChanged()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showWidgetsSettingsDialog() {
        val ramState = if (preferencesManager.isRamWidgetEnabled()) "Activé" else "Désactivé"
        val weatherState = if (preferencesManager.isWeatherWidgetEnabled()) "Activé (${preferencesManager.getWeatherCity()})" else "Désactivé"
        val newsState = if (preferencesManager.isNewsWidgetEnabled()) "Activé" else "Désactivé"
        val sportsState = if (preferencesManager.isSportsWidgetEnabled()) "Activé (${preferencesManager.getSportsType()})" else "Désactivé"
        val topShelfState = if (preferencesManager.isTopShelfEnabled()) "Activé" else "Désactivé"

        val options = arrayOf(
            "⚡ Widget RAM ($ramState)",
            "☀️ Configuration Météo ($weatherState)",
            "📰 Personnalisation Google News ($newsState)",
            "🏆 Scores Sports en direct ($sportsState)",
            "📺 Top Shelf Style Apple TV ($topShelfState)"
        )

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("🧩 Widgets")
            .setItems(options) { _: DialogInterface, which: Int ->
                when (which) {
                    0 -> {
                        preferencesManager.setRamWidgetEnabled(!preferencesManager.isRamWidgetEnabled())
                        refreshWidgetsAsync()
                        showWidgetsSettingsDialog()
                    }
                    1 -> showWeatherConfigDialog()
                    2 -> showNewsPersonalizationDialog()
                    3 -> showSportsConfigDialog()
                    4 -> {
                        preferencesManager.setTopShelfEnabled(!preferencesManager.isTopShelfEnabled())
                        refreshWidgetsAsync()
                        showWidgetsSettingsDialog()
                    }
                }
            }
            .setNegativeButton("Retour") { _, _ -> showSettingsDialog() }
            .show()
    }

    private fun showAccessibilityAndSystemDialog() {
        val contrastState = if (preferencesManager.isHighContrast()) "Activé (OLED)" else "Standard"
        val soundState = if (preferencesManager.isAudioFeedbackEnabled()) "Activé" else "Désactivé"
        val platformLabel = preferencesManager.getPlatformOverride()
        val displayInfo = displayResolutionManager.getDisplayInfo()
        val displaySummary = "${displayInfo.resolutionType.label} • ${displayInfo.refreshRateMode.nominalHz}Hz"

        val options = arrayOf(
            "👁️ Mode Contraste Élevé OLED ($contrastState)",
            "🔊 Rétroaction Sonore D-Pad ($soundState)",
            "🖥️ Résolution & Fréquence HDMI ($displaySummary)",
            "📺 Plateforme TV ($platformLabel)",
            "⚙️ Paramètres Système TV"
        )

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("♿ Accessibilité & Système")
            .setItems(options) { _: DialogInterface, which: Int ->
                when (which) {
                    0 -> {
                        preferencesManager.setHighContrast(!preferencesManager.isHighContrast())
                        applyThemeAndPreferences()
                        favoritesAdapter.notifyDataSetChanged()
                        drawerAdapter.notifyDataSetChanged()
                        showAccessibilityAndSystemDialog()
                    }
                    1 -> {
                        preferencesManager.setAudioFeedbackEnabled(!preferencesManager.isAudioFeedbackEnabled())
                        showAccessibilityAndSystemDialog()
                    }
                    2 -> showDisplayInfoDialog()
                    3 -> showPlatformDialog()
                    4 -> startActivity(Intent(platformManager.getActivePlatform().settingsAction))
                }
            }
            .setNegativeButton("Retour") { _, _ -> showSettingsDialog() }
            .show()
    }

    private fun showDisplayInfoDialog() {
        val info = displayResolutionManager.getDisplayInfo()
        val safeInsets = displayResolutionManager.calculateSafeInsets()
        val highHzStatus = if (info.isHighRefreshRate) "Supporté (120 Hz)" else "Standard (60 Hz)"

        val message = """
            • Résolution détectée : ${info.widthPixels} × ${info.heightPixels} (${info.resolutionType.label})
            • Fréquence HDMI active : ${String.format(java.util.Locale.US, "%.1f", info.refreshRateHz)} Hz (${info.refreshRateMode.label})
            • Mode Haute Fréquence : $highHzStatus
            • Marges Anti-Overscan TV : ${safeInsets.left}px horizontales / ${safeInsets.top}px verticales (5%)
            • VSync & Pacing : Fluidité 60/120 fps sans déchirement
        """.trimIndent()

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Informations Affichage & HDMI")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showWallpaperSourceDialog() {
        val sources = arrayOf(
            "Photo par défaut (Lac Alpin 4K)",
            "Bing (Image 4K Quotidienne)",
            "Stockage TV (/sdcard/Pictures/wallpaper.jpg)",
            "Google Photos (Lien web partagé)"
        )
        val currentIndex = when (preferencesManager.getWallpaperSource()) {
            PreferencesManager.WALLPAPER_BING -> 1
            PreferencesManager.WALLPAPER_STORAGE -> 2
            PreferencesManager.WALLPAPER_GOOGLE_PHOTOS -> 3
            else -> 0
        }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Source du Fond d'écran")
            .setSingleChoiceItems(sources, currentIndex) { dialog, which ->
                val newSource = when (which) {
                    1 -> PreferencesManager.WALLPAPER_BING
                    2 -> PreferencesManager.WALLPAPER_STORAGE
                    3 -> PreferencesManager.WALLPAPER_GOOGLE_PHOTOS
                    else -> PreferencesManager.WALLPAPER_DEFAULT
                }
                preferencesManager.setWallpaperSource(newSource)
                dialog.dismiss()
                refreshWallpaperAsync()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showWeatherConfigDialog() {
        val options = arrayOf(
            "Détection automatique par IP",
            "Montréal (Canada)",
            "Paris (France)",
            "Québec (Canada)",
            "Lyon (France)",
            "Saisie manuelle d'une ville..."
        )

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Configuration Météo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        preferencesManager.setWeatherAutoLocation(true)
                        preferencesManager.setWeatherWidgetEnabled(true)
                        refreshWidgetsAsync()
                    }
                    1 -> setManualCity("Montréal")
                    2 -> setManualCity("Paris")
                    3 -> setManualCity("Québec")
                    4 -> setManualCity("Lyon")
                    5 -> promptCityInput()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setManualCity(city: String) {
        preferencesManager.setWeatherAutoLocation(false)
        preferencesManager.setWeatherCity(city)
        preferencesManager.setWeatherWidgetEnabled(true)
        refreshWidgetsAsync()
    }

    private fun promptCityInput() {
        val input = EditText(this)
        input.hint = "Nom de la ville (ex: Marseille, Bruxelles)"

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Entrez une ville")
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotBlank()) {
                    setManualCity(text)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showNewsPersonalizationDialog() {
        val topics = arrayOf("Technologie", "Cinéma & Séries", "Sports", "Sciences & Espace", "Monde", "Économie")
        val topicKeys = arrayOf(
            PreferencesManager.TOPIC_TECHNOLOGY,
            PreferencesManager.TOPIC_ENTERTAINMENT,
            PreferencesManager.TOPIC_SPORTS,
            PreferencesManager.TOPIC_SCIENCE,
            PreferencesManager.TOPIC_WORLD,
            PreferencesManager.TOPIC_BUSINESS
        )
        val currentTopics = preferencesManager.getNewsTopics().toMutableSet()
        val checkedTopics = BooleanArray(topics.size) { i -> currentTopics.contains(topicKeys[i]) }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Google News : Thématiques")
            .setMultiChoiceItems(topics, checkedTopics) { _, which, isChecked ->
                if (isChecked) currentTopics.add(topicKeys[which]) else currentTopics.remove(topicKeys[which])
            }
            .setPositiveButton("Sources...") { _, _ ->
                preferencesManager.setNewsTopics(currentTopics)
                preferencesManager.setNewsWidgetEnabled(true)
                showNewsSourcesDialog()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showNewsSourcesDialog() {
        val sources = PreferencesManager.PREDEFINED_NEWS_SOURCES.toTypedArray()
        val currentSources = preferencesManager.getNewsSources().toMutableSet()
        val checkedSources = BooleanArray(sources.size) { i -> currentSources.contains(sources[i]) }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Google News : Journaux & Sources")
            .setMultiChoiceItems(sources, checkedSources) { _, which, isChecked ->
                if (isChecked) currentSources.add(sources[which]) else currentSources.remove(sources[which])
            }
            .setPositiveButton("Valider") { _, _ ->
                preferencesManager.setNewsSources(currentSources)
                refreshWidgetsAsync()
            }
            .setNeutralButton("Mot-clé...") { _, _ ->
                preferencesManager.setNewsSources(currentSources)
                promptNewsKeywordInput()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun promptNewsKeywordInput() {
        val input = EditText(this)
        input.hint = "Mot-clé (ex: Intelligence Artificielle, Formule 1)"
        input.setText(preferencesManager.getNewsCustomKeyword())

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Mot-clé Google News personnalisé")
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                preferencesManager.setNewsCustomKeyword(input.text.toString().trim())
                refreshWidgetsAsync()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showSportsConfigDialog() {
        val sports = arrayOf("🏒 Hockey (LNH/NHL)", "🏀 Basketball (NBA)", "🏈 Football (NFL)", "⚽ Soccer (Europe/MLS)", "⚾ Baseball (MLB)")
        val sportKeys = arrayOf(
            SportsManager.SPORT_NHL,
            SportsManager.SPORT_NBA,
            SportsManager.SPORT_NFL,
            SportsManager.SPORT_SOCCER,
            SportsManager.SPORT_MLB
        )
        val currentSport = preferencesManager.getSportsType()
        val currentIndex = sportKeys.indexOf(currentSport).coerceAtLeast(0)

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Widget Sports en direct : Choix du Sport")
            .setSingleChoiceItems(sports, currentIndex) { dialog, which ->
                preferencesManager.setSportsType(sportKeys[which])
                preferencesManager.setSportsWidgetEnabled(true)
                dialog.dismiss()
                showSportsTeamDialog()
            }
            .setNeutralButton(if (preferencesManager.isSportsWidgetEnabled()) "Désactiver" else "Activer") { _, _ ->
                preferencesManager.setSportsWidgetEnabled(!preferencesManager.isSportsWidgetEnabled())
                refreshWidgetsAsync()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showSportsTeamDialog() {
        val input = EditText(this)
        input.hint = "Équipe favorite (ex: Canadiens, Lakers, Real Madrid)"
        input.setText(preferencesManager.getSportsFavoriteTeam())

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Équipe Favorite en Priorité")
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                preferencesManager.setSportsFavoriteTeam(input.text.toString().trim())
                refreshWidgetsAsync()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showPlatformDialog() {
        val platforms = arrayOf("Auto (Détection matérielle)", "Google TV", "Amazon Fire TV")
        val platformKeys = arrayOf(
            PlatformManager.PLATFORM_AUTO,
            PlatformManager.PLATFORM_GOOGLE_TV,
            PlatformManager.PLATFORM_FIRE_TV
        )
        val current = preferencesManager.getPlatformOverride()
        val currentIndex = platformKeys.indexOf(current).coerceAtLeast(0)

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Plateforme TV")
            .setSingleChoiceItems(platforms, currentIndex) { dialog, which ->
                preferencesManager.setPlatformOverride(platformKeys[which])
                platformManager = PlatformManager(platformKeys[which])
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDrawerColumnsDialog() {
        val options = arrayOf("4 colonnes", "5 colonnes (Aligné avec le Dock)", "6 colonnes")
        val values = intArrayOf(4, 5, 6)
        val current = preferencesManager.getDrawerColumns()
        val currentIndex = values.indexOf(current).coerceAtLeast(1)

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Nombre de colonnes du tiroir")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                val selected = values[which]
                preferencesManager.setDrawerColumns(selected)
                (recyclerDrawer.layoutManager as? GridLayoutManager)?.spanCount = selected
                drawerAdapter.notifyDataSetChanged()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openAppDetails(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (_: Exception) {
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isControlCenterOpen) {
                closeControlCenter()
                return true
            }
            if (isDrawerOpen) {
                closeDrawer()
                return true
            }
            // Already on dock at home, stay on home launcher
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
