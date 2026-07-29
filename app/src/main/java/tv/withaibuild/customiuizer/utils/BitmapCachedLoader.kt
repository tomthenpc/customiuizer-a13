@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
package tv.withaibuild.customiuizer.utils

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.AsyncTask
import android.widget.ImageView
import java.lang.ref.WeakReference

@SuppressLint("StaticFieldLeak")
@Suppress("DEPRECATION")
internal class BitmapCachedLoader(
    target: Any,
    info: Any,
    context: Context
) : AsyncTask<Void, Void, Bitmap?>() {

    private val targetRef = WeakReference(target as ImageView)
    private val appInfo = WeakReference(info as AppData)
    private val ctx: Context = context.applicationContext
    private val theTag: Int = (target as ImageView).tag as? Int ?: -1

    override fun doInBackground(vararg params: Void?): Bitmap? {
        val ad = appInfo.get() ?: return null

        if (ad.pkgName.isNullOrEmpty() && ad.actName.isNullOrEmpty()) return null

        val pkgName = ad.pkgName ?: return null
        val pkgMgr: PackageManager = ctx.packageManager

        val icon = try {
            val actName = ad.actName
            if (!actName.isNullOrEmpty() && actName != "-") {
                val component = ComponentName(pkgName, actName)
                if (pkgMgr.getActivityInfo(component, PackageManager.MATCH_ALL).icon != 0) {
                    pkgMgr.getActivityIcon(component)
                } else {
                    null
                }
            } else null
        } catch (t: Throwable) {
            null
        } ?: try {
            if (pkgMgr.getApplicationInfo(pkgName, PackageManager.MATCH_DISABLED_COMPONENTS).icon != 0) {
                pkgMgr.getApplicationIcon(pkgName)
            } else null
        } catch (t: Throwable) {
            null
        } ?: return null

        val cacheKey = buildString {
            append(pkgName)
            val actName = ad.actName
            if (!actName.isNullOrEmpty() && actName != "-") {
                append('|').append(actName)
            }
        }

        val newIconSize = ctx.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        val bmp = Bitmap.createBitmap(newIconSize, newIconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        icon.setBounds(0, 0, newIconSize, newIconSize)
        icon.draw(canvas)

        if (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() > 8 * 1024 * 1024) {
            Helpers.memoryCache.put(cacheKey, bmp)
        } else {
            Runtime.getRuntime().gc()
        }

        return bmp
    }

    override fun onPostExecute(bmp: Bitmap?) {
        val itemIcon = targetRef.get() ?: return
        if (bmp == null) return
        if (theTag == itemIcon.tag as? Int) {
            val drawable = itemIcon.drawable
            if (drawable is TransitionDrawable) {
                drawable.addLayer(BitmapDrawable(ctx.resources, bmp))
                drawable.startTransition(200)
            }
        }
    }
}
