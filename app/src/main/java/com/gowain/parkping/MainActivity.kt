package com.gowain.parkping

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import com.gowain.parkping.ui.ParkPingApp
import com.gowain.parkping.ui.theme.ParkPingTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ParkPingTheme {
                ParkPingApp()
            }
        }
    }
}
