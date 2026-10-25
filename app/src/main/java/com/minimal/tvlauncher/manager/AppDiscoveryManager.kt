package com.minimal.tvlauncher.manager

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import com.minimal.tvlauncher.model.AppModel
import java.util.Locale

/**
 * AppDiscoveryManager handles fast, low-memory discovery of all installed TV
 * (Leanback) and sideloaded mobile applications on Google TV.
 */
class AppDiscoveryManager(
    private val context: Context,
    private val preferencesManager: IPreferencesManager
) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Scans all installed launchable applications (both TV Leanback and mobile sideloaded).
     *
     * @param includeHidden If true, returns all applications including those marked as hidden.
     * @return List of AppModel items, sorted with favorites first, then alphabetically.
     */
    fun loadInstalledApps(includeHidden: Boolean = false): List<AppModel> {
        val appsMap = mutableMapOf<String, AppModel>()
        val hiddenPackages = preferencesManager.getHiddenApps()
        val favoritePackages = preferencesManager.getFavorites()
        val ownPackageName = context.packageName

        // 1. Discover Official Android TV Leanback Apps
        val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val leanbackList: List<ResolveInfo> = queryIntentSafe(leanbackIntent)

        for (resolveInfo in leanbackList) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == ownPackageName) continue

            val app = createAppModel(resolveInfo, isLeanback = true, favoritePackages, hiddenPackages)
            appsMap[pkg] = app
        }

        // 2. Discover Sideloaded Mobile Apps (Standard Launcher intent)
        val mobileIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val mobileList: List<ResolveInfo> = queryIntentSafe(mobileIntent)

        for (resolveInfo in mobileList) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == ownPackageName) continue

            // Only add if not already registered as leanback app
            if (!appsMap.containsKey(pkg)) {
                val app = createAppModel(resolveInfo, isLeanback = false, favoritePackages, hiddenPackages)
                appsMap[pkg] = app
            }
        }

        // 3. Filter hidden apps if requested
        val resultList = if (includeHidden) {
            appsMap.values.toList()
        } else {
            appsMap.values.filterNot { it.isHidden }
        }

        // 4. Sort: Favorites first (in exact saved order), then remaining alphabetically
        return sortApps(resultList, favoritePackages)
    }

    private fun createAppModel(
        resolveInfo: ResolveInfo,
        isLeanback: Boolean,
        favorites: List<String>,
        hidden: Set<String>
    ): AppModel {
        val packageName = resolveInfo.activityInfo.packageName
        val activityName = resolveInfo.activityInfo.name
        val label = resolveInfo.loadLabel(packageManager).toString()

        // Banner detection: TV apps usually provide a 320x180 banner
        var banner: Drawable? = null
        try {
            banner = resolveInfo.activityInfo.loadBanner(packageManager)
        } catch (_: Exception) {
            banner = null
        }

        // Icon fallback
        val icon: Drawable? = try {
            resolveInfo.loadIcon(packageManager)
        } catch (_: Exception) {
            null
        }

        val isFavorite = favorites.contains(packageName)
        val isHidden = hidden.contains(packageName)

        return AppModel(
            packageName = packageName,
            activityName = activityName,
            label = label,
            isLeanback = isLeanback,
            banner = banner,
            icon = icon,
            isFavorite = isFavorite,
            isHidden = isHidden
        )
    }

    private fun queryIntentSafe(intent: Intent): List<ResolveInfo> {
        return try {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        } catch (_: Exception) {
            try {
                packageManager.queryIntentActivities(intent, 0)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    private fun sortApps(apps: List<AppModel>, favoritesOrder: List<String>): List<AppModel> {
        val favorites = mutableListOf<AppModel>()
        val regular = mutableListOf<AppModel>()

        val favoriteMap = apps.filter { it.isFavorite }.associateBy { it.packageName }

        // Preserve exact favorite ordering
        for (pkg in favoritesOrder) {
            favoriteMap[pkg]?.let { favorites.add(it) }
        }

        for (app in apps) {
            if (!app.isFavorite) {
                regular.add(app)
            }
        }

        // Sort regular apps case-insensitively
        regular.sortBy { it.label.lowercase(Locale.ROOT) }

        return favorites + regular
    }

    /**
     * Launches the chosen application safely.
     */
    fun launchApp(app: AppModel): Boolean {
        return try {
            val intent = packageManager.getLeanbackLaunchIntentForPackage(app.packageName)
                ?: packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
