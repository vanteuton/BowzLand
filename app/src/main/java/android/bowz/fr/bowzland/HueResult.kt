package android.bowz.fr.bowzland

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName




class HueResult {

    @SerializedName("success")
    @Expose
    var success: Success? = null

}

class Error {
    @SerializedName("type")
    @Expose
    var type: Int? = null
    @SerializedName("address")
    @Expose
    var address: String? = null

    @SerializedName("description")
    @Expose
    var description: String? = null

}

class Success {

    @SerializedName("/lights/8/state/on")
    @Expose
    var lights8StateOn: Boolean? = null

}