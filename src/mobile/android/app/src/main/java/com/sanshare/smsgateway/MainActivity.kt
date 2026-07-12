package com.sanshare.smsgateway

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sanshare.smsgateway.ui.navigation.SmsGatewayApp
import com.sanshare.smsgateway.ui.theme.AndroidSmsGatewayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AndroidSmsGatewayTheme {
                SmsGatewayApp()
            }
        }
    }
}
