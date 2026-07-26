package name.monwf.customiuizer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")
class CredentialsShortcut : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent().apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, Intent(this@CredentialsShortcut, Credentials::class.java))
            putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.credentials_unlock))
            putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this@CredentialsShortcut, R.drawable.ic_credentials))
        }

        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
