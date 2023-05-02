package com.minkiapps.biometricwrapper.util

import android.content.Context
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability

fun Context.isHMSAvailable() : Boolean {
    return HuaweiApiAvailability.getInstance()
        .isHuaweiMobileServicesAvailable(this) == ConnectionResult.SUCCESS
}