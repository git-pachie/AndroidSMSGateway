package com.sanshare.smsgateway.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = GatewayBlue,
    secondary = GatewayGreen,
    tertiary = GatewayAmber,
    error = GatewayRed,
)

private val DarkColors = darkColorScheme(
    primary = GatewayBlue,
    secondary = GatewayGreen,
    tertiary = GatewayAmber,
    error = GatewayRed,
)

@Composable
fun AndroidSmsGatewayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
