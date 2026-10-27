package io.github.goodwind.hypervoice

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** The colours of the tvOS-like set the other Hyper apps use: dark surfaces, white text, a near-white plate on the focused control. */
object Ink {
    const val BACKGROUND = 0xFF111113.toInt()
    const val PANEL = 0xFF242427.toInt()
    const val LABEL = 0xFFFFFFFF.toInt()
    const val SECONDARY = 0xFFA5A5AD.toInt()
    const val FILL = 0xFF343438.toInt()
    const val FOCUS = 0xFFF5F5F7.toInt()
    const val ON_FOCUS = 0xFF1D1D1F.toInt()
    const val RED = 0xFFFF453A.toInt()
    const val ORANGE = 0xFFFF9F0A.toInt()
    const val GREEN = 0xFF32D74B.toInt()
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun Context.plate(color: Int, radius: Int) = GradientDrawable().apply {
    setColor(color)
    cornerRadius = dp(radius).toFloat()
}

fun Context.label(text: CharSequence, size: Float, color: Int = Ink.LABEL, bold: Boolean = false) = TextView(this).apply {
    this.text = text
    textSize = size
    setTextColor(color)
    if (bold) setTypeface(typeface, Typeface.BOLD)
}

/** A row the remote can focus and press: a name, its state (or a chevron for a link) and a near-white plate while focused. */
class FocusRow(context: Context, name: String, state: String?, onClick: () -> Unit) : LinearLayout(context) {
    private val name = context.label(name, 16f)
    private val state = context.label(state.orEmpty(), 16f, Ink.SECONDARY)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isClickable = true
        minimumHeight = context.dp(48)
        setPadding(context.dp(16), context.dp(6), context.dp(16), context.dp(6))
        addView(this.name, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(this.state)
        setOnClickListener { onClick() }
        onFocusChangeListener = View.OnFocusChangeListener { _, _ -> paint() }
        paint()
    }

    fun setState(text: String) { state.text = text }

    private fun paint() {
        background = if (hasFocus()) context.plate(Ink.FOCUS, 12) else null
        name.setTextColor(if (hasFocus()) Ink.ON_FOCUS else Ink.LABEL)
        state.setTextColor(if (hasFocus()) Ink.ON_FOCUS else Ink.SECONDARY)
    }
}

/** A short action: a grey plate that goes near-white under focus. */
class FocusButton(context: Context, text: String, onClick: () -> Unit) : TextView(context) {
    init {
        this.text = text
        textSize = 16f
        gravity = Gravity.CENTER
        isFocusable = true
        isClickable = true
        setPadding(context.dp(20), context.dp(10), context.dp(20), context.dp(10))
        setOnClickListener { onClick() }
        onFocusChangeListener = View.OnFocusChangeListener { _, _ -> paint() }
        paint()
    }

    private fun paint() {
        background = context.plate(if (hasFocus()) Ink.FOCUS else Ink.FILL, 14)
        setTextColor(if (hasFocus()) Ink.ON_FOCUS else Ink.LABEL)
    }
}
