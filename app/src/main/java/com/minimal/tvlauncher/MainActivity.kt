package com.minimal.tvlauncher

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.minimal.tvlauncher.adapter.AppCardAdapter
import com.minimal.tvlauncher.manager.AppDiscoveryManager
import com.minimal.tvlauncher.manager.BingWallpaperManager
import com.minimal.tvlauncher.manager.IPreferencesManager
import com.minimal.tvlauncher.manager.NewsManager
import com.minimal.tvlauncher.manager.PreferencesManager
import com.minimal.tvlauncher.manager.WeatherManager
import com.minimal.tvlauncher.manager.WidgetManager
import com.minimal.tvlauncher.model.AppModel
import java.util.concurrent.Executors

class MainActivity : FragmentActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var discoveryManager: AppDiscoveryManager
    private lateinit var widgetManager: WidgetManager
    private lateinit var wallpaperManager: BingWallpaperManager
    private lateinit var newsManager: NewsManager
    private lateinit var weatherManager: WeatherManager

    private lateinit var mainRoot: FrameLayout
    private lateinit var imageWallpaper: ImageView
    private lateinit var viewScrim: View
    private lateinit var recyclerApps: RecyclerView
    private lateinit var progressLoader: ProgressBar
    private lateinit var btnSettings: FrameLayout

    // Status Widgets
    private lateinit var widgetRamContainer: LinearLayout
    private lateinit var textWidgetRam: TextView
    private lateinit var widgetNetworkContainer: LinearLayout
    private lateinit var iconWidgetNetwork: ImageView
    private lateinit var textWidgetNetwork: TextView
    private lateinit var widgetWeatherContainer: LinearLayout
    private lateinit var iconWidgetWeather: ImageView
    private lateinit var textWidgetWeather: TextView

    // News Ticker
    private lateinit var widgetNewsContainer: FrameLayout
    private lateinit var textWidgetNews: TextView

    private lateinit var adapter: AppCardAdapter
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

        ensureDefaultWidgetPreferences()
        initViews()
        setupRecyclerView()
        setupListeners()
        registerPackageReceiver()
    }

    override fun onResume() {
        super.onResume()
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
        viewScrim = findViewById(R.id.view_scrim)
        recyclerApps = findViewById(R.id.recycler_apps)
        progressLoader = findViewById(R.id.progress_loader)
        btnSettings = findViewById(R.id.btn_settings)

        widgetRamContainer = findViewById(R.id.widget_ram_container)
        textWidgetRam = findViewById(R.id.text_widget_ram)
        widgetNetworkContainer = findViewById(R.id.widget_network_container)
        iconWidgetNetwork = findViewById(R.id.icon_widget_network)
        textWidgetNetwork = findViewById(R.id.text_widget_network)
        widgetWeatherContainer = findViewById(R.id.widget_weather_container)
        iconWidgetWeather = findViewById(R.id.icon_widget_weather)
        textWidgetWeather = findViewById(R.id.text_widget_weather)

        widgetNewsContainer = findViewById(R.id.widget_news_container)
        textWidgetNews = findViewById(R.id.text_widget_news)

        // Make news ticker marquee continuously
        textWidgetNews.isSelected = true
    }

    private fun setupRecyclerView() {
        // Apple TV signature horizontal dock
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerApps.layoutManager = layoutManager

        adapter = AppCardAdapter(
            preferencesManager = preferencesManager,
            onAppClick = { app ->
                discoveryManager.launchApp(app)
            },
            onAppLongClick = { app, _ ->
                showAppContextMenu(app)
            },
            onAppFocus = { app, hasFocus ->
                // Optional subtitle feedback
            }
        )
        recyclerApps.adapter = adapter
    }

    private fun setupListeners() {
        btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        // Enable D-Pad click on news ticker if desired
        widgetNewsContainer.setOnClickListener {
            refreshWidgetsAsync()
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
            val apps = discoveryManager.loadInstalledApps(includeHidden = false)
            mainHandler.post {
                adapter.setApps(apps)
                progressLoader.visibility = View.GONE

                // Auto-focus the first app in the dock on launch
                if (recyclerApps.childCount > 0) {
                    recyclerApps.getChildAt(0)?.requestFocus()
                } else {
                    recyclerApps.post {
                        recyclerApps.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    }
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

        // 2. Network widget
        val networkEnabled = preferencesManager.isNetworkWidgetEnabled()
        widgetNetworkContainer.visibility = if (networkEnabled) View.VISIBLE else View.GONE
        if (networkEnabled) {
            val (iconRes, netLabel) = when (widgetManager.getNetworkStatus()) {
                WidgetManager.NetworkType.ETHERNET -> Pair(R.drawable.ic_wifi, "Ethernet")
                WidgetManager.NetworkType.WIFI -> Pair(R.drawable.ic_wifi, "Wi-Fi 6")
                WidgetManager.NetworkType.NONE -> Pair(R.drawable.ic_wifi, "Non connecté")
            }
            iconWidgetNetwork.setImageResource(iconRes)
            textWidgetNetwork.text = netLabel
        }

        // 3. Weather widget
        val weatherEnabled = preferencesManager.isWeatherWidgetEnabled()
        widgetWeatherContainer.visibility = if (weatherEnabled) View.VISIBLE else View.GONE

        // 4. News ticker
        val newsEnabled = preferencesManager.isNewsWidgetEnabled()
        widgetNewsContainer.visibility = if (newsEnabled) View.VISIBLE else View.GONE

        // Async network fetching for weather & news
        if (weatherEnabled || newsEnabled) {
            backgroundExecutor.execute {
                val weatherString = if (weatherEnabled) weatherManager.getWeatherText() else null
                val newsString = if (newsEnabled) newsManager.getNewsTickerText() else null

                mainHandler.post {
                    if (weatherEnabled && weatherString != null) {
                        textWidgetWeather.text = weatherString
                    }
                    if (newsEnabled && newsString != null) {
                        textWidgetNews.text = newsString
                        textWidgetNews.isSelected = true
                    }
                }
            }
        }
    }

    private fun refreshWallpaperAsync() {
        val bingEnabled = preferencesManager.isBingWallpaperEnabled()
        val isHighContrast = preferencesManager.isHighContrast()

        if (isHighContrast) {
            imageWallpaper.visibility = View.GONE
            viewScrim.visibility = View.GONE
            mainRoot.setBackgroundColor(Color.BLACK)
            return
        }

        imageWallpaper.visibility = View.VISIBLE
        viewScrim.visibility = View.VISIBLE

        if (bingEnabled) {
            backgroundExecutor.execute {
                // Check if we have cached wallpaper or need to fetch
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
        } else {
            imageWallpaper.setImageResource(R.drawable.bg_default_scenic)
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
            btnSettings.setBackgroundResource(R.drawable.bg_capsule_pill_selector)
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
        val ramState = if (preferencesManager.isRamWidgetEnabled()) "Activé" else "Désactivé"
        val netState = if (preferencesManager.isNetworkWidgetEnabled()) "Activé" else "Désactivé"
        val weatherState = if (preferencesManager.isWeatherWidgetEnabled()) "Activé" else "Désactivé"
        val newsState = if (preferencesManager.isNewsWidgetEnabled()) "Activé" else "Désactivé"
        val wallpaperState = if (preferencesManager.isBingWallpaperEnabled()) "Bing 4K" else "Alpin par défaut"
        val contrastState = if (preferencesManager.isHighContrast()) "Activé (OLED)" else "Standard"

        val options = arrayOf(
            "⚡ Widget RAM ($ramState)",
            "📡 Widget Réseau ($netState)",
            "☀️ Widget Météo ($weatherState)",
            "📰 Bandeau Google News ($newsState)",
            "🖼 Fond d'écran ($wallpaperState)",
            "👁️ Mode Contraste Élevé ($contrastState)",
            "📐 Forme des Icônes (${preferencesManager.getIconShape()})",
            "⚙️ Réglages Système Android TV"
        )

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.settings_title)
            .setItems(options) { _: DialogInterface, which: Int ->
                when (which) {
                    0 -> {
                        preferencesManager.setRamWidgetEnabled(!preferencesManager.isRamWidgetEnabled())
                        refreshWidgetsAsync()
                    }
                    1 -> {
                        preferencesManager.setNetworkWidgetEnabled(!preferencesManager.isNetworkWidgetEnabled())
                        refreshWidgetsAsync()
                    }
                    2 -> {
                        preferencesManager.setWeatherWidgetEnabled(!preferencesManager.isWeatherWidgetEnabled())
                        refreshWidgetsAsync()
                    }
                    3 -> {
                        preferencesManager.setNewsWidgetEnabled(!preferencesManager.isNewsWidgetEnabled())
                        refreshWidgetsAsync()
                    }
                    4 -> {
                        preferencesManager.setBingWallpaperEnabled(!preferencesManager.isBingWallpaperEnabled())
                        refreshWallpaperAsync()
                    }
                    5 -> {
                        preferencesManager.setHighContrast(!preferencesManager.isHighContrast())
                        applyThemeAndPreferences()
                        adapter.notifyDataSetChanged()
                    }
                    6 -> {
                        cycleIconShapes()
                    }
                    7 -> {
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun cycleIconShapes() {
        val current = preferencesManager.getIconShape()
        val next = when (current) {
            PreferencesManager.SHAPE_SQUIRCLE -> PreferencesManager.SHAPE_CIRCLE
            PreferencesManager.SHAPE_CIRCLE -> PreferencesManager.SHAPE_ROUNDED_RECT
            else -> PreferencesManager.SHAPE_SQUIRCLE
        }
        preferencesManager.setIconShape(next)
        adapter.notifyDataSetChanged()
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
        // Prevent Back button from closing the Home Launcher Activity
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
