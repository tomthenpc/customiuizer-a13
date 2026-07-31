package tv.withaibuild.customiuizer.mods

import android.graphics.Typeface
import android.widget.TextView

/**
 * Manages network-speed text boldness without losing the MIUI typeface family
 * or accumulating Bold style on repeated re-applications.
 *
 * Strategy:
 * 1. Derive an unbolded base typeface from the current [TextView.typeface].
 * 2. If the current typeface is already bold (from a previous apply or the
 *    system), strip the Bold bit to recover the base family.
 * 3. Re-apply Bold by creating a Typeface from that base family with the
 *    BOLD or BOLD_ITALIC style.
 * 4. When no base typeface is available yet (view not fully initialized or
 *    system has not applied a TextAppearance), fall back to
 *    [TextPaint.isFakeBoldText] to avoid forcing a default bold font.
 * 5. Track the last applied typeface and fake-bold state so repeated calls on
 *    the same view with unchanged settings are no-ops.
 */
internal object NetSpeedTypefaceHelper {

    /**
     * Per-TextView state. Stored in the view's tag so it follows the view
     * lifecycle and never holds a stale Context/View.
     */
    data class State(
        var baseTypeface: Typeface? = null,
        var lastAppliedTypeface: Typeface? = null,
        var lastDesiredBold: Boolean = false,
        var lastFakeBold: Boolean = false
    )

    /**
     * Applies the configured bold state to [textView].
     *
     * @param textView the view to style
     * @param bold whether bold should be enabled
     * @param state the [State] attached to this view
     */
    @JvmStatic
    fun apply(textView: TextView, bold: Boolean, state: State) {
        val current = textView.typeface
        val paint = textView.paint
        val currentFakeBold = paint.isFakeBoldText

        // Fast path: nothing has changed since our last apply.
        if (current === state.lastAppliedTypeface &&
            state.lastDesiredBold == bold &&
            state.lastFakeBold == currentFakeBold
        ) {
            return
        }

        if (current === state.lastAppliedTypeface) {
            // The view still holds the typeface we set, so our cached base is valid.
            val base = state.baseTypeface
            if (base == null) {
                // Fake-bold path.
                paint.isFakeBoldText = bold
                state.lastAppliedTypeface = null
                state.lastFakeBold = bold
            } else {
                val desired = if (bold) Typeface.create(base, boldStyleOf(base)) else base
                if (currentFakeBold) paint.isFakeBoldText = false
                if (current !== desired) textView.typeface = desired
                state.lastAppliedTypeface = desired
                state.lastFakeBold = false
            }
        } else {
            // Someone else (system TextAppearance, setNetworkSpeed, theme refresh,
            // or first call) has changed the typeface. Re-derive the base.
            if (current == null) {
                paint.isFakeBoldText = bold
                state.baseTypeface = null
                state.lastAppliedTypeface = null
                state.lastFakeBold = bold
            } else {
                val base = if ((current.style and Typeface.BOLD) != 0) {
                    Typeface.create(current, plainStyleOf(current))
                } else current
                state.baseTypeface = base
                val desired = if (bold) Typeface.create(base, boldStyleOf(base)) else base
                if (currentFakeBold) paint.isFakeBoldText = false
                if (current !== desired) textView.typeface = desired
                state.lastAppliedTypeface = desired
                state.lastFakeBold = false
            }
        }

        state.lastDesiredBold = bold
    }

    /**
     * Returns the plain (non-bold) style for [typeface], preserving Italic.
     *
     * Using concrete [Typeface] style constants avoids Lint
     * [WrongConstant] errors with arbitrary bitwise expressions.
     */
    private fun plainStyleOf(typeface: Typeface): Int = when {
        (typeface.style and Typeface.ITALIC) != 0 -> Typeface.ITALIC
        else -> Typeface.NORMAL
    }

    /**
     * Returns the bold style for [typeface], preserving Italic.
     */
    private fun boldStyleOf(typeface: Typeface): Int = when {
        (typeface.style and Typeface.ITALIC) != 0 -> Typeface.BOLD_ITALIC
        else -> Typeface.BOLD
    }

    /**
     * Pure style helpers for unit testing without Android view wiring.
     */
    @JvmStatic
    fun resolveBaseStyle(currentStyle: Int): Int =
        if ((currentStyle and Typeface.BOLD) != 0) currentStyle and Typeface.BOLD.inv() else currentStyle

    @JvmStatic
    fun resolveDesiredStyle(baseStyle: Int, bold: Boolean): Int =
        if (bold) baseStyle or Typeface.BOLD else baseStyle

    @JvmStatic
    fun shouldUseFakeBold(typeface: Typeface?): Boolean = typeface == null
}
