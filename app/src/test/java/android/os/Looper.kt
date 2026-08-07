package android.os

open class Looper {

    companion object {
        private val sThreadLocal = ThreadLocal<Looper?>()
        private val sMainLooper by lazy { Looper() }

        @JvmStatic
        open fun prepare() {
            sThreadLocal.set(Looper())
        }

        @JvmStatic
        open fun myLooper(): Looper? = sThreadLocal.get()

        @JvmStatic
        open fun getMainLooper(): Looper? = sMainLooper
    }

    open fun getQueue(): MessageQueue? = null
    open fun quit() {}
}
