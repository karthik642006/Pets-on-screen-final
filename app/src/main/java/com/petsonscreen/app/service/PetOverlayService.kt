package com.petsonscreen.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.petsonscreen.app.MainActivity
import com.petsonscreen.app.data.PetPreferencesRepository
import com.petsonscreen.app.engine.MascotInstance
import com.petsonscreen.app.engine.TICK_MS
import com.petsonscreen.app.model.PetAppPreferences
import com.petsonscreen.app.sprites.PetSpriteCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

class PetOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: PetPreferencesRepository
    private lateinit var windowManager: WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentPrefs = PetAppPreferences()
    private var isScreenOn = true
    private var lastCommandSeq = 0L

    private val petViews = mutableListOf<OverlayPetEntry>()
    private var quickMenuView: View? = null
    private var quickMenuDismissRunnable: Runnable? = null

    private data class OverlayPetEntry(
        val mascot: MascotInstance,
        val view: PetOverlayView,
        val params: WindowManager.LayoutParams
    )

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isScreenOn || !currentPrefs.enabled || currentPrefs.petsHidden) return
            val screenW = resources.displayMetrics.widthPixels.toFloat()
            val screenH = resources.displayMetrics.heightPixels.toFloat()
            val spritePx = computeSpriteSizePx(currentPrefs.size)

            petViews.forEach { entry ->
                if (entry.params.width != spritePx || entry.params.height != spritePx) {
                    entry.params.width = spritePx
                    entry.params.height = spritePx
                }
                entry.mascot.tick(screenW, screenH, spritePx.toFloat(), currentPrefs.speed)
                entry.params.x = entry.mascot.x.roundToInt()
                entry.params.y = entry.mascot.y.roundToInt()
                runCatching {
                    windowManager.updateViewLayout(entry.view, entry.params)
                }
                entry.view.invalidate()
            }
            mainHandler.postDelayed(this, TICK_MS)
        }
    }

    private val reshuffleRunnable = object : Runnable {
        override fun run() {
            if (currentPrefs.enabled && !currentPrefs.petsHidden) {
                rebuildActivePets(forceReshuffle = true)
            }
            val intervalMs = currentPrefs.timeChange.coerceIn(1, 60) * 60_000L
            mainHandler.postDelayed(this, intervalMs)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    mainHandler.removeCallbacks(tickRunnable)
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    isScreenOn = true
                    restartTickLoopIfNeeded()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = PetPreferencesRepository.getInstance(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        isScreenOn = pm.isInteractive

        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        serviceScope.launch {
            repository.state.collectLatest { prefs ->
                val prev = currentPrefs
                currentPrefs = prefs
                if (!prefs.enabled || !Settings.canDrawOverlays(this@PetOverlayService)) {
                    clearOverlayViews()
                    stopSelf()
                    return@collectLatest
                }
                updateNotification()
                if (prefs.petsHidden) {
                    clearOverlayViews()
                    mainHandler.removeCallbacks(tickRunnable)
                } else {
                    val rosterChanged = prev.slots != prefs.slots ||
                        prev.numberOnScreen != prefs.numberOnScreen ||
                        prev.petsHidden != prefs.petsHidden ||
                        petViews.isEmpty()
                    if (rosterChanged) {
                        rebuildActivePets(forceReshuffle = false)
                    }
                    if (prev.timeChange != prefs.timeChange) {
                        scheduleReshuffle()
                    }
                    restartTickLoopIfNeeded()
                }
            }
        }

        serviceScope.launch {
            repository.commandEvents.collectLatest { cmd ->
                if (cmd != null && cmd.seq > lastCommandSeq) {
                    lastCommandSeq = cmd.seq
                    val screenW = resources.displayMetrics.widthPixels.toFloat()
                    val screenH = resources.displayMetrics.heightPixels.toFloat()
                    val spritePx = computeSpriteSizePx(currentPrefs.size).toFloat()
                    petViews.forEach { entry ->
                        entry.mascot.applyCommand(cmd.action, screenW, screenH, spritePx)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                repository.setEnabled(false)
                clearOverlayViews()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_HIDE -> {
                repository.setPetsHidden(!currentPrefs.petsHidden)
            }
            else -> {
                if (!Settings.canDrawOverlays(this) || !repository.state.value.enabled) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                currentPrefs = repository.state.value
                if (!currentPrefs.petsHidden && petViews.isEmpty()) {
                    rebuildActivePets(forceReshuffle = false)
                }
                scheduleReshuffle()
                restartTickLoopIfNeeded()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(screenReceiver) }
        clearOverlayViews()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun computeSpriteSizePx(scale: Float): Int {
        val baseDp = 76f
        val density = resources.displayMetrics.density
        return (baseDp * density * scale).roundToInt().coerceAtLeast(64)
    }

    private fun scheduleReshuffle() {
        mainHandler.removeCallbacks(reshuffleRunnable)
        val intervalMs = currentPrefs.timeChange.coerceIn(1, 60) * 60_000L
        mainHandler.postDelayed(reshuffleRunnable, intervalMs)
    }

    private fun restartTickLoopIfNeeded() {
        mainHandler.removeCallbacks(tickRunnable)
        if (isScreenOn && currentPrefs.enabled && !currentPrefs.petsHidden && petViews.isNotEmpty()) {
            mainHandler.post(tickRunnable)
        }
    }

    private fun rebuildActivePets(forceReshuffle: Boolean) {
        if (!Settings.canDrawOverlays(this)) return
        val filled = currentPrefs.slots.filterNotNull()
        if (filled.isEmpty() || currentPrefs.petsHidden || !currentPrefs.enabled) {
            clearOverlayViews()
            return
        }

        val targetCount = minOf(currentPrefs.numberOnScreen, filled.size)
        val existingIds = petViews.map { it.mascot.petId }
        if (!forceReshuffle && petViews.size == targetCount && existingIds.all { it in filled }) {
            return
        }

        clearOverlayViews()
        val chosenIds = filled.shuffled(Random.Default).take(targetCount)
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = resources.displayMetrics.heightPixels.toFloat()
        val spritePx = computeSpriteSizePx(currentPrefs.size)

        chosenIds.forEachIndexed { index, petId ->
            val startX = ((screenW - spritePx) * ((index + 1f) / (chosenIds.size + 1f)))
                .coerceAtLeast(0f)
            val startY = (screenH * 0.12f) + (index * 36f)
            val mascot = MascotInstance(
                instanceKey = "overlay_${index}_$petId",
                petId = petId,
                initialX = startX,
                initialY = startY
            )
            val params = WindowManager.LayoutParams(
                spritePx,
                spritePx,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = mascot.x.roundToInt()
                y = mascot.y.roundToInt()
            }
            val view = PetOverlayView(this, mascot, params)
            runCatching {
                windowManager.addView(view, params)
                petViews.add(OverlayPetEntry(mascot, view, params))
            }
        }
    }

    private fun clearOverlayViews() {
        dismissQuickMenu()
        petViews.forEach { entry ->
            runCatching { windowManager.removeView(entry.view) }
        }
        petViews.clear()
    }

    private fun showQuickMenuAbove(entry: OverlayPetEntry) {
        dismissQuickMenu()
        val density = resources.displayMetrics.density
        val menuWidth = (186 * density).roundToInt()
        val menuHeight = (48 * density).roundToInt()
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding((10 * density).roundToInt(), (6 * density).roundToInt(), (10 * density).roundToInt(), (6 * density).roundToInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#111318"))
                cornerRadius = 24f * density
                setStroke((1.2f * density).roundToInt(), Color.parseColor("#B68D40"))
            }
            elevation = 10f * density
        }

        fun createButton(label: String, targetRoute: String): TextView {
            return TextView(this).apply {
                text = label
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding((12 * density).roundToInt(), (6 * density).roundToInt(), (12 * density).roundToInt(), (6 * density).roundToInt())
                setOnClickListener {
                    dismissQuickMenu()
                    val launchIntent = Intent(this@PetOverlayService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(MainActivity.EXTRA_TARGET_TAB, targetRoute)
                    }
                    startActivity(launchIntent)
                }
            }
        }

        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#353A47"))
            layoutParams = LinearLayout.LayoutParams((1 * density).roundToInt(), (18 * density).roundToInt()).apply {
                marginStart = (4 * density).roundToInt()
                marginEnd = (4 * density).roundToInt()
            }
        }

        container.addView(createButton("Home", MainActivity.TAB_HOME))
        container.addView(divider)
        container.addView(createButton("Settings", MainActivity.TAB_SETTINGS))

        val desiredX = (entry.params.x + entry.params.width / 2 - menuWidth / 2)
            .coerceIn((12 * density).roundToInt(), (screenW - menuWidth - 12 * density).roundToInt().coerceAtLeast(12))
        val desiredY = (entry.params.y - menuHeight - (8 * density).roundToInt())
            .coerceIn((24 * density).roundToInt(), (screenH - menuHeight - 24 * density).roundToInt().coerceAtLeast(24))

        val menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = desiredX
            y = desiredY
        }

        runCatching {
            windowManager.addView(container, menuParams)
            quickMenuView = container
            val dismissTask = Runnable { dismissQuickMenu() }
            quickMenuDismissRunnable = dismissTask
            mainHandler.postDelayed(dismissTask, 5000L)
        }
    }

    private fun dismissQuickMenu() {
        quickMenuDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        quickMenuDismissRunnable = null
        quickMenuView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        quickMenuView = null
    }

    private inner class PetOverlayView(
        context: Context,
        private val mascot: MascotInstance,
        private val params: WindowManager.LayoutParams
    ) : View(context) {

        private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val drawMatrix = Matrix()

        private var velocityTracker: VelocityTracker? = null
        private var downRawX = 0f
        private var downRawY = 0f
        private var lastRawX = 0f
        private var lastRawY = 0f
        private var maxDragDistance = 0f
        private var downTimeMs = 0L

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val bmp = PetSpriteCache.getBitmapFrame(context, mascot.petId, mascot.currentFrameIndex)
            val sx = width.toFloat() / bmp.width.toFloat()
            val sy = height.toFloat() / bmp.height.toFloat()
            drawMatrix.reset()
            if (mascot.facingRight) {
                drawMatrix.setScale(-sx, sy)
                drawMatrix.postTranslate(width.toFloat(), 0f)
            } else {
                drawMatrix.setScale(sx, sy)
            }
            canvas.drawBitmap(bmp, drawMatrix, bitmapPaint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dismissQuickMenu()
                    downRawX = event.rawX
                    downRawY = event.rawY
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    maxDragDistance = 0f
                    downTimeMs = SystemClock.uptimeMillis()
                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain().apply { addMovement(event) }
                    mascot.startDrag()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addMovement(event)
                    val dx = event.rawX - lastRawX
                    val dy = event.rawY - lastRawY
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    val totalDist = hypot(event.rawX - downRawX, event.rawY - downRawY)
                    if (totalDist > maxDragDistance) maxDragDistance = totalDist

                    val screenW = resources.displayMetrics.widthPixels.toFloat()
                    val screenH = resources.displayMetrics.heightPixels.toFloat()
                    val spritePx = width.toFloat().coerceAtLeast(64f)
                    mascot.dragBy(dx, dy, screenW, screenH, spritePx)
                    params.x = mascot.x.roundToInt()
                    params.y = mascot.y.roundToInt()
                    runCatching { windowManager.updateViewLayout(this, params) }
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    velocityTracker?.addMovement(event)
                    velocityTracker?.computeCurrentVelocity(1000)
                    val vx = velocityTracker?.xVelocity ?: 0f
                    val vy = velocityTracker?.yVelocity ?: 0f
                    velocityTracker?.recycle()
                    velocityTracker = null

                    val elapsed = SystemClock.uptimeMillis() - downTimeMs
                    val tapSlop = 6f * resources.displayMetrics.density
                    if (maxDragDistance < tapSlop && elapsed < 400L) {
                        mascot.endDrag(0f, 0f)
                        petViews.firstOrNull { it.view === this }?.let { showQuickMenuAbove(it) }
                    } else {
                        mascot.endDrag(vx, vy)
                    }
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Desktop Companions",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controls for active desktop companions on screen"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleLabel = if (currentPrefs.petsHidden) "Show" else "Hide"
        val toggleIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PetOverlayService::class.java).apply { action = ACTION_TOGGLE_HIDE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, PetOverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activeCount = currentPrefs.activeDisplayCount()
        val subtitle = if (currentPrefs.petsHidden) {
            "Companions hidden · Tap Show to resume"
        } else {
            "$activeCount companion${if (activeCount == 1) "" else "s"} active on screen"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Pets On Screen")
            .setContentText(subtitle)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, toggleLabel, toggleIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "pet_overlay_channel"
        private const val NOTIFICATION_ID = 4012
        const val ACTION_TOGGLE_HIDE = "com.petsonscreen.app.action.TOGGLE_HIDE"
        const val ACTION_STOP = "com.petsonscreen.app.action.STOP"

        fun syncServiceState(context: Context, prefs: PetAppPreferences) {
            val appCtx = context.applicationContext
            if (prefs.enabled && Settings.canDrawOverlays(appCtx)) {
                val intent = Intent(appCtx, PetOverlayService::class.java)
                ContextCompat.startForegroundService(appCtx, intent)
            } else {
                appCtx.stopService(Intent(appCtx, PetOverlayService::class.java))
            }
        }
    }
}
