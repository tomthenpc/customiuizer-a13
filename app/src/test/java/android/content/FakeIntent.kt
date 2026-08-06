package android.content

class FakeIntent(action: String? = null) : Intent(action) {

    val extras = mutableMapOf<String, Any>()

    override fun getStringExtra(name: String): String? {
        return extras[name] as? String ?: super.getStringExtra(name)
    }

    override fun getBooleanExtra(name: String, defaultValue: Boolean): Boolean {
        return extras[name] as? Boolean ?: defaultValue
    }

    override fun putExtra(name: String, value: String?): Intent {
        if (value != null) extras[name] = value
        return this
    }

    override fun putExtra(name: String, value: Boolean): Intent {
        extras[name] = value
        return this
    }
}
