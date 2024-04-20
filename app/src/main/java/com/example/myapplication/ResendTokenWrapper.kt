package com.example.myapplication

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.auth.PhoneAuthProvider

data class ResendTokenWrapper(val resendToken: PhoneAuthProvider.ForceResendingToken?) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readParcelable<PhoneAuthProvider.ForceResendingToken>(PhoneAuthProvider.ForceResendingToken::class.java.classLoader))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(resendToken, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ResendTokenWrapper> {
        override fun createFromParcel(parcel: Parcel): ResendTokenWrapper {
            return ResendTokenWrapper(parcel)
        }

        override fun newArray(size: Int): Array<ResendTokenWrapper?> {
            return arrayOfNulls(size)
        }
    }
}
