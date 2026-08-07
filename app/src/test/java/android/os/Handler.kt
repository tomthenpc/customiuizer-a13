package android.os

open class Handler @JvmOverloads constructor(
    looper: Looper,
    private val callback: Callback? = null
) {

    private val mLooper: Looper = looper

    constructor() : this(Looper.myLooper() ?: Looper.getMainLooper() ?: Looper())

    constructor(callback: Callback) : this(Looper.myLooper() ?: Looper.getMainLooper() ?: Looper(), callback)

    interface Callback {
        fun handleMessage(msg: Message): Boolean
    }

    open fun post(r: Runnable): Boolean {
        r.run()
        return true
    }

    open fun postDelayed(r: Runnable, delayMillis: Long): Boolean = post(r)

    open fun postAtTime(r: Runnable, uptimeMillis: Long): Boolean = post(r)

    open fun postAtFrontOfQueue(r: Runnable): Boolean = post(r)

    open fun sendMessage(msg: Message): Boolean {
        dispatchMessage(msg)
        return true
    }

    open fun sendMessageDelayed(msg: Message, delayMillis: Long): Boolean = sendMessage(msg)

    open fun sendMessageAtFrontOfQueue(msg: Message): Boolean = sendMessage(msg)

    open fun sendEmptyMessage(what: Int): Boolean {
        return sendMessage(obtainMessage().apply { this.what = what })
    }

    open fun sendEmptyMessageDelayed(what: Int, delayMillis: Long): Boolean = sendEmptyMessage(what)

    open fun obtainMessage(): Message = Message().apply { target = this@Handler }

    open fun obtainMessage(what: Int): Message = obtainMessage().apply { this.what = what }

    open fun obtainMessage(what: Int, obj: Any?): Message = obtainMessage(what).apply { this.obj = obj }

    open fun obtainMessage(what: Int, arg1: Int, arg2: Int): Message =
        obtainMessage(what).apply { this.arg1 = arg1; this.arg2 = arg2 }

    open fun obtainMessage(what: Int, arg1: Int, arg2: Int, obj: Any?): Message =
        obtainMessage(what, arg1, arg2).apply { this.obj = obj }

    open fun dispatchMessage(msg: Message) {
        if (msg.callback != null) {
            msg.callback.run()
        } else {
            if (callback == null || !callback.handleMessage(msg)) {
                handleMessage(msg)
            }
        }
    }

    open fun handleMessage(msg: Message) {}

    open fun getLooper(): Looper = mLooper

    open fun hasMessages(what: Int): Boolean = false

    open fun removeCallbacks(r: Runnable?) {}

    open fun removeMessages(what: Int) {}

    open fun removeCallbacksAndMessages(token: Any?) {}
}
