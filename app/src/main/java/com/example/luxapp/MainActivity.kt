package com.example.luxapp

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import com.cheonjaeung.powerwheelpicker.android.WheelPicker
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener {

    private data class SceneCategory(
        val title: String,
        val scenes: List<SceneRange>
    )

    private data class SceneRange(
        val title: String,
        val minLux: Int,
        val maxLux: Int
    )

    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null
    private var lastLux: Float = 0f
    private var refreshIntervalMs = DEFAULT_UPDATE_INTERVAL_MS
    private val refreshRateOptionsMs = listOf(200L, 500L, 1000L, 2000L)
    private var isDataHoldEnabled = false
    private var heldLux: Float? = null
    private val handler = Handler(Looper.getMainLooper())
    private var luxText: TextView? = null
    private var levelText: TextView? = null
    private var equivalentSceneText: TextView? = null
    private var sceneStatusText: TextView? = null
    private var sceneResultText: TextView? = null
    private var sceneStatusCard: MaterialCardView? = null
    private var selectedSceneText: TextView? = null
    private var majorSceneWheel: WheelPicker? = null
    private var subSceneWheel: WheelPicker? = null
    private var themeToggleButton: ImageButton? = null
    private var dataHoldButton: MaterialButton? = null
    private var refreshRateButton: MaterialButton? = null
    private var refreshRateDisplay: TextView? = null
    private var luxProgress: ProgressBar? = null
    private var panelLuxMeter: View? = null
    private var panelSceneCheck: View? = null
    private var bottomNav: BottomNavigationView? = null
    private var vibrator: Vibrator? = null
    private var selectedScene: SceneRange? = null
    private val progressBarMax = 1000
    private val logLuxUpperBound = 100000f
    private var currentNavItemId: Int = R.id.nav_lux_meter
    private var selectedMajorIndex = 0
    private var selectedSubIndex = 0
    private val majorWheelAdapter = SceneWheelAdapter()
    private val subWheelAdapter = SceneWheelAdapter()
    private var suppressWheelCallback = false
    private var lastMajorTickPosition = RecyclerView.NO_POSITION
    private var lastSubTickPosition = RecyclerView.NO_POSITION
    private var lastTickVibrationAtMs = 0L

    private val sceneCategories: List<SceneCategory> by lazy {
        listOf(
            SceneCategory(
                "居家空间",
                listOf(
                    SceneRange(getString(R.string.scene_bedroom), 30, 75),
                    SceneRange(getString(R.string.scene_living_relax), 50, 120),
                    SceneRange(getString(R.string.scene_living_daily), 100, 300),
                    SceneRange(getString(R.string.scene_dining), 150, 300),
                    SceneRange(getString(R.string.scene_entryway), 75, 150)
                )
            ),
            SceneCategory(
                "功能区域",
                listOf(
                    SceneRange(getString(R.string.scene_kitchen), 300, 500),
                    SceneRange(getString(R.string.scene_bathroom), 200, 500),
                    SceneRange(getString(R.string.scene_balcony), 100, 300),
                    SceneRange(getString(R.string.scene_hallway), 50, 100),
                    SceneRange(getString(R.string.scene_stairs), 75, 150)
                )
            ),
            SceneCategory(
                "学习办公",
                listOf(
                    SceneRange(getString(R.string.scene_study), 300, 500),
                    SceneRange(getString(R.string.scene_office), 500, 750),
                    SceneRange(getString(R.string.scene_kids_study), 500, 750),
                    SceneRange(getString(R.string.scene_elder_reading), 400, 700),
                    SceneRange(getString(R.string.scene_handcraft), 600, 900)
                )
            ),
            SceneCategory(
                "收纳与设备",
                listOf(
                    SceneRange(getString(R.string.scene_dressing_table), 500, 800),
                    SceneRange(getString(R.string.scene_cloakroom), 200, 400),
                    SceneRange(getString(R.string.scene_display), 300, 600),
                    SceneRange(getString(R.string.scene_garage), 100, 200),
                    SceneRange(getString(R.string.scene_basement), 100, 250)
                )
            )
        )
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            val luxToDisplay = if (isDataHoldEnabled) (heldLux ?: lastLux) else lastLux
            updateUiWithLux(luxToDisplay)
            handler.postDelayed(this, refreshIntervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyStatusBarStyle()
        luxText = findViewById(R.id.text_lux)
        levelText = findViewById(R.id.text_level)
        equivalentSceneText = findViewById(R.id.text_equivalent_scene)
        sceneStatusText = findViewById(R.id.text_scene_status)
        sceneResultText = findViewById(R.id.text_scene_result)
        sceneStatusCard = findViewById(R.id.card_scene_status)
        selectedSceneText = findViewById(R.id.text_selected_scene)
        majorSceneWheel = findViewById(R.id.wheel_major_scene)
        subSceneWheel = findViewById(R.id.wheel_sub_scene)
        themeToggleButton = findViewById(R.id.button_theme_toggle)
        dataHoldButton = findViewById(R.id.button_data_hold)
        refreshRateButton = findViewById(R.id.button_refresh_rate)
        refreshRateDisplay = findViewById(R.id.text_refresh_rate_display)
        luxProgress = findViewById(R.id.progress_lux)
        luxProgress?.max = progressBarMax
        panelLuxMeter = findViewById(R.id.panel_lux_meter)
        panelSceneCheck = findViewById(R.id.panel_scene_check)
        bottomNav = findViewById(R.id.bottom_nav)
        currentNavItemId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getInt(KEY_LAST_TAB, R.id.nav_lux_meter)
        loadSceneSelectionPreferences()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as? SensorManager
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

        loadRealtimeControlPreferences()
        setupThemeToggle()
        setupRealtimeControls()
        setupScenePickers()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        }
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            lastLux = event.values[0]
        }
    }

    private fun loadRealtimeControlPreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedRefresh = prefs.getLong(KEY_REFRESH_INTERVAL_MS, DEFAULT_UPDATE_INTERVAL_MS)
        refreshIntervalMs = if (savedRefresh in refreshRateOptionsMs) {
            savedRefresh
        } else {
            DEFAULT_UPDATE_INTERVAL_MS
        }
        isDataHoldEnabled = prefs.getBoolean(KEY_DATA_HOLD_ENABLED, false)
        heldLux = null
    }

    private fun setupRealtimeControls() {
        updateDataHoldButtonUi()
        updateRefreshRateButtonUi()

        dataHoldButton?.setOnClickListener {
            isDataHoldEnabled = !isDataHoldEnabled
            heldLux = if (isDataHoldEnabled) lastLux else null
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_DATA_HOLD_ENABLED, isDataHoldEnabled)
                .apply()
            updateDataHoldButtonUi()
            handler.removeCallbacks(updateRunnable)
            handler.post(updateRunnable)
            vibrateFeedback()
        }

        refreshRateButton?.setOnClickListener {
            val currentIndex = refreshRateOptionsMs.indexOf(refreshIntervalMs).coerceAtLeast(0)
            val nextIndex = (currentIndex + 1) % refreshRateOptionsMs.size
            refreshIntervalMs = refreshRateOptionsMs[nextIndex]
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putLong(KEY_REFRESH_INTERVAL_MS, refreshIntervalMs)
                .apply()
            updateRefreshRateButtonUi()
            handler.removeCallbacks(updateRunnable)
            handler.post(updateRunnable)
            vibrateFeedback()
        }
    }

    private fun updateDataHoldButtonUi() {
        if (isDataHoldEnabled) {
            dataHoldButton?.backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.indicator_color)
            )
            dataHoldButton?.setTextColor(
                ContextCompat.getColor(this, android.R.color.white)
            )
        } else {
            dataHoldButton?.backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.chip_bg)
            )
            dataHoldButton?.setTextColor(
                ContextCompat.getColor(this, R.color.chip_text)
            )
        }
    }

    private fun updateRefreshRateButtonUi() {
        val hz = 1000.0 / refreshIntervalMs
        refreshRateDisplay?.text = if (hz == kotlin.math.floor(hz)) {
            getString(R.string.refresh_rate_format_int, hz.toInt())
        } else {
            getString(R.string.refresh_rate_format_decimal, hz)
        }
        refreshRateButton?.setTextColor(ContextCompat.getColor(this, R.color.chip_text))
    }

    private fun setupScenePickers() {
        selectedMajorIndex = selectedMajorIndex.coerceIn(0, (sceneCategories.size - 1).coerceAtLeast(0))
        majorSceneWheel?.adapter = majorWheelAdapter
        subSceneWheel?.adapter = subWheelAdapter
        majorSceneWheel?.circular = true
        subSceneWheel?.circular = true
        majorSceneWheel?.clearOnScrollListeners()
        subSceneWheel?.clearOnScrollListeners()
        attachWheelTickVibration(majorSceneWheel, isMajorWheel = true)
        attachWheelTickVibration(subSceneWheel, isMajorWheel = false)
        majorWheelAdapter.submitItems(sceneCategories.map { it.title })
        refreshSubWheel(selectedMajorIndex, selectedSubIndex)
        majorWheelAdapter.updateSelectedPosition(selectedMajorIndex)
        subWheelAdapter.updateSelectedPosition(selectedSubIndex)
        lastMajorTickPosition = RecyclerView.NO_POSITION
        lastSubTickPosition = RecyclerView.NO_POSITION
        updateSelectedScene(selectedMajorIndex, selectedSubIndex)
        // 注意：面板此时可能是 gone，不在这里设置滚轮位置。
        // 位置由 showPanel -> centerWheelsOnNextLayout 在面板真正可见后一次性设置。

        majorSceneWheel?.clearOnItemSelectedListeners()
        majorSceneWheel?.addOnItemSelectedListener { _, position ->
            if (suppressWheelCallback || position == selectedMajorIndex) return@addOnItemSelectedListener
            selectedMajorIndex = position
            selectedSubIndex = 0
            majorSceneWheel?.post {
                majorWheelAdapter.updateSelectedPosition(selectedMajorIndex)
            }
            vibrateFeedback()
            refreshSubWheel(selectedMajorIndex, selectedSubIndex)
            updateSelectedScene(selectedMajorIndex, selectedSubIndex)
            persistSceneSelection()
        }

        subSceneWheel?.clearOnItemSelectedListeners()
        subSceneWheel?.addOnItemSelectedListener { _, position ->
            if (suppressWheelCallback || position == selectedSubIndex) return@addOnItemSelectedListener
            selectedSubIndex = position
            subSceneWheel?.post {
                subWheelAdapter.updateSelectedPosition(selectedSubIndex)
            }
            vibrateFeedback()
            updateSelectedScene(selectedMajorIndex, selectedSubIndex)
            persistSceneSelection()
        }
    }

    private fun attachWheelTickVibration(wheelPicker: WheelPicker?, isMajorWheel: Boolean) {
        wheelPicker ?: return
        wheelPicker.addOnScrollListener(object : WheelPicker.OnScrollListener() {
            override fun onScrolled(wheelPicker: WheelPicker, dx: Int, dy: Int) {
                if (suppressWheelCallback) return
                val currentPosition = wheelPicker.currentPosition
                if (currentPosition == RecyclerView.NO_POSITION) return
                if (isMajorWheel) {
                    if (currentPosition == lastMajorTickPosition) return
                    lastMajorTickPosition = currentPosition
                } else {
                    if (currentPosition == lastSubTickPosition) return
                    lastSubTickPosition = currentPosition
                }
                vibrateWheelTick()
            }
        })
    }

    private fun refreshSubWheel(majorIndex: Int, targetSubIndex: Int) {
        val subTitles = sceneCategories
            .getOrNull(majorIndex)
            ?.scenes
            ?.map { it.title }
            ?: emptyList()
        subWheelAdapter.submitItems(subTitles)
        selectedSubIndex = targetSubIndex.coerceIn(0, (subTitles.size - 1).coerceAtLeast(0))
        suppressWheelCallback = true
        subWheelAdapter.updateSelectedPosition(selectedSubIndex)
        val sub = subSceneWheel ?: run { suppressWheelCallback = false; return }
        sub.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                sub.viewTreeObserver.removeOnGlobalLayoutListener(this)
                sub.setCurrentPosition(selectedSubIndex, false)
                suppressWheelCallback = false
                lastSubTickPosition = sub.currentPosition
            }
        })
    }

    private fun updateSelectedScene(majorIndex: Int, subIndex: Int) {
        val category = sceneCategories.getOrNull(majorIndex) ?: return
        val scene = category.scenes.getOrNull(subIndex) ?: return
        selectedScene = scene
        selectedSceneText?.text = getString(R.string.selected_scene_format, category.title, scene.title)
        updateSceneResult(lastLux)
    }

    private fun setupThemeToggle() {
        val isDarkMode = AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO
        updateThemeIcon(isDarkMode)
        themeToggleButton?.setOnClickListener {
            vibrateFeedback()
            val nextDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO
            updateThemeIcon(nextDarkMode)
            val newMode = if (nextDarkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(KEY_THEME_MODE, newMode)
                .putInt(KEY_LAST_TAB, currentNavItemId)
                .apply()
            AppCompatDelegate.setDefaultNightMode(newMode)
        }
    }

    private fun loadSceneSelectionPreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        selectedMajorIndex = prefs.getInt(KEY_SCENE_MAJOR_INDEX, 0)
        selectedSubIndex = prefs.getInt(KEY_SCENE_SUB_INDEX, 0)
    }

    private fun persistSceneSelection() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putInt(KEY_SCENE_MAJOR_INDEX, selectedMajorIndex)
            .putInt(KEY_SCENE_SUB_INDEX, selectedSubIndex)
            .apply()
    }

    private fun setupBottomNavigation() {
        showPanel(isLuxMeter = currentNavItemId == R.id.nav_lux_meter)
        bottomNav?.selectedItemId = currentNavItemId
        bottomNav?.setOnItemSelectedListener { item ->
            if (item.itemId == currentNavItemId) {
                return@setOnItemSelectedListener true
            }
            currentNavItemId = item.itemId
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(KEY_LAST_TAB, currentNavItemId)
                .apply()
            vibrateFeedback()
            when (item.itemId) {
                R.id.nav_lux_meter -> {
                    showPanel(isLuxMeter = true)
                    true
                }
                R.id.nav_scene_check -> {
                    showPanel(isLuxMeter = false)
                    true
                }
                else -> false
            }
        }
    }

    private fun showPanel(isLuxMeter: Boolean) {
        panelLuxMeter?.visibility = if (isLuxMeter) View.VISIBLE else View.GONE
        panelSceneCheck?.visibility = if (isLuxMeter) View.GONE else View.VISIBLE
        if (!isLuxMeter) {
            centerWheelsOnNextLayout()
        }
    }

    private fun centerWheelsOnNextLayout() {
        val major = majorSceneWheel ?: return
        major.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                major.viewTreeObserver.removeOnGlobalLayoutListener(this)
                suppressWheelCallback = true
                majorSceneWheel?.setCurrentPosition(selectedMajorIndex, false)
                subSceneWheel?.setCurrentPosition(selectedSubIndex, false)
                majorWheelAdapter.updateSelectedPosition(selectedMajorIndex)
                subWheelAdapter.updateSelectedPosition(selectedSubIndex)
                suppressWheelCallback = false
                lastMajorTickPosition = majorSceneWheel?.currentPosition ?: RecyclerView.NO_POSITION
                lastSubTickPosition = subSceneWheel?.currentPosition ?: RecyclerView.NO_POSITION
            }
        })
    }

    private fun updateThemeIcon(isDarkMode: Boolean) {
        themeToggleButton?.setImageResource(
            if (isDarkMode) R.drawable.ic_moon else R.drawable.ic_sun
        )
    }

    private fun vibrateFeedback() {
        val targetVibrator = vibrator ?: return
        if (!targetVibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            targetVibrator.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            targetVibrator.vibrate(18L)
        }
    }

    private fun applySavedTheme() {
        val savedMode = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_YES)
        AppCompatDelegate.setDefaultNightMode(savedMode)
    }

    private fun applyStatusBarStyle() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)
        // 与底部栏同色，使底部栏背景视觉上延伸到底部导航栏
        window.navigationBarColor = ContextCompat.getColor(this, R.color.card_bg)
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isDarkMode
        insetsController.isAppearanceLightNavigationBars = !isDarkMode
    }

    private fun updateUiWithLux(lux: Float) {
        luxText?.text = getString(R.string.lux_format, formatLux(lux))
        luxProgress?.progress = mapLuxToLogProgress(lux)
        levelText?.text = getBrightnessLevelLabel(lux)
        equivalentSceneText?.text = getString(
            R.string.equivalent_scene_format,
            findClosestScene(lux).title
        )
        updateSceneResult(lux)
    }

    private fun updateSceneResult(lux: Float) {
        val scene = selectedScene ?: return
        val state = when {
            lux < scene.minLux -> SceneStatus.LOW
            lux > scene.maxLux -> SceneStatus.HIGH
            else -> SceneStatus.OK
        }
        val statusLabelRes = when (state) {
            SceneStatus.OK -> R.string.scene_status_ok
            SceneStatus.LOW -> R.string.scene_status_low
            SceneStatus.HIGH -> R.string.scene_status_high
        }
        sceneStatusText?.text = getString(
            R.string.scene_status_format,
            scene.title,
            getString(statusLabelRes)
        )
        val detailRes = when (state) {
            SceneStatus.OK -> R.string.scene_result_ok_detail
            SceneStatus.LOW -> R.string.scene_result_low_detail
            SceneStatus.HIGH -> R.string.scene_result_high_detail
        }
        sceneResultText?.text = getString(
            detailRes,
            scene.minLux,
            scene.maxLux,
            lux
        )
        selectedSceneText?.text = getString(R.string.selected_scene_format, getSceneCategoryTitle(scene), scene.title)
        applySceneStatusStyle(state)
    }

    private fun getSceneCategoryTitle(scene: SceneRange): String {
        return sceneCategories.firstOrNull { category -> category.scenes.contains(scene) }?.title
            ?: getString(R.string.scene_check_title)
    }

    private fun applySceneStatusStyle(status: SceneStatus) {
        val bgColor = ContextCompat.getColor(
            this,
            when (status) {
                SceneStatus.OK -> R.color.scene_ok_bg
                SceneStatus.LOW -> R.color.scene_low_bg
                SceneStatus.HIGH -> R.color.scene_high_bg
            }
        )
        val textColor = ContextCompat.getColor(
            this,
            when (status) {
                SceneStatus.OK -> R.color.scene_ok_text
                SceneStatus.LOW -> R.color.scene_low_text
                SceneStatus.HIGH -> R.color.scene_high_text
            }
        )
        sceneStatusCard?.setCardBackgroundColor(bgColor)
        sceneStatusCard?.strokeColor = textColor
        sceneStatusText?.setTextColor(textColor)
        sceneResultText?.setTextColor(textColor)
    }

    private fun findClosestScene(lux: Float): SceneRange {
        val allScenes = sceneCategories.flatMap { it.scenes }
        var closestScene = allScenes.first()
        var minDistance = Float.MAX_VALUE
        for (scene in allScenes) {
            val center = (scene.minLux + scene.maxLux) / 2f
            val distance = kotlin.math.abs(center - lux)
            if (distance < minDistance) {
                minDistance = distance
                closestScene = scene
            }
        }
        return closestScene
    }

    private fun getBrightnessLevelLabel(lux: Float): String {
        val label = when {
            lux < 10f -> "夜晚微光"
            lux < 50f -> "昏暗环境"
            lux < 150f -> "柔和舒适"
            lux < 300f -> "居家日常"
            lux < 500f -> "明亮清晰"
            lux < 1000f -> "办公级亮度"
            lux < 5000f -> "强自然光"
            else -> "户外强光"
        }
        return "亮度等级：$label"
    }

    private fun formatLux(lux: Float): String {
        return String.format(Locale.getDefault(), "%.2f", lux)
    }

    private fun mapLuxToLogProgress(lux: Float): Int {
        val clampedLux = lux.coerceAtLeast(0f)
        val numerator = kotlin.math.ln(1f + clampedLux)
        val denominator = kotlin.math.ln(1f + logLuxUpperBound)
        if (denominator <= 0f) return 0
        val normalized = (numerator / denominator).coerceIn(0f, 1f)
        return (normalized * progressBarMax).toInt()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val PREFS_NAME = "lux_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LAST_TAB = "last_tab"
        private const val KEY_SCENE_MAJOR_INDEX = "scene_major_index"
        private const val KEY_SCENE_SUB_INDEX = "scene_sub_index"
        private const val KEY_DATA_HOLD_ENABLED = "data_hold_enabled"
        private const val KEY_REFRESH_INTERVAL_MS = "refresh_interval_ms"
        private const val DEFAULT_UPDATE_INTERVAL_MS = 500L
    }

    private inner class SceneWheelAdapter : RecyclerView.Adapter<SceneWheelAdapter.SceneViewHolder>() {
        private val items = mutableListOf<String>()
        private var selectedPosition: Int = RecyclerView.NO_POSITION

        fun submitItems(newItems: List<String>) {
            items.clear()
            items.addAll(newItems)
            selectedPosition = RecyclerView.NO_POSITION
            notifyDataSetChanged()
        }

        fun updateSelectedPosition(newPosition: Int) {
            selectedPosition = newPosition
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): SceneViewHolder {
            val view = layoutInflater.inflate(R.layout.item_scene_wheel, parent, false)
            return SceneViewHolder(view)
        }

        override fun onBindViewHolder(holder: SceneViewHolder, position: Int) {
            holder.bind(items[position], position == selectedPosition)
        }

        override fun getItemCount(): Int = items.size

        inner class SceneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val text: TextView = itemView.findViewById(R.id.text_wheel_item)

            fun bind(label: String, selected: Boolean) {
                text.text = label
                text.alpha = if (selected) 1f else 0.45f
                text.textSize = if (selected) 17f else 16f
                text.setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        if (selected) R.color.text_primary else R.color.text_secondary
                    )
                )
            }
        }
    }

    private enum class SceneStatus {
        OK, LOW, HIGH
    }

    private fun vibrateWheelTick() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastTickVibrationAtMs < 30L) return
        lastTickVibrationAtMs = now
        val targetVibrator = vibrator ?: return
        if (!targetVibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            targetVibrator.vibrate(VibrationEffect.createOneShot(8L, 50))
        } else {
            @Suppress("DEPRECATION")
            targetVibrator.vibrate(8L)
        }
    }
}
