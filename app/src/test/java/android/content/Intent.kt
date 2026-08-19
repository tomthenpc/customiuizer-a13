package android.content

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable

/**
 * Minimal but functional test shadow of [android.content.Intent] for JVM unit tests.
 *
 * The framework stub jar's [Intent] returns default values and does not store extras,
 * which makes any production code that sends broadcasts with extras untestable. This
 * shadow re-implements the subset of the public API used by the app and tests while
 * keeping state in memory.
 */
open class Intent {

    private var mPackage: String? = null
    private var mComponent: ComponentName? = null
    private var mClassName: String? = null
    private var mFlags: Int = 0
    private var mData: Uri? = null
    private val mExtras: MutableMap<String, Any> = mutableMapOf()

    open var action: String? = null

    constructor()

    constructor(action: String?) {
        this.action = action
    }

    constructor(action: String?, data: Uri?) {
        this.action = action
        mData = data
    }

    open fun getPackage(): String? = mPackage

    open fun setPackage(packageName: String?): Intent {
        mPackage = packageName
        return this
    }

    open fun getComponent(): ComponentName? = mComponent

    open fun setComponent(component: ComponentName?): Intent {
        mComponent = component
        return this
    }

    open fun setClassName(packageName: String, className: String): Intent {
        mPackage = packageName
        mClassName = className
        mComponent = ComponentName(packageName, className)
        return this
    }

    open fun setClassName(context: Context, className: String): Intent {
        mPackage = context.packageName
        mClassName = className
        mComponent = ComponentName(context.packageName, className)
        return this
    }

    open fun getFlags(): Int = mFlags

    open fun setFlags(flags: Int): Intent {
        mFlags = flags
        return this
    }

    open fun addFlags(flags: Int): Intent {
        mFlags = mFlags or flags
        return this
    }

    open fun setData(data: Uri?): Intent {
        mData = data
        return this
    }

    open fun getData(): Uri? = mData

    open fun putExtra(name: String, value: String?): Intent {
        if (value != null) mExtras[name] = value else mExtras.remove(name)
        return this
    }

    open fun putExtra(name: String, value: Boolean): Intent {
        mExtras[name] = value
        return this
    }

    open fun putExtra(name: String, value: Int): Intent {
        mExtras[name] = value
        return this
    }

    open fun putExtra(name: String, value: Long): Intent {
        mExtras[name] = value
        return this
    }

    open fun putExtra(name: String, value: Float): Intent {
        mExtras[name] = value
        return this
    }

    open fun putExtra(name: String, value: Double): Intent {
        mExtras[name] = value
        return this
    }

    open fun putExtra(name: String, value: Bundle?): Intent {
        if (value != null) mExtras[name] = value else mExtras.remove(name)
        return this
    }

    open fun putExtra(name: String, value: Parcelable?): Intent {
        if (value != null) mExtras[name] = value else mExtras.remove(name)
        return this
    }

    open fun putExtra(name: String, value: java.io.Serializable?): Intent {
        if (value != null) mExtras[name] = value else mExtras.remove(name)
        return this
    }

    open fun putExtra(name: String, value: Array<String>?): Intent {
        if (value != null) mExtras[name] = value else mExtras.remove(name)
        return this
    }

    open fun putExtra(name: String, value: IntArray?): Intent {
        if (value != null) mExtras[name] = value else mExtras.remove(name)
        return this
    }

    open fun getStringExtra(name: String): String? = mExtras[name] as? String

    open fun getBooleanExtra(name: String, defaultValue: Boolean): Boolean =
        mExtras[name] as? Boolean ?: defaultValue

    open fun getIntExtra(name: String, defaultValue: Int): Int =
        mExtras[name] as? Int ?: defaultValue

    open fun getLongExtra(name: String, defaultValue: Long): Long =
        mExtras[name] as? Long ?: defaultValue

    open fun getBundleExtra(name: String): Bundle? = mExtras[name] as? Bundle

    open fun <T : Parcelable> getParcelableExtra(name: String): T? =
        mExtras[name] as? T

    open fun getStringArrayExtra(name: String): Array<String>? =
        mExtras[name] as? Array<String>

    open fun getIntArrayExtra(name: String): IntArray? =
        mExtras[name] as? IntArray

    open fun hasExtra(name: String): Boolean = mExtras.containsKey(name)

    override fun toString(): String = "Intent{action=$action, extras=$mExtras}"

    companion object {
        const val ACTION_TIME_CHANGED = "android.intent.action.TIME_SET"
        const val ACTION_TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED"
        const val ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON"
        const val ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF"
        const val ACTION_BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED"
        const val ACTION_VIEW = "android.intent.action.VIEW"

        const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
        const val FLAG_ACTIVITY_RESET_TASK_IF_NEEDED = 0x00200000
        const val FLAG_ACTIVITY_CLEAR_TASK = 0x00008000
    }
}
