package name.monwf.customiuizer.subs

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import name.monwf.customiuizer.R
import name.monwf.customiuizer.SubFragmentWithSearch
import name.monwf.customiuizer.utils.AppHelper
import name.monwf.customiuizer.utils.ResolveInfoAdapter
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayList

@Suppress("DEPRECATION")
class ShortcutSelector : SubFragmentWithSearch() {

    private var key: String? = null
    private var keyContents: String? = null
    private lateinit var shortcuts: ArrayList<ResolveInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        this.padded = false
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val args = arguments ?: return
        key = args.getString("key")
        key?.let { keyContents = AppHelper.getStringOfAppPrefs(it, null) }

        val shortcutIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)
        val pm = activity?.packageManager ?: return
        shortcuts = ArrayList(pm.queryIntentActivities(shortcutIntent, 0))

        val ctx = context ?: return
        listView?.adapter = ResolveInfoAdapter(ctx, shortcuts)
        listView?.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val createShortcutIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)
            val app = parent.getItemAtPosition(position) as? ResolveInfo ?: return@OnItemClickListener
            val cn = ComponentName(app.activityInfo.packageName, app.activityInfo.name)
            createShortcutIntent.component = cn
            keyContents = app.activityInfo.packageName + "|" + app.activityInfo.name
            startActivityForResult(createShortcutIntent, 7350)
        }

        view?.findViewById<View>(R.id.am_progressBar)?.visibility = View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 7350 && resultCode == Activity.RESULT_OK) {
            val ctx = context
            if (ctx == null) {
                super.onActivityResult(requestCode, resultCode, data)
                return
            }

            val iconResId = data?.getParcelableExtra<Intent.ShortcutIconResource>(Intent.EXTRA_SHORTCUT_ICON_RESOURCE)

            var icon: Bitmap? = null
            if (iconResId != null) try {
                val mContext = ctx.createPackageContext(iconResId.packageName, Context.CONTEXT_IGNORE_SECURITY)
                val resId = mContext.resources.getIdentifier(iconResId.resourceName, "drawable", iconResId.packageName)
                icon = BitmapFactory.decodeResource(mContext.resources, resId)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            if (icon == null) icon = data?.getParcelableExtra<Bitmap>(Intent.EXTRA_SHORTCUT_ICON)

            val intent = Intent(ctx, this.javaClass)

            if (icon != null && key != null) try {
                val dir = ctx.filesDir?.absolutePath + "/shortcuts"
                val fileName = "$dir/tmp.png"

                val shortcutsDir = File(dir)
                shortcutsDir.mkdirs()
                val shortcutFileName = File(fileName)
                FileOutputStream(shortcutFileName, false).use { shortcutOutStream ->
                    if (icon.compress(Bitmap.CompressFormat.PNG, 100, shortcutOutStream)) {
                        intent.putExtra("shortcut_icon", fileName)
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            intent.putExtra("shortcut_contents", keyContents)
            intent.putExtra("shortcut_name", data?.getStringExtra(Intent.EXTRA_SHORTCUT_NAME))
            intent.putExtra("shortcut_intent", data?.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT))
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
            finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
