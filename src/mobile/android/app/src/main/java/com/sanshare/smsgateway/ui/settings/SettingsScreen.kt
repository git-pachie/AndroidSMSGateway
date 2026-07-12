package com.sanshare.smsgateway.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sanshare.smsgateway.domain.model.AppSettings
import com.sanshare.smsgateway.domain.repository.GeneratedApiKey
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.ui.UiFormatters
import com.sanshare.smsgateway.ui.component.ConfirmationDialog
import com.sanshare.smsgateway.ui.component.PrimaryOperationButton
import com.sanshare.smsgateway.ui.component.SectionHeader
import com.sanshare.smsgateway.ui.component.SecondaryOperationButton
import com.sanshare.smsgateway.ui.component.StatusCard
import com.sanshare.smsgateway.ui.component.WarningBanner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings? = null,
    val revealedApiKey: String? = null,
    val apiKeyIdentifier: String? = null,
    val isRegeneratingApiKey: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val generated = settingsRepository.ensureInitialized()
            mutableUiState.value = mutableUiState.value.copy(
                settings = settingsRepository.getSettings(),
                revealedApiKey = generated?.rawKey,
                apiKeyIdentifier = generated?.identifier,
            )
            settingsRepository.observeSettings().collect { settings ->
                mutableUiState.value = mutableUiState.value.copy(settings = settings)
            }
        }
    }

    fun regenerateApiKey() {
        if (mutableUiState.value.isRegeneratingApiKey) return
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(isRegeneratingApiKey = true)
            val regenerated = settingsRepository.regenerateApiKey()
            mutableUiState.value = mutableUiState.value.copy(
                isRegeneratingApiKey = false,
                revealedApiKey = regenerated.rawKey,
                apiKeyIdentifier = regenerated.identifier,
            )
        }
    }

    fun dismissApiKeyDialog() {
        mutableUiState.value = mutableUiState.value.copy(revealedApiKey = null, apiKeyIdentifier = null)
    }
}

@Composable
fun SettingsRoute(
    onCopyText: (String, String) -> Unit,
    onShowSnackbar: suspend (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onCopyApiKey = { uiState.revealedApiKey?.let { onCopyText("API key", it) } },
        onDismissApiKey = viewModel::dismissApiKeyDialog,
        onRegenerateApiKey = viewModel::regenerateApiKey,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onCopyApiKey: () -> Unit,
    onDismissApiKey: () -> Unit,
    onRegenerateApiKey: () -> Unit,
) {
    var showRegenerateConfirmation by remember { mutableStateOf(false) }

    if (showRegenerateConfirmation) {
        ConfirmationDialog(
            title = "Regenerate API key",
            message = "The current API key cannot be recovered later. Regenerate only if the existing key is lost or compromised.",
            confirmText = "Regenerate",
            onConfirm = {
                showRegenerateConfirmation = false
                onRegenerateApiKey()
            },
            onDismiss = { showRegenerateConfirmation = false },
        )
    }

    uiState.revealedApiKey?.let { apiKey ->
        AlertDialog(
            onDismissRequest = onDismissApiKey,
            title = { Text("Save this API key now") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This is the only time the raw API key will be shown.")
                    Text(apiKey, style = MaterialTheme.typography.bodyLarge)
                    uiState.apiKeyIdentifier?.let { Text("Identifier: $it") }
                }
            },
            confirmButton = {
                PrimaryOperationButton(text = "Copy", onClick = onCopyApiKey)
            },
            dismissButton = {
                SecondaryOperationButton(text = "Close", onClick = onDismissApiKey)
            },
        )
    }

    val settings = uiState.settings
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        if (settings == null) {
            WarningBanner("Loading settings…")
            return@Column
        }

        StatusCard(
            title = "Gateway identity",
            value = settings.deviceId,
            detail = "Port ${settings.serverPort} • API key ${settings.apiKeyIdentifier ?: "Not generated"}",
        )
        StatusCard(
            title = "Webhook configuration",
            value = if (settings.webhookEnabled) "Enabled" else "Disabled",
            detail = "URL ${UiFormatters.urlPreview(settings.webhookUrl)} • Secret ${if (settings.webhookSecretConfigured) "configured" else "not configured"} • HTTPS required ${settings.requireHttpsWebhook}",
        )
        StatusCard(
            title = "Rate controls",
            value = "${settings.rateLimitPerMinute} per minute",
            detail = "Daily limit ${if (settings.dailySmsLimitEnabled) settings.dailySmsLimit else "disabled"} • Retry max ${settings.maxRetryCount} • Base delay ${settings.retryBaseDelaySeconds}s",
        )

        SectionHeader("Security")
        WarningBanner("API keys are stored as hashes. Lost keys cannot be revealed later and must be regenerated.")
        SecondaryOperationButton(
            text = if (uiState.isRegeneratingApiKey) "Regenerating..." else "Regenerate API Key",
            onClick = { showRegenerateConfirmation = true },
            enabled = !uiState.isRegeneratingApiKey,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
