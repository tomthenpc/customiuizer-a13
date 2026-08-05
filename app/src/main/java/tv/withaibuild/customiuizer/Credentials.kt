package tv.withaibuild.customiuizer

import android.app.Activity
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import tv.withaibuild.customiuizer.utils.SettingsDiagnostics
import javax.crypto.KeyGenerator

@Suppress("DEPRECATION")
class Credentials : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km != null && km.isKeyguardSecure) {
                try {
                    val spec = KeyGenParameterSpec.Builder(
                        "dummy",
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    ).setUserAuthenticationRequired(true).build()
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                        init(spec)
                        generateKey()
                    }
                    Toast.makeText(this, R.string.credentials_ok, Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Throwable) {
                    if (e is OutOfMemoryError || e is ThreadDeath || e is VirtualMachineError) throw e
                    val authIntent = km.createConfirmDeviceCredentialIntent(
                        getString(R.string.credentials_unlock),
                        getString(R.string.dummy)
                    )
                    authIntent?.let { startActivityForResult(it, 0) }
                }
            } else {
                finish()
                startActivity(Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD))
            }
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            SettingsDiagnostics.failure("Credentials.initializeCredentialFlow", t)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, R.string.credentials_success, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
