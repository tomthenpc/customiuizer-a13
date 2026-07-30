package tv.withaibuild.customiuizer.mods.utils

import android.app.WallpaperColors
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.WindowManager
import tv.withaibuild.customiuizer.mods.GlobalActions
import tv.withaibuild.customiuizer.utils.Helpers

/**
 * Owns the Android 13 lock-screen artwork state while keeping the original MIUI 14 hook
 * targets and preference semantics in [tv.withaibuild.customiuizer.mods.SystemUILockScreenHooks].
 */
object LockScreenAlbumArtController {

    private var miuiThemeUtilsClass: Class<*>? = null

    @JvmStatic
    fun setMiuiThemeUtilsClass(clazz: Class<*>?) {
        miuiThemeUtilsClass = clazz
    }

    @JvmStatic
    fun applyTo(view: View): Boolean {
        val art = getAlbumArt()
        if (art != null) {
            view.background = BitmapDrawable(view.resources, art)
        }
        return art != null
    }

    @JvmStatic
    fun update(
        context: Context,
        art: Bitmap?,
        blur: Int,
        rescale: Int,
        grayscale: Boolean
    ) {
        val clazz = miuiThemeUtilsClass ?: return
        val previous = getSource()
        try {
            if (art == null && previous == null) return
            if (art != null && previous != null && art.sameAs(previous)) return
        } catch (_: Throwable) {
        }

        XposedHelpers.setAdditionalStaticField(clazz, "mAlbumArtSource", art)
        val processed = processAlbumArt(
            context,
            if (art != null && blur > 0) Helpers.fastBlur(art, blur + 1) else art,
            rescale,
            grayscale
        )
        XposedHelpers.setAdditionalStaticField(clazz, "mAlbumArt", processed)
        publish(context, processed)
    }

    @JvmStatic
    fun clear(context: Context?, notify: Boolean) {
        val clazz = miuiThemeUtilsClass ?: return
        XposedHelpers.setAdditionalStaticField(clazz, "mAlbumArtSource", null)
        XposedHelpers.setAdditionalStaticField(clazz, "mAlbumArt", null)
        if (notify && context != null) sendUpdateBroadcast(context)
    }

    private fun getSource(): Bitmap? {
        val clazz = miuiThemeUtilsClass ?: return null
        return XposedHelpers.getAdditionalStaticField(clazz, "mAlbumArtSource") as? Bitmap
    }

    private fun getAlbumArt(): Bitmap? {
        val clazz = miuiThemeUtilsClass ?: return null
        return XposedHelpers.getAdditionalStaticField(clazz, "mAlbumArt") as? Bitmap
    }

    private fun processAlbumArt(
        context: Context,
        bitmap: Bitmap?,
        rescale: Int,
        grayscale: Boolean
    ): Bitmap? {
        if (bitmap == null) return null
        if (rescale == 1 && !grayscale) return bitmap

        val paint = Paint()
        val transformation = Matrix()
        var width = 0
        var height = 0

        if (grayscale) {
            width = bitmap.width
            height = bitmap.height

            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        }

        if (rescale != 1) {
            val display =
                (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
            val point = Point()
            display?.getRealSize(point)
            width = point.x
            height = point.y

            val originalWidth = bitmap.width.toFloat()
            val originalHeight = bitmap.height.toFloat()
            val scale = if (rescale == 2) {
                minOf(width / originalWidth, height / originalHeight)
            } else {
                maxOf(width / originalWidth, height / originalHeight)
            }
            val xTranslation = (width - originalWidth * scale) / 2f
            val yTranslation = (height - originalHeight * scale) / 2f
            transformation.postTranslate(xTranslation, yTranslation)
            transformation.preScale(scale, scale)
            paint.isFilterBitmap = true
        }

        val processed = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(processed).drawBitmap(bitmap, transformation, paint)
        return processed
    }

    private fun publish(context: Context, processed: Bitmap?) {
        sendUpdateBroadcast(context)
        if (processed == null) return

        val updateFakeWallpaper = Intent("miui.intent.action.LOCK_WALLPAPER_CHANGED")
        updateFakeWallpaper.setPackage("com.android.systemui")
        val colors = WallpaperColors.fromBitmap(processed)
        updateFakeWallpaper.putExtra("is_wallpaper_color_light", (colors.colorHints and 1) == 1)
        context.sendBroadcast(updateFakeWallpaper)
    }

    private fun sendUpdateBroadcast(context: Context) {
        val update = Intent(GlobalActions.EVENT_PREFIX + "UPDATE_LS_ALBUM_ART")
        update.setPackage("com.android.systemui")
        context.sendBroadcast(update)
    }
}
