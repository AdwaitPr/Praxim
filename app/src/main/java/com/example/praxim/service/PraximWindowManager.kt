package com.example.praxim.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.TypedValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

enum class WindowState {
    RESTING,
    ANIMATING,
    ACTIVE
}

data class InsetsData(val top: Int = 0, val bottom: Int = 0)

class PraximWindowManager(
    private val context: Context,
    private val lifecycleHost: PraximLifecycleHost
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null

    private val _windowState = MutableStateFlow(WindowState.RESTING)
    val windowState = _windowState.asStateFlow()

    private val _insets = MutableStateFlow(InsetsData())
    val insets = _insets.asStateFlow()

    fun mount(content: @Composable () -> Unit) {
        if (composeView != null) return

        composeView = ComposeView(context).apply {
            setContent {
                content()
            }
        }

        composeView?.let { view ->
            lifecycleHost.attachToView(view)

            // Phase 3: IME & Insets Listener
            ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
                val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
                val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                _insets.value = InsetsData(top = systemBars.top, bottom = ime.bottom)
                windowInsets
            }

            val params = getLayoutParamsForState(WindowState.RESTING)
            try {
                windowManager.addView(view, params)
                _windowState.value = WindowState.RESTING
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun expand() {
        if (_windowState.value != WindowState.RESTING) return
        composeView?.let { view ->
            // Phase 1 of Expansion: Instantly snap to MATCH_PARENT but disable touches
            _windowState.value = WindowState.ANIMATING
            windowManager.updateViewLayout(view, getLayoutParamsForState(WindowState.ANIMATING))

            // Wait for visual animation to potentially start/finish (managed outside/delays)
            kotlinx.coroutines.delay(300)

            // Phase 2 of Expansion: Enable touches, allow IME overlay
            _windowState.value = WindowState.ACTIVE
            windowManager.updateViewLayout(view, getLayoutParamsForState(WindowState.ACTIVE))
        }
    }

    suspend fun collapse() {
        if (_windowState.value == WindowState.RESTING) return
        composeView?.let { view ->
            // Phase 1 of Collapse: Disable touches during visual collapse
            _windowState.value = WindowState.ANIMATING
            windowManager.updateViewLayout(view, getLayoutParamsForState(WindowState.ANIMATING))

            // Wait for Compose visual animation to finish
            kotlinx.coroutines.delay(300)

            // Phase 2 of Collapse: Snap back to pill dimensions
            _windowState.value = WindowState.RESTING
            windowManager.updateViewLayout(view, getLayoutParamsForState(WindowState.RESTING))
        }
    }

    fun destroy() {
        composeView?.let { view ->
            try {
                windowManager.removeViewImmediate(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        composeView = null
    }

    private fun getLayoutParamsForState(state: WindowState): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val flags = when (state) {
            WindowState.RESTING -> {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            }
            WindowState.ANIMATING -> {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            }
            WindowState.ACTIVE -> {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            }
        }

        val width = if (state == WindowState.RESTING) {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, context.resources.displayMetrics).toInt()
        } else {
            WindowManager.LayoutParams.MATCH_PARENT
        }

        val height = if (state == WindowState.RESTING) {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64f, context.resources.displayMetrics).toInt()
        } else {
            WindowManager.LayoutParams.MATCH_PARENT
        }

        return WindowManager.LayoutParams(width, height, type, flags, PixelFormat.TRANSLUCENT).apply {
            gravity = if (state == WindowState.RESTING) Gravity.CENTER_VERTICAL or Gravity.END else Gravity.CENTER

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For Android 11+ Untrusted Touch bypass during ANIMATING state
                if (state == WindowState.ANIMATING && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                     alpha = 0.8f // Aggressively clamp to 0.8f
                } else if (state == WindowState.ANIMATING) {
                     alpha = 0.8f
                } else {
                     alpha = 1.0f
                }
            } else if (state == WindowState.ANIMATING) {
                alpha = 0.8f
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }
}