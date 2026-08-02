package com.android.server

class TelephonyRegistry {
    fun notifyCallState(state: Int, incomingNumber: String) {}

    fun notifyCallStateForPhoneId(phoneId: Int, subId: Int, state: Int, incomingNumber: String) {}
}
