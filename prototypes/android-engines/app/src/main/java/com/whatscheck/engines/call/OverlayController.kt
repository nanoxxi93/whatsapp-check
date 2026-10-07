package com.whatscheck.engines.call

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.whatscheck.engines.Prefs
import kotlin.math.abs

/**
 * Aviso flotante sobre la pantalla de llamada (RF-04). Views simples, sin Compose: vive en una ventana
 * TYPE_APPLICATION_OVERLAY creada desde un servicio. Centrado en la pantalla (Gravity.CENTER) para no tapar
 * el número arriba ni los botones abajo, y evitar colisionar con el banner flotante de llamada si se está en otra app;
 * se arrastra, se cierra con un toque y se oculta automáticamente (10 s en verificación, o la duración configurada
 * en ajustes tras mostrar el resultado).
 * Todas las llamadas deben hacerse en el hilo principal.
 */
class OverlayController(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())
    private var root: LinearLayout? = null
    private var title: TextView? = null
    private val hide = Runnable { dismiss() }
    private var activeDurationMs: Long = DEFAULT_RESULT_DURATION_MS

    enum class Kind(val text: String, val color: Int) {
        CHECKING("⏳  Verificando WhatsApp…", Color.parseColor("#37474F")),
        HAS("✅  Tiene WhatsApp", Color.parseColor("#1B5E20")),
        NO("⚠️  No tiene WhatsApp", Color.parseColor("#E65100")),
        UNKNOWN("❔  No se pudo verificar", Color.parseColor("#455A64")),
    }

    fun canShow(): Boolean = Settings.canDrawOverlays(context)

    /** Muestra el aviso (o lo actualiza si ya está visible). Devuelve el instante en que quedó visible. */
    fun show(kind: Kind, subtitle: String? = null, durationMs: Long? = null): Long {
        val view = root ?: create()
        title?.text = kind.text
        (view.background as GradientDrawable).setColor(kind.color)
        (view.getChildAt(1) as TextView).text = subtitle ?: "WhatsCheck · toca para cerrar"
        main.removeCallbacks(hide)
        val delay = when (kind) {
            Kind.CHECKING -> CHECKING_TIMEOUT_MS
            else -> durationMs ?: (Prefs.overlayDurationSeconds(context) * 1_000L)
        }
        activeDurationMs = delay
        main.postDelayed(hide, delay)
        return SystemClock.elapsedRealtime()
    }

    fun dismiss() {
        main.removeCallbacks(hide)
        root?.let { runCatching { wm.removeView(it) } }
        root = null
        title = null
    }

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, context.resources.displayMetrics).toInt()

    private fun create(): LinearLayout {
        val big = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        }
        val small = TextView(context).apply {
            setTextColor(Color.parseColor("#DDFFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18f), dp(12f), dp(18f), dp(12f))
            background = GradientDrawable().apply { cornerRadius = dp(18f).toFloat() }
            elevation = dp(8f).toFloat()
            addView(big)
            addView(small)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }
        box.setOnTouchListener(DragOrTap(params))
        wm.addView(box, params)
        root = box
        title = big
        return box
    }

    /** Arrastrar mueve el aviso; un toque (sin arrastre) lo cierra. */
    private inner class DragOrTap(private val params: WindowManager.LayoutParams) : View.OnTouchListener {
        private var startX = 0f
        private var startY = 0f
        private var originX = 0
        private var originY = 0
        private var moved = false

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = e.rawX; startY = e.rawY; originX = params.x; originY = params.y; moved = false
                    main.removeCallbacks(hide)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - startX
                    val dy = e.rawY - startY
                    if (abs(dx) > dp(6f) || abs(dy) > dp(6f)) moved = true
                    if (moved) {
                        params.x = originX + dx.toInt()
                        params.y = originY + dy.toInt()
                        runCatching { wm.updateViewLayout(v, params) }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        dismiss()
                    } else {
                        main.postDelayed(hide, activeDurationMs)
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    main.postDelayed(hide, activeDurationMs)
                }
            }
            return true
        }
    }

    companion object {
        const val CHECKING_TIMEOUT_MS = 10_000L
        const val DEFAULT_RESULT_DURATION_MS = 8_000L
    }
}
