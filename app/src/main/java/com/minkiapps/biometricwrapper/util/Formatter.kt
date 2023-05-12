package com.minkiapps.biometricwrapper.util

import android.icu.text.SimpleDateFormat
import java.util.Date

private val logTimeFormatter = SimpleDateFormat("HH:mm:ss.SSS")

fun Long.formatLogTime() : String {
    return logTimeFormatter.format(Date(this))
}