package android.bowz.fr.bowzland

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        val textView = textView_explanation

        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish()
            return
        }

        if (!mNfcAdapter.isEnabled) {
            textView.text = getString(R.string.NFC_disabled)
        } else {
            textView.setText(R.string.explanation)
        }

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // TODO: handle Intent
    }
}
