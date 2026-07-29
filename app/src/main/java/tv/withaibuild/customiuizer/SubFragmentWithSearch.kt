package tv.withaibuild.customiuizer

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import tv.withaibuild.customiuizer.utils.AppDataAdapter
import tv.withaibuild.customiuizer.utils.Helpers
import tv.withaibuild.customiuizer.utils.LockedAppAdapter
import tv.withaibuild.customiuizer.utils.PrivacyAppAdapter
import tv.withaibuild.customiuizer.utils.ResolveInfoAdapter

open class SubFragmentWithSearch : SubFragment() {

    @JvmField
    var listView: ListView? = null
    private var searchView: View? = null
    private var isSearchFocused = false
    private var textInput: TextView? = null

    fun setActionModeStyle(searchView: View?) {
        val ctx = getValidContext() ?: return
        val isNight = Helpers.isNightMode(ctx)
        if (searchView != null) try {
            searchView.setSaveFromParentEnabled(false)
            val inputIcon = searchView.findViewById<ImageView>(R.id.inputIcon)
            inputIcon?.setImageResource(
                resources.getIdentifier(
                    if (isNight) "edit_text_search_dark" else "edit_text_search",
                    "drawable",
                    "miui"
                )
            )
            inputIcon?.setImageTintList(
                ColorStateList.valueOf(
                    resources.getColor(R.color.color_on_surface_variant, ctx.theme)
                )
            )
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val view = view ?: return

        searchView = view.findViewById(R.id.searchView)
        setActionModeStyle(searchView)
        textInput = searchView?.findViewById(android.R.id.input)

        textInput?.setOnFocusChangeListener { _, hasFocus ->
            isSearchFocused = hasFocus
        }
        textInput?.setOnClickListener { v ->
            isSearchFocused = v.hasFocus()
        }
        textInput?.setOnEditorActionListener { v, _, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                val act = activity as? AppCompatActivity
                if (act != null) Helpers.hideKeyboard(act, v)
                listView?.requestFocus()
                true
            } else {
                false
            }
        }
        textInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                applyFilter(s.toString().trim())
            }
        })

        listView = view.findViewById(android.R.id.list)
        listView?.setOnTouchListener { v, event ->
            if (isSearchFocused) {
                isSearchFocused = false
                val act = activity as? AppCompatActivity
                if (act != null) Helpers.hideKeyboard(act, v)
            }
            false
        }
    }

    protected fun applyFilter(filter: String) {
        val adapter = listView?.adapter ?: return
        when (adapter) {
            is AppDataAdapter -> adapter.filter.filter(filter)
            is PrivacyAppAdapter -> adapter.filter.filter(filter)
            is LockedAppAdapter -> adapter.filter.filter(filter)
            is ResolveInfoAdapter -> adapter.filter.filter(filter)
            else -> {}
        }
    }
}
