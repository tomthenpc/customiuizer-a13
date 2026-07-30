package tv.withaibuild.customiuizer.mods.utils

import android.app.WallpaperColors
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.util.LruCache
import android.view.View
import android.view.WindowManager
import tv.withaibuild.customiuizer.mods.GlobalActions
import tv.withaibuild.customiuizer.utils.Helpers
import java.lang.ref.WeakReference
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Android 13 lock-screen artwork owner. Bitmap work is serialized off the main thread and
 * only the latest request may publish into SystemUI.
 */
object LockScreenAlbumArtController {

    private const val SCREEN_RECEIVER_KEY = "systemui.lockScreenAlbumArt.screenState"

    private var miuiThemeUtilsClass: Class<*>? = null
    private var contextRef: WeakReference<Context>? = null
    private var ownerRef: WeakReference<View>? = null
    private var ownerListener: View.OnAttachStateChangeListener? = null
    private var screenReceiverRegistered = false
    private var lastTargetWidth = 0
    private var lastTargetHeight = 0

    @Volatile
    private var screenOn = true

    @Volatile
    private var ownerAttached = false

    private var pendingSource: Bitmap? = null
    private var pendingBlur = 0
    private var pendingRescale = 1
    private var pendingGrayscale = false

    private val requestGeneration = AtomicLong()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val workLock = Any()
    private var workFuture: Future<*>? = null
    private var activeKey: AlbumArtCacheKey? = null
    private var activeGeneration = 0L

    private val worker = ThreadPoolExecutor(
        1,
        1,
        15L,
        TimeUnit.SECONDS,
        ArrayBlockingQueue(1),
        { runnable ->
            Thread(
                {
                    ModuleHelper.guarded("LockScreenAlbumArtController.workerThread") {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                        runnable.run()
                    }
                },
                "Pengeek-LockscreenArtwork"
            )
        },
        ThreadPoolExecutor.AbortPolicy()
    ).apply {
        allowCoreThreadTimeOut(true)
    }

    private var cache: LruCache<AlbumArtCacheKey, Bitmap>? = null
    private var cacheBudgetBytes = 0
    private var cacheSignature: AlbumArtCacheSignature? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            ModuleHelper.guarded("LockScreenAlbumArtController.screenReceiver") {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> setScreenOn(false)
                    Intent.ACTION_SCREEN_ON -> setScreenOn(true)
                }
            }
        }
    }

    @JvmStatic
    fun setMiuiThemeUtilsClass(clazz: Class<*>?) {
        miuiThemeUtilsClass = clazz
        if (clazz == null) cancelWork()
    }

    @JvmStatic
    fun applyTo(view: View): Boolean {
        bindOwner(view)
        invalidateForSizeChange(view.width, view.height)
        val art = getAlbumArt()
        if (art != null) {
            view.background = BitmapDrawable(view.resources, art)
        } else if (canProcess()) {
            processPending()
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
        rememberContext(context)

        val previousSource = getSource()
        val sameParameters =
            blur == pendingBlur &&
                rescale == pendingRescale &&
                grayscale == pendingGrayscale
        val sameSource = previousSource === art

        pendingSource = art
        pendingBlur = blur
        pendingRescale = rescale
        pendingGrayscale = grayscale

        if (art == null && previousSource == null) return
        if (sameSource && sameParameters && (getAlbumArt() != null || hasActiveWork())) return
        if (!sameParameters) {
            cache?.evictAll()
            cacheSignature = null
        }

        XposedHelpers.setAdditionalStaticField(clazz, "mAlbumArtSource", art)
        XposedHelpers.setAdditionalStaticField(clazz, "mAlbumArt", null)
        if (art == null) {
            clear(context, true)
            return
        }

        if (canProcess()) processPending()
    }

    @JvmStatic
    fun clear(context: Context?, notify: Boolean) {
        requestGeneration.incrementAndGet()
        cancelWork()
        pendingSource = null
        activeKey = null
        cache?.evictAll()
        cacheSignature = null
        lastTargetWidth = 0
        lastTargetHeight = 0

        val clazz = miuiThemeUtilsClass ?: return
        XposedHelpers.setAdditionalStaticField(clazz, "mAlbumArtSource", null)
        XposedHelpers.setAdditionalStaticField(clazz, "mAlbumArt", null)
        if (notify && context != null) sendUpdateBroadcast(context)
    }

    private fun rememberContext(context: Context) {
        val applicationContext = context.applicationContext ?: context
        contextRef = WeakReference(applicationContext)
        val powerManager = applicationContext.getSystemService(PowerManager::class.java)
        screenOn = powerManager?.isInteractive ?: true

        if (!screenReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            screenReceiverRegistered = ModuleHelper.registerModuleReceiver(
                applicationContext,
                SCREEN_RECEIVER_KEY,
                screenReceiver,
                filter,
                Context.RECEIVER_EXPORTED
            )
        }
    }

    private fun bindOwner(view: View) {
        val current = ownerRef?.get()
        if (current === view) {
            ownerAttached = view.isAttachedToWindow
            return
        }

        val oldListener = ownerListener
        if (current != null && oldListener != null) {
            current.removeOnAttachStateChangeListener(oldListener)
        }

        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(attachedView: View) {
                ModuleHelper.guarded("LockScreenAlbumArtController.ownerAttached") {
                    if (ownerRef?.get() === attachedView) {
                        ownerAttached = true
                        if (getAlbumArt() == null) processPending()
                    }
                }
            }

            override fun onViewDetachedFromWindow(detachedView: View) {
                ModuleHelper.guarded("LockScreenAlbumArtController.ownerDetached") {
                    if (ownerRef?.get() === detachedView) {
                        ownerAttached = false
                        requestGeneration.incrementAndGet()
                        cancelWork()
                    }
                }
            }
        }
        ownerRef = WeakReference(view)
        ownerListener = listener
        ownerAttached = view.isAttachedToWindow
        view.addOnAttachStateChangeListener(listener)
    }

    private fun setScreenOn(on: Boolean) {
        if (screenOn == on) return
        screenOn = on
        if (on) {
            if (getAlbumArt() == null) processPending()
        } else {
            requestGeneration.incrementAndGet()
            cancelWork()
        }
    }

    private fun canProcess(): Boolean =
        screenOn && ownerAttached && miuiThemeUtilsClass != null

    private fun processPending() {
        if (!canProcess()) return
        val source = pendingSource ?: return
        val context = contextRef?.get() ?: return
        val target = targetSize(context)
        lastTargetWidth = target.width
        lastTargetHeight = target.height
        generate(
            context,
            source,
            pendingBlur,
            pendingRescale,
            pendingGrayscale,
            target.width,
            target.height
        )
    }

    private fun generate(
        context: Context,
        source: Bitmap,
        blur: Int,
        rescale: Int,
        grayscale: Boolean,
        targetWidth: Int,
        targetHeight: Int
    ) {
        val key = AlbumArtCacheKey(
            System.identityHashCode(source),
            source.width,
            source.height,
            targetWidth,
            targetHeight,
            blur,
            rescale,
            grayscale
        )
        synchronized(workLock) {
            if (activeKey == key && activeGeneration != 0L) return
            val generation = requestGeneration.incrementAndGet()
            workFuture?.cancel(true)
            worker.queue.clear()
            worker.purge()
            activeKey = key
            activeGeneration = generation
            try {
                workFuture = worker.submit {
                    var posted = false
                    ModuleHelper.guarded("LockScreenAlbumArtController.worker") {
                        val processed = process(
                            source,
                            blur,
                            rescale,
                            grayscale,
                            targetWidth,
                            targetHeight,
                            generation,
                            key
                        ) ?: return@guarded
                        if (!isCurrent(generation)) return@guarded

                        val colors = try {
                            WallpaperColors.fromBitmap(processed)
                        } catch (failure: Throwable) {
                            XposedHelpers.log(failure)
                            null
                        }
                        if (!isCurrent(generation)) return@guarded

                        posted = mainHandler.post {
                            ModuleHelper.guarded("LockScreenAlbumArtController.publish") {
                                if (isCurrent(generation) && canProcess() && pendingSource === source) {
                                    applyResult(context, processed, colors)
                                }
                            }
                            finishGeneration(generation)
                        }
                    }
                    if (!posted) finishGeneration(generation)
                }
            } catch (failure: RejectedExecutionException) {
                activeKey = null
                activeGeneration = 0L
                workFuture = null
                XposedHelpers.log("LockScreenAlbumArtController", failure)
            }
        }
    }

    private fun process(
        source: Bitmap,
        blur: Int,
        rescale: Int,
        grayscale: Boolean,
        targetWidth: Int,
        targetHeight: Int,
        generation: Long,
        key: AlbumArtCacheKey
    ): Bitmap? {
        if (!isCurrent(generation)) return null
        if (blur == 0 && rescale == 1 && !grayscale) return source

        val inputSize = AlbumArtPolicy.downsampleSize(
            source.width,
            source.height,
            targetWidth,
            targetHeight,
            blur,
            rescale
        )
        val outputWidth = if (rescale == 1) inputSize.width else targetWidth
        val outputHeight = if (rescale == 1) inputSize.height else targetHeight
        if (outputWidth <= 0 || outputHeight <= 0) return null

        val signature =
            AlbumArtCacheSignature(outputWidth, outputHeight, blur, rescale, grayscale)
        val artworkCache = cacheFor(signature)
        artworkCache?.get(key)?.let { return it }

        val downsampled =
            if (source.width == inputSize.width && source.height == inputSize.height) {
                source
            } else {
                Bitmap.createScaledBitmap(source, inputSize.width, inputSize.height, true)
            }
        if (!isCurrent(generation)) return null

        val blurred =
            if (blur > 0) Helpers.fastBlur(downsampled, blur + 1) ?: downsampled else downsampled
        if (!isCurrent(generation)) return null

        val processed = drawAlbumArt(
            blurred,
            rescale,
            grayscale,
            outputWidth,
            outputHeight
        ) ?: return null
        if (!isCurrent(generation)) return null

        artworkCache?.put(key, processed)
        return processed
    }

    private fun cacheFor(signature: AlbumArtCacheSignature): LruCache<AlbumArtCacheKey, Bitmap>? {
        val budget = AlbumArtPolicy.cacheBudgetBytes(signature.targetWidth, signature.targetHeight)
        if (budget <= 0) return null

        if (
            cache == null ||
            cacheBudgetBytes != budget ||
            AlbumArtPolicy.shouldRebuildCache(cacheSignature, signature)
        ) {
            cache?.evictAll()
            cacheBudgetBytes = budget
            cacheSignature = signature
            cache = object : LruCache<AlbumArtCacheKey, Bitmap>(budget) {
                override fun sizeOf(key: AlbumArtCacheKey, value: Bitmap): Int =
                    value.allocationByteCount
            }
        }
        return cache
    }

    private fun drawAlbumArt(
        bitmap: Bitmap?,
        rescale: Int,
        grayscale: Boolean,
        width: Int,
        height: Int
    ): Bitmap? {
        if (bitmap == null) return null
        if (rescale == 1 && !grayscale) return bitmap

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val transformation = Matrix()
        if (grayscale) {
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        }

        if (rescale != 1) {
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
        }

        val processed = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(processed).drawBitmap(bitmap, transformation, paint)
        return processed
    }

    private fun targetSize(context: Context): AlbumArtSize {
        val view = ownerRef?.get()
        if (view != null && view.width > 0 && view.height > 0) {
            return AlbumArtSize(view.width, view.height)
        }

        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                ?: return AlbumArtSize(1080, 1920)
        val point = Point()
        windowManager.defaultDisplay.getRealSize(point)
        return AlbumArtSize(point.x, point.y)
    }

    private fun applyResult(
        context: Context,
        processed: Bitmap,
        colors: WallpaperColors?
    ) {
        val clazz = miuiThemeUtilsClass ?: return
        XposedHelpers.setAdditionalStaticField(clazz, "mAlbumArt", processed)
        sendUpdateBroadcast(context)

        if (colors != null) {
            val wallpaperUpdate = Intent("miui.intent.action.LOCK_WALLPAPER_CHANGED")
            wallpaperUpdate.setPackage("com.android.systemui")
            wallpaperUpdate.putExtra("is_wallpaper_color_light", (colors.colorHints and 1) == 1)
            context.sendBroadcast(wallpaperUpdate)
        }
    }

    private fun getSource(): Bitmap? {
        val clazz = miuiThemeUtilsClass ?: return null
        return XposedHelpers.getAdditionalStaticField(clazz, "mAlbumArtSource") as? Bitmap
    }

    private fun getAlbumArt(): Bitmap? {
        val clazz = miuiThemeUtilsClass ?: return null
        return XposedHelpers.getAdditionalStaticField(clazz, "mAlbumArt") as? Bitmap
    }

    private fun isCurrent(generation: Long): Boolean =
        AlbumArtPolicy.shouldPublish(generation, requestGeneration.get())

    private fun hasActiveWork(): Boolean =
        synchronized(workLock) { activeGeneration != 0L }

    private fun finishGeneration(generation: Long) {
        synchronized(workLock) {
            if (activeGeneration != generation) return
            activeGeneration = 0L
            activeKey = null
            workFuture = null
        }
    }

    private fun cancelWork() {
        synchronized(workLock) {
            workFuture?.cancel(true)
            workFuture = null
            activeKey = null
            activeGeneration = 0L
            worker.queue.clear()
            worker.purge()
        }
    }

    private fun invalidateForSizeChange(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        if (
            lastTargetWidth > 0 &&
            lastTargetHeight > 0 &&
            (lastTargetWidth != width || lastTargetHeight != height)
        ) {
            requestGeneration.incrementAndGet()
            cancelWork()
            cache?.evictAll()
            cacheSignature = null
            val clazz = miuiThemeUtilsClass
            if (clazz != null) XposedHelpers.setAdditionalStaticField(clazz, "mAlbumArt", null)
        }
        lastTargetWidth = width
        lastTargetHeight = height
    }

    private fun sendUpdateBroadcast(context: Context) {
        val update = Intent(GlobalActions.EVENT_PREFIX + "UPDATE_LS_ALBUM_ART")
        update.setPackage("com.android.systemui")
        context.sendBroadcast(update)
    }
}
