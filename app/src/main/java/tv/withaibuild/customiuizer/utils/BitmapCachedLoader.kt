package tv.withaibuild.customiuizer.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import android.widget.ImageView
import java.lang.ref.WeakReference
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal fun appIconCacheKey(appData: AppData): String {
    val cached = appData.iconKey
    if (cached.isNotEmpty()) return cached
    val pkgName = appData.pkgName.orEmpty()
    val actName = appData.actName.orEmpty()
    return if (actName.isEmpty()) pkgName else "$pkgName|$actName"
}

internal class BitmapCachedLoader(
    target: ImageView,
    info: AppData,
    context: Context
) {
    private val targetRef = WeakReference(target)
    private val appInfo = WeakReference(info)
    private val ctx = context.applicationContext
    private val targetTag = target.tag
    private var iconKey = ""

    fun execute() {
        val appData = appInfo.get() ?: return
        iconKey = appIconCacheKey(appData)
        if (iconKey.isEmpty()) return

        val isLeader = synchronized(inFlightLock) {
            val waiters = inFlight.getOrPut(iconKey) { ArrayList() }
            waiters.add(this)
            waiters.size == 1
        }
        if (!isLeader) return

        try {
            executor.execute(LoadTask(this))
        } catch (rejected: RejectedExecutionException) {
            Log.w(TAG, "Icon load rejected, queue is full: $iconKey")
            releaseWaiters()
        }
    }

    private class LoadTask(private val loader: BitmapCachedLoader) : Runnable {
        override fun run() {
            val bitmap = try {
                loader.loadBitmap()
            } catch (t: Throwable) {
                Log.w(TAG, "Icon load failed", t)
                null
            }
            if (bitmap == null) {
                loader.releaseWaiters()
            } else {
                mainHandler.post { loader.publish(bitmap) }
            }
        }
    }

    private fun loadBitmap(): Bitmap? {
        val appData = appInfo.get() ?: return null
        Helpers.memoryCache.get(iconKey)?.let { return it }

        val pkgName = appData.pkgName.orEmpty()
        val actName = appData.actName.orEmpty()
        if (pkgName.isEmpty() && actName.isEmpty()) return null

        val packageManager = ctx.packageManager
        var icon: android.graphics.drawable.Drawable? = null
        if (actName.isNotEmpty() && actName != "-") {
            try {
                val component = ComponentName(pkgName, actName)
                if (packageManager.getActivityInfo(component, PackageManager.MATCH_ALL).icon != 0) {
                    icon = packageManager.getActivityIcon(component)
                }
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        if (icon == null) {
            try {
                if (
                    packageManager.getApplicationInfo(
                        pkgName,
                        PackageManager.MATCH_DISABLED_COMPONENTS
                    ).icon != 0
                ) {
                    icon = packageManager.getApplicationIcon(pkgName)
                }
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        icon ?: return null

        val iconSize = ctx.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        icon.setBounds(0, 0, iconSize, iconSize)
        icon.draw(canvas)
        Helpers.memoryCache.put(iconKey, bitmap)
        return bitmap
    }

    private fun publish(bitmap: Bitmap) {
        val waiters = synchronized(inFlightLock) {
            inFlight.remove(iconKey)?.toList() ?: return
        }
        for (loader in waiters) {
            try {
                loader.applyToTarget(bitmap)
            } catch (t: Throwable) {
                Log.w(TAG, "Unable to apply app icon", t)
            }
        }
    }

    private fun applyToTarget(bitmap: Bitmap) {
        val imageView = targetRef.get() ?: return
        if (targetTag != imageView.tag) return
        val drawable = imageView.drawable
        if (drawable is TransitionDrawable) {
            drawable.addLayer(BitmapDrawable(ctx.resources, bitmap))
            drawable.startTransition(200)
        }
    }

    private fun releaseWaiters() {
        synchronized(inFlightLock) {
            inFlight.remove(iconKey)
        }
    }

    companion object {
        private const val TAG = "Pengeek.IconLoader"
        private const val MAX_PENDING_TASKS = 128

        internal fun clampLoaderThreadCount(availableProcessors: Int): Int =
            (availableProcessors / 2).coerceIn(2, 4)

        private val threadCount =
            clampLoaderThreadCount(Runtime.getRuntime().availableProcessors())
        private val threadNumber = AtomicInteger()
        private val executor = ThreadPoolExecutor(
            threadCount,
            threadCount,
            15L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(MAX_PENDING_TASKS),
            { runnable ->
                Thread(
                    {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                        runnable.run()
                    },
                    "Pengeek-IconLoader-${threadNumber.incrementAndGet()}"
                )
            },
            ThreadPoolExecutor.AbortPolicy()
        ).apply {
            allowCoreThreadTimeOut(true)
        }
        private val mainHandler = Handler(Looper.getMainLooper())
        private val inFlightLock = Any()
        private val inFlight = HashMap<String, MutableList<BitmapCachedLoader>>()
    }
}
