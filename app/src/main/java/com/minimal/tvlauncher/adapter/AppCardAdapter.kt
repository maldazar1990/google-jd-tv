package com.minimal.tvlauncher.adapter

import android.graphics.Color
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.minimal.tvlauncher.R
import com.minimal.tvlauncher.config.AppConfig
import com.minimal.tvlauncher.manager.IPreferencesManager
import com.minimal.tvlauncher.manager.PreferencesManager
import com.minimal.tvlauncher.model.AppModel

/**
 * High-performance, zero-stutter RecyclerView Adapter for Android TV.
 * Delivers tvOS Apple TV fluid focus scaling (1.08x) and elevation glow at 60 fps,
 * along with native D-Pad audio feedback and bidirectional vertical navigation.
 */
class AppCardAdapter(
    private val preferencesManager: IPreferencesManager,
    private val onAppClick: (AppModel) -> Unit,
    private val onAppLongClick: (AppModel, View) -> Unit,
    private val onAppFocus: ((AppModel, Boolean) -> Unit)? = null,
    private val nextFocusDownId: Int = View.NO_ID,
    private val nextFocusUpId: Int = View.NO_ID,
    private val onDpadKeyDown: ((position: Int, keyCode: Int) -> Boolean)? = null
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
        holder.bind(app, preferencesManager, onAppClick, onAppLongClick, onAppFocus, nextFocusDownId, nextFocusUpId, onDpadKeyDown)
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
            onFocus: ((AppModel, Boolean) -> Unit)?,
            focusDownId: Int = View.NO_ID,
            focusUpId: Int = View.NO_ID,
            onDpadKeyDown: ((position: Int, keyCode: Int) -> Boolean)? = null
        ) {
            textAppTitle.text = app.label
            cardRoot.contentDescription = app.label

            // Explicit vertical D-Pad focus chaining between Dock and Drawer
            if (focusDownId != View.NO_ID) {
                cardRoot.nextFocusDownId = focusDownId
            }
            if (focusUpId != View.NO_ID) {
                cardRoot.nextFocusUpId = focusUpId
            }

            // D-Pad Interceptor for 2-Tier Navigation
            cardRoot.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    val pos = when {
                        bindingAdapterPosition != RecyclerView.NO_POSITION -> bindingAdapterPosition
                        absoluteAdapterPosition != RecyclerView.NO_POSITION -> absoluteAdapterPosition
                        layoutPosition != RecyclerView.NO_POSITION -> layoutPosition
                        else -> 0
                    }
                    if (onDpadKeyDown?.invoke(pos, keyCode) == true) {
                        return@setOnKeyListener true
                    }
                }
                false
            }

            // Banner or Icon handling with fallback
            if (app.banner != null) {
                imgAppIcon.setImageDrawable(app.banner)
                imgAppIcon.setPadding(0, 0, 0, 0)
                imgAppIcon.scaleType = ImageView.ScaleType.CENTER_CROP
            } else if (app.icon != null) {
                imgAppIcon.setImageDrawable(app.icon)
                imgAppIcon.setPadding(8, 8, 8, 8)
                imgAppIcon.scaleType = ImageView.ScaleType.FIT_CENTER
            } else {
                imgAppIcon.setImageResource(R.drawable.ic_app_placeholder)
                imgAppIcon.setPadding(8, 8, 8, 8)
                imgAppIcon.scaleType = ImageView.ScaleType.FIT_CENTER
            }

            // Favorite star badge
            badgeFavorite.visibility = if (app.isFavorite) View.VISIBLE else View.GONE

            // Apply customizable icon shape (Squircle, Circle, Rounded Rectangle)
            val cornerRadiusDp = when (prefs.getIconShape()) {
                PreferencesManager.SHAPE_CIRCLE -> 47f
                PreferencesManager.SHAPE_ROUNDED_RECT -> 10f
                else -> AppConfig.Ui.CARD_CORNER_RADIUS_DP // Widescreen squircle default
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

            // D-Pad Focus Behavior (Apple TV fluid animation at 60 fps + Audio Feedback)
            cardRoot.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    if (prefs.isAudioFeedbackEnabled()) {
                        v.playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT)
                    }
                    cardContainer.animate()
                        .scaleX(AppConfig.Ui.FOCUS_SCALE_FACTOR)
                        .scaleY(AppConfig.Ui.FOCUS_SCALE_FACTOR)
                        .translationZ(10f * density)
                        .setDuration(AppConfig.Ui.FOCUS_ANIMATION_DURATION_MS)
                        .start()
                    viewFocusBorder.visibility = View.VISIBLE
                    textAppTitle.isSelected = true
                } else {
                    cardContainer.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .translationZ(0f)
                        .setDuration(AppConfig.Ui.FOCUS_ANIMATION_DURATION_MS)
                        .start()
                    viewFocusBorder.visibility = View.INVISIBLE
                    textAppTitle.isSelected = false
                }
                onFocus?.invoke(app, hasFocus)
            }

            cardRoot.setOnClickListener { v ->
                if (prefs.isAudioFeedbackEnabled()) {
                    v.playSoundEffect(SoundEffectConstants.CLICK)
                }
                onClick(app)
            }

            cardRoot.setOnLongClickListener {
                onLongClick(app, it)
                true
            }
        }
    }
}
