package com.minkiapps.biometricwrapper

import android.app.Application
import android.util.Log
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                Log.println(priority, "BiometricWrapperLog", message)
            }
        })
    }
}