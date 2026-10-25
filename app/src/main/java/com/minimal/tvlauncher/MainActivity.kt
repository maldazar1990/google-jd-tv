package com.minimal.tvlauncher

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.minimal.tvlauncher.adapter.AppCardAdapter
import com.minimal.tvlauncher.manager.AppDiscoveryManager
import com.minimal.tvlauncher.manager.IPreferencesManager
import com.minimal.tvlauncher.manager.PreferencesManager
import com.minimal.tvlauncher.manager.WidgetManager
import com.minimal.tvlauncher.model.AppModel
import java.util.concurrent.Executors

class MainActivity : FragmentActivity() {

    private lateinit var preferencesManager: IPreferencesManager
    private lateinit var discoveryManager: AppDiscoveryManager
    private lateinit var widgetManager: WidgetManager

    private lateinit var mainRoot: FrameLayout
    private lateinit var recyclerApps: RecyclerView
    private lateinit var progressLoader: ProgressBar
    private lateinit var btnSettings: FrameLayout
    private lateinit var textSectionTitle: TextView

    private lateinit var adapter: AppCardAdapter
    private val backgroundExecutor = Executors.newSingleThreadExecutor()
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

        initViews()
        setupRecyclerView()
        setupListeners()
        registerPackageReceiver()
    }

    override fun onResume() {
        super.onResume()
        applyThemeAndPreferences()
        loadAppsAsync()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(packageChangeReceiver)
        backgroundExecutor.shutdown()
    }

    private fun initViews() {
        mainRoot = findViewById(R.id.main_root)
        recyclerApps = findViewById(R.id.recycler_apps)
        progressLoader = findViewById(R.id.progress_loader)
        btnSettings = findViewById(R.id.btn_settings)
        textSectionTitle = findViewById(R.id.text_section_title)
    }

    private fun setupRecyclerView() {
        val columns = preferencesManager.getGridColumns()
        recyclerApps.layoutManager = GridLayoutManager(this, columns)

        adapter = AppCardAdapter(
            preferencesManager = preferencesManager,
            onAppClick = { app ->
                discoveryManager.launchApp(app)
            },
            onAppLongClick = { app, _ ->
                showAppContextMenu(app)
            }
        )
        recyclerApps.adapter = adapter
    }

    private fun setupListeners() {
        btnSettings.setOnClickListener {
            showSettingsDialog()
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
            }
        }
    }

    private fun applyThemeAndPreferences() {
        val isHighContrast = preferencesManager.isHighContrast()
        if (isHighContrast) {
            mainRoot.setBackgroundColor(android.graphics.Color.BLACK)
            btnSettings.setBackgroundResource(R.drawable.focus_border_high_contrast)
        } else {
            mainRoot.setBackgroundResource(R.drawable.bg_appletv_gradient)
            btnSettings.setBackgroundResource(R.drawable.focus_border_normal)
        }

        val columns = preferencesManager.getGridColumns()
        val currentLayoutManager = recyclerApps.layoutManager as? GridLayoutManager
        if (currentLayoutManager?.spanCount != columns) {
            recyclerApps.layoutManager = GridLayoutManager(this, columns)
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
        val options = arrayOf(
            "Contraste Élevé OLED: " + (if (preferencesManager.isHighContrast()) "ACTIVÉ" else "DÉSACTIVÉ"),
            "Colonnes de la grille: ${preferencesManager.getGridColumns()}",
            "Forme des icônes: ${preferencesManager.getIconShape().replaceFirstChar { it.uppercase() }}",
            "Retour aux réglages système TV"
        )

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.settings_title)
            .setItems(options) { _: DialogInterface, which: Int ->
                when (which) {
                    0 -> {
                        preferencesManager.setHighContrast(!preferencesManager.isHighContrast())
                        applyThemeAndPreferences()
                        adapter.notifyDataSetChanged()
                    }
                    1 -> {
                        cycleGridColumns()
                    }
                    2 -> {
                        cycleIconShapes()
                    }
                    3 -> {
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun cycleGridColumns() {
        val current = preferencesManager.getGridColumns()
        val next = when (current) {
            6 -> 4
            4 -> 3
            else -> 6
        }
        preferencesManager.setGridColumns(next)
        applyThemeAndPreferences()
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
