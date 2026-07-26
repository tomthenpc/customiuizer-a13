package name.monwf.customiuizer.utils

import android.os.Parcel
import android.os.Parcelable
import java.util.Calendar

class SoundData private constructor(
    val caller: String,
    val uid: String,
    val type: String,
    val time: Long
) : Parcelable {

    constructor(caller: String, type: String, uid: String) : this(
        caller,
        uid,
        type,
        Calendar.getInstance().time.time
    )

    private constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong()
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(caller)
        dest.writeString(uid)
        dest.writeString(type)
        dest.writeLong(time)
    }

    fun toPref(): String = "$caller|$type|$uid"

    override fun toString(): String = "SoundData{caller='$caller', uid='$uid', type='$type', time=$time}"

    override fun equals(other: Any?): Boolean {
        if (other !is SoundData) return false
        return caller == other.caller && uid == other.uid && type == other.type
    }

    override fun hashCode(): Int {
        var result = caller.hashCode()
        result = 31 * result + uid.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SoundData> = object : Parcelable.Creator<SoundData> {
            override fun createFromParcel(parcel: Parcel): SoundData = SoundData(parcel)
            override fun newArray(size: Int): Array<SoundData?> = arrayOfNulls(size)
        }

        @JvmStatic
        fun fromPref(pref: String): SoundData {
            val dataArr = pref.split("\\|".toRegex())
            return SoundData(dataArr[0], dataArr[1], dataArr[2])
        }
    }
}
