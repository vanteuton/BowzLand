package android.bowz.fr.bowzland

import android.app.Activity
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentFilter.MalformedMimeTypeException
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.UnsupportedEncodingException
import java.util.*
import kotlin.experimental.and


class MainActivity : AppCompatActivity() {

    lateinit var mNfcAdapter : NfcAdapter
    private val mimeTextPlain = "text/plain"
    private val TAG = "NfcDemo"
    lateinit var textView : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        textView = textView_explanation

        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show()
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

    override fun onResume() {
        super.onResume()

        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */
        setupForegroundDispatch(this, mNfcAdapter)
    }

    override fun onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, mNfcAdapter)

        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        handleIntent(intent)
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    fun setupForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
        val intent = Intent(activity.applicationContext, activity.javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = PendingIntent.getActivity(activity.applicationContext, 0, intent, 0)

        val filters = arrayOfNulls<IntentFilter>(1)
        val techList = arrayOf<Array<String>>()

        // Notice that this is the same filter as in our manifest.
        filters[0] = IntentFilter()
        filters[0]?.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
        filters[0]?.addCategory(Intent.CATEGORY_DEFAULT)
        try {
            filters[0]?.addDataType(mimeTextPlain)
        } catch (e: MalformedMimeTypeException) {
            throw RuntimeException("Check your mime type.")
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList)
    }

    /**
     * @param activity The corresponding [MainActivity] requesting to stop the foreground dispatch.
     * @param adapter The [NfcAdapter] used for the foreground dispatch.
     */
    fun stopForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
        adapter.disableForegroundDispatch(activity)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action) {

            val type = intent.type
            if (mimeTextPlain == type) {

                val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as Tag
                NdefReaderTask().execute(tag)

            } else {
                Log.d(TAG, "Wrong mime type: $type")
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED == action) {

            // In case we would still use the Tech Discovered Intent
            val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as Tag
            val techList = tag.techList
            val searchedTech = Ndef::class.java.name

            for (tech in techList) {
                if (searchedTech == tech) {
                    NdefReaderTask().execute(tag)
                    break
                }
            }
        }
    }

    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     * @author Ralf Wondratschek
     */
    private inner class NdefReaderTask : AsyncTask<Tag, Void, String>() {

        override fun doInBackground(vararg params: Tag): String? {
            val tag = params[0]

            val ndef = Ndef.get(tag)
                    ?: // NDEF is not supported by this Tag.
                    return null

            val ndefMessage = ndef.cachedNdefMessage

            val records = ndefMessage.records
            for (ndefRecord in records) {
                if (ndefRecord.tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.type, NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord)
                    } catch (e: UnsupportedEncodingException) {
                        Log.e(ContentValues.TAG, "Unsupported Encoding", e)
                    }

                }
            }

            return null
        }

        @Throws(UnsupportedEncodingException::class)
        private fun readText(record: NdefRecord): String {
            /*
             * See NFC forum specification for "Text Record Type Definition" at 3.2.1
             *
             * http://www.nfc-forum.org/specs/
             *
             * bit_7 defines encoding
             * bit_6 reserved for future use, must be 0
             * bit_5..0 length of IANA language code
             */

            val payload = record.payload

            // Get the Text Encoding
            val textEncoding = if ((payload[0] and 128.toByte()) == 0.toByte()){ Charsets.UTF_8 }else {Charsets.UTF_16}

            // Get the Language Code
            val languageCodeLength = payload[0] and 51

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, textEncoding)
        }

        override fun onPostExecute(result: String?) {
            if (result != null) textView.text = "Read content: $result"
        }
    }
}
