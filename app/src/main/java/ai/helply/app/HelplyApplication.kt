package ai.helply.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HelplyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize local encrypted storage & security providers
    }
}
