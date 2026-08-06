package android.app

class FakeKeyguardManager : KeyguardManager() {
    var locked: Boolean = false
    var secure: Boolean = false

    override fun isKeyguardLocked(): Boolean = locked
    override fun isKeyguardSecure(): Boolean = secure
}
