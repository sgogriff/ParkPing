package com.gowain.parkping

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.gowain.parkping.ui.AppLanguageManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.osmdroid.config.Configuration as OsmConfiguration

@HiltAndroidApp
class ParkPingApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appLanguageManager: AppLanguageManager

    override fun onCreate() {
        super.onCreate()
        appLanguageManager.applyCurrentLanguage()
        OsmConfiguration.getInstance().userAgentValue = packageName
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
