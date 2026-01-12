package de.meply.meply

import android.app.Application
import android.util.Log

class MeplyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("MeplyApp", "✅ Application gestartet")
    }
}
