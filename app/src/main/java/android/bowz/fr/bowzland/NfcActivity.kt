package android.bowz.fr.bowzland

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
    val salon = 1
    val cuisine = 2
    val entree = 4
    val chambre = 3
    var speakerCuisineState = false
    var lightCuisineState = false
    private lateinit var sharedPref : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nfc_activity)
        sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        lightCuisineState = sharedPref.getBoolean("lightCuisineState",false)
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

    fun roomState(roomNumber: Int, state: Boolean) {
        val hueTrofitInterface = HueTrofitInterface.create()
        val callback = object : Callback<Array<HueResult>> {
            override fun onFailure(call: Call<Array<HueResult>>?, t: Throwable?) {
                Toast.makeText(this@NfcActivity, "Erreur d'accès à la lampe", Toast.LENGTH_LONG).show()
                Log.d("OkHttpError", t.toString())
            }

            override fun onResponse(call: Call<Array<HueResult>>?, response: Response<Array<HueResult>>?) {
                Toast.makeText(this@NfcActivity, "Lumière modifiée -> ${response?.body()}", Toast.LENGTH_LONG).show()
            }
        }
        val changeCall = hueTrofitInterface.roomState(roomNumber, HueRequest(state))
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
            val ndef = Ndef.get(tag) ?: return null
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
            val payload = record.payload
            val textEncoding = if ((payload[0] and 128.toByte()) == 0.toByte()) {
                Charsets.UTF_8
            } else {
                Charsets.UTF_16
            }
            val languageCodeLength = payload[0] and 51
            return String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, textEncoding)
        }

        @SuppressLint("SetTextI18n")
        override fun onPostExecute(result: String?) {
            if (result != null) {
                when (result) {
                    "salon" ->{
                        roomState(salon, true)
                        computerState(true)
                        speakerState(salon,true)
                    }
                    "entree" -> roomState(entree, true)
                    "chambre" -> {
                        roomState(chambre, true)
                        roomState(salon,false)
                        roomState(cuisine,false)
                        roomState(entree,false)
                        computerState(false)
                        speakerState(cuisine,false)
                        speakerState(salon,false)
                    }
                    "cuisine" -> {
                        roomState(cuisine, true)
                        toogleCuisineSpeakers()
                    }
                    "travail" -> {
                        roomState(chambre, false)
                        roomState(salon,false)
                        roomState(cuisine,false)
                        roomState(entree,false)
                        computerState(false)
                        speakerState(cuisine,false)
                        speakerState(salon,false)
                    }
                    else -> Toast.makeText(this@NfcActivity, "Hein ?", Toast.LENGTH_LONG).show()
                }
            }
        }


    }

    fun computerState(running : Boolean){
        //TODO("trouver un moyen d'allumer et d'éteindre le pc... en passant par l'API sur le raspberry ?)
//        java WakeOnLan <broadcast-ip> <mac-address>
//        GENRE ça
        when(running){
            true -> WakeOnLan.main(arrayOf("192.168.1.1","AA:AA:AA:AA:AA"))
            false -> getRaspiDoingStuff()
        }
    }

    fun getRaspiDoingStuff(){
        TODO("ecrire les scripts qui font les mm trucs")
    }

    fun speakerState(room : Int,running : Boolean){
        val raspberryTerface = RaspberryTerface.create()
        val callback = object : Callback<RaspiResult> {
            override fun onFailure(call: Call<RaspiResult>?, t: Throwable?) {
                Toast.makeText(this@NfcActivity, "Erreur de SpeakerState", Toast.LENGTH_LONG).show()
                Log.d("OkHttpError", t.toString())
            }

            override fun onResponse(call: Call<RaspiResult>?, response: Response<RaspiResult>?) {
                Toast.makeText(this@NfcActivity, "Enceinte atteinte -> ${response?.body()}", Toast.LENGTH_LONG).show()
            }
        }
        val changeCall = raspberryTerface.speakerState(room,raspiRequest(running))
        changeCall.enqueue(callback)
        //TODO("codé mais pas testé")
    }

    fun toogleCuisineSpeakers(){
        speakerCuisineState = sharedPref.getBoolean("speakerCuisineState",false)
        speakerState(cuisine,!speakerCuisineState)
        sharedPref.edit().putBoolean("speakerCuisineState",!speakerCuisineState).apply()
//        TODO("codé pas testé")
    }

    fun deBug(text: String) {
        Log.d("monDebug", text)
    }

}