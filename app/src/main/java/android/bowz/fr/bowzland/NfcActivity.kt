package android.bowz.fr.bowzland

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.UnsupportedEncodingException
import java.util.*
import kotlin.experimental.and

class NfcActivity : Activity() {

    private lateinit var mNfcAdapter: NfcAdapter
    private val mimeTextPlain = "text/plain"
    private val tag = "NfcDemo"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nfc_activity)

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        handleIntent(intent)
        finish()
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

    fun roomState(roomNumber : Int, state : Boolean){
        val hueTrofitInterface = HueTrofitInterface.create()

        val callback = object : Callback<Array<HueResult>> {
            override fun onFailure(call: Call<Array<HueResult>>?, t: Throwable?) {
                Toast.makeText(this@NfcActivity,"Erreur d'accès à la lampe",Toast.LENGTH_LONG).show()
                Log.d("OkHttpError",t.toString())
            }

            override fun onResponse(call: Call<Array<HueResult>>?, response: Response<Array<HueResult>>?) {
                Toast.makeText(this@NfcActivity,"Lumière modifiée -> ${response?.body()}",Toast.LENGTH_LONG).show()
            }
        }

        val data = when(state){
            true -> HueRequest(true)
            false -> HueRequest(false)
        }
        val changeCall = hueTrofitInterface.roomState(roomNumber,data)
        changeCall.enqueue(callback)
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
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("Check your mime type.")
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList)
    }

    /**
     * @param activity The corresponding [MainSettingsActivity] requesting to stop the foreground dispatch.
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
                Log.d(tag, "Wrong mime type: $type")
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
    @SuppressLint("StaticFieldLeak")
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
                        deBug("Unsupported Encoding")
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
            val textEncoding = if ((payload[0] and 128.toByte()) == 0.toByte()) {
                Charsets.UTF_8
            } else {
                Charsets.UTF_16
            }

            // Get the Language Code
            val languageCodeLength = payload[0] and 51

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, textEncoding)
        }

        @SuppressLint("SetTextI18n")
        override fun onPostExecute(result: String?) {
            if (result != null) {
                when (result) {
                    "cuisine" -> cuisineAction()
                    "entree" -> chambreAction()
                    "chambre" -> chambreAction()
                    "travail" -> travailAction()
                    else -> Toast.makeText(this@NfcActivity, "Hein ?", Toast.LENGTH_LONG).show()
                }
            }
        }

        fun cuisineAction() {
            Toast.makeText(this@NfcActivity, "TU ES DANS LA CUISIIIIIIIIIINE", Toast.LENGTH_LONG).show()
            roomState(1,true)
        }

        fun chambreAction() {
            Toast.makeText(this@NfcActivity, "Bonne nuit :3", Toast.LENGTH_LONG).show()
            roomState(1,true)
        }

        fun travailAction() {
            Toast.makeText(this@NfcActivity, "Aller courage mon gars", Toast.LENGTH_LONG).show()
            roomState(1,true)
        }
    }

    fun deBug(text: String) {
        Log.d("monDebug", text)
    }

}