package com.example.cmovil.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager

class DeviceHelper(private val context: Context) {

    fun getDeviceId(): String {
        // Using Android ID as IMEI is restricted
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    fun getPhoneNumber(): String? {
        // This often returns null or requires carrier privileges/SIM specific
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return try {
            telephonyManager.line1Number
        } catch (e: SecurityException) {
            null
        }
    }

    fun getModel(): String {
        return Build.MODEL
    }

    fun getBrand(): String {
        return Build.MANUFACTURER
    }

    fun getOsVersion(): String {
        return Build.VERSION.RELEASE
    }
}
