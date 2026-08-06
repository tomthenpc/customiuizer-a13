package tv.withaibuild.customiuizer.mods

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import java.lang.reflect.Field
import java.lang.reflect.Modifier

private fun setField(obj: Any, fieldName: String, value: Any?) {
    try {
        val field = View::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        try {
            field.set(obj, value)
        } catch (iae: IllegalAccessException) {
            if (Modifier.isFinal(field.modifiers)) {
                try {
                    val modifiersField = Field::class.java.getDeclaredField("modifiers")
                    modifiersField.isAccessible = true
                    modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
                    field.set(obj, value)
                } catch (t: Throwable) {
                    // ignore
                }
            }
        }
    } catch (t: Throwable) {
        // ignore
    }
}

private fun setIntField(obj: Any, fieldName: String, value: Int) {
    try {
        val field = View::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        try {
            field.setInt(obj, value)
        } catch (iae: IllegalAccessException) {
            if (Modifier.isFinal(field.modifiers)) {
                try {
                    val modifiersField = Field::class.java.getDeclaredField("modifiers")
                    modifiersField.isAccessible = true
                    modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
                    field.setInt(obj, value)
                } catch (t: Throwable) {
                    // ignore
                }
            }
        }
    } catch (t: Throwable) {
        // ignore
    }
}

open class RecordingTextView(context: Context?) : TextView(context) {
    var maxWidthCalls = mutableListOf<Int>()

    init {
        setField(this, "mContext", context)
    }

    override fun setMaxWidth(maxPixels: Int) {
        maxWidthCalls.add(maxPixels)
        super.setMaxWidth(maxPixels)
    }
}

open class RecordingMenuItemView(context: Context?) : RecordingTextView(context) {

    var storedOnClickListener: View.OnClickListener? = null

    init {
        setIntField(this, "mID", 12345)
    }

    override fun setOnClickListener(l: View.OnClickListener?) {
        storedOnClickListener = l
    }
}

open class RecordingMenuContainer(context: Context?) : LinearLayout(context) {

    val addedChildren = mutableListOf<Pair<View, ViewGroup.LayoutParams?>>()

    init {
        setField(this, "mContext", context)
    }

    override fun addView(child: View?) {
        if (child != null) {
            addedChildren.add(child to null)
            super.addView(child)
        }
    }

    override fun addView(child: View?, index: Int) {
        if (child != null) {
            addedChildren.add(child to null)
            super.addView(child, index)
        }
    }

    override fun addView(child: View?, width: Int, height: Int) {
        if (child != null) {
            addedChildren.add(child to null)
            super.addView(child, width, height)
        }
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        if (child != null) {
            addedChildren.add(child to params)
            super.addView(child, params)
        }
    }
}
