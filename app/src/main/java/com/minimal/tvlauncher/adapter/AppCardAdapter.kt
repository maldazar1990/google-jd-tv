package com.minimal.tvlauncher.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.minimal.tvlauncher.R
import com.minimal.tvlauncher.manager.IPreferencesManager
import com.minimal.tvlauncher.manager.PreferencesManager
import com.minimal.tvlauncher.model.AppModel

/**
 * High-performance, zero-stutter RecyclerView Adapter for Android TV.
 * Delivers tvOS Apple TV fluid focus scaling (1.08x) and elevation glow at 60 fps.
 */
class AppCardAdapter(
    private val preferencesManager: IPreferencesManager,
    private val onAppClick: (AppModel) -> Unit,
    private val onAppLongClick: (AppModel, View) -> Unit,
    private val onAppFocus: ((AppModel, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<AppCardAdapter.AppViewHolder>() {

    private val apps = mutableListOf<AppModel>()

    fun setApps(newApps: List<AppModel>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = apps.size
            override fun getNewListSize(): Int = newApps.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return apps[oldItemPosition].packageName == newApps[newItemPosition].packageName
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = apps[oldItemPosition]
                val new = newApps[newItemPosition]
                return old.label == new.label &&
                        old.isFavorite == new.isFavorite &&
                        old.isHidden == new.isHidden
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        apps.clear()
        apps.addAll(newApps)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = apps.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_card, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(app, preferencesManager, onAppClick, onAppLongClick, onAppFocus)
    }

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardRoot: View = itemView.findViewById(R.id.card_root)
        private val cardContainer: FrameLayout = itemView.findViewById(R.id.card_container)
        private val cardView: CardView = itemView.findViewById(R.id.card_view)
        private val imgAppIcon: ImageView = itemView.findViewById(R.id.img_app_icon)
        private val viewFocusBorder: View = itemView.findViewById(R.id.view_focus_border)
        private val badgeFavorite: ImageView = itemView.findViewById(R.id.badge_favorite)
        private val textAppTitle: TextView = itemView.findViewById(R.id.text_app_title)

        fun bind(
            app: AppModel,
            prefs: IPreferencesManager,
            onClick: (AppModel) -> Unit,
            onLongClick: (AppModel, View) -> Unit,
            onFocus: ((AppModel, Boolean) -> Unit)?
        ) {
            textAppTitle.text = app.label
            cardRoot.contentDescription = app.label

            // Banner or Icon handling with fallback
            if (app.banner != null) {
                imgAppIcon.setImageDrawable(app.banner)
                imgAppIcon.setPadding(0, 0, 0, 0)
                imgAppIcon.scaleType = ImageView.ScaleType.CENTER_CROP
            } else if (app.icon != null) {
                imgAppIcon.setImageDrawable(app.icon)
                imgAppIcon.setPadding(16, 16, 16, 16)
                imgAppIcon.scaleType = ImageView.ScaleType.FIT_CENTER
            } else {
                imgAppIcon.setImageResource(R.drawable.ic_app_placeholder)
                imgAppIcon.setPadding(16, 16, 16, 16)
                imgAppIcon.scaleType = ImageView.ScaleType.FIT_CENTER
            }

            // Favorite star badge
            badgeFavorite.visibility = if (app.isFavorite) View.VISIBLE else View.GONE

            // Apply customizable icon shape (Squircle, Circle, Rounded Rectangle)
            val cornerRadiusDp = when (prefs.getIconShape()) {
                PreferencesManager.SHAPE_CIRCLE -> 58f
                PreferencesManager.SHAPE_ROUNDED_RECT -> 12f
                else -> 24f // Squircle default
            }
            val density = itemView.resources.displayMetrics.density
            cardView.radius = cornerRadiusDp * density

            // High Contrast Mode check
            val isHighContrast = prefs.isHighContrast()
            if (isHighContrast) {
                cardView.setCardBackgroundColor(Color.parseColor("#121212"))
                viewFocusBorder.setBackgroundResource(R.drawable.focus_border_high_contrast)
                textAppTitle.setTextColor(Color.parseColor("#FFFFFF"))
            } else {
                cardView.setCardBackgroundColor(Color.parseColor("#252A36"))
                viewFocusBorder.setBackgroundResource(R.drawable.focus_border_apple)
                textAppTitle.setTextColor(Color.parseColor("#F5F7FA"))
            }

            // D-Pad Focus Behavior (Apple TV fluid animation at 60 fps)
            cardRoot.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    cardContainer.animate()
                        .scaleX(1.10f)
                        .scaleY(1.10f)
                        .translationZ(10f * density)
                        .setDuration(120)
                        .start()
                    viewFocusBorder.visibility = View.VISIBLE
                    textAppTitle.isSelected = true
                } else {
                    cardContainer.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .translationZ(0f)
                        .setDuration(120)
                        .start()
                    viewFocusBorder.visibility = View.INVISIBLE
                    textAppTitle.isSelected = false
                }
                onFocus?.invoke(app, hasFocus)
            }

            cardRoot.setOnClickListener {
                onClick(app)
            }

            cardRoot.setOnLongClickListener {
                onLongClick(app, it)
                true
            }
        }
    }
}
