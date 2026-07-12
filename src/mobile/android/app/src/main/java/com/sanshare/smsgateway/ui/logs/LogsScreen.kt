package com.sanshare.smsgateway.ui.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sanshare.smsgateway.data.local.dao.RequestAuditLogDao
import com.sanshare.smsgateway.data.local.dao.SystemLogDao
import com.sanshare.smsgateway.data.local.entity.RequestAuditLogEntity
import com.sanshare.smsgateway.data.local.entity.SystemLogEntity
import com.sanshare.smsgateway.data.local.entity.WebhookAttemptEntity
import com.sanshare.smsgateway.domain.repository.WebhookAttemptQuery
import com.sanshare.smsgateway.domain.repository.WebhookAttemptRepository
import com.sanshare.smsgateway.ui.UiFormatters
import com.sanshare.smsgateway.ui.component.SecondaryOperationButton
import com.sanshare.smsgateway.ui.component.StatusCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LogTab { SYSTEM, AUDIT, WEBHOOK }
private const val LOG_PAGE_SIZE = 30

data class LogsUiState(
    val selectedTab: LogTab = LogTab.SYSTEM,
    val offset: Int = 0,
    val systemLogs: List<SystemLogEntity> = emptyList(),
    val auditLogs: List<RequestAuditLogEntity> = emptyList(),
    val webhookAttempts: List<WebhookAttemptEntity> = emptyList(),
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val systemLogDao: SystemLogDao,
    private val requestAuditLogDao: RequestAuditLogDao,
    private val webhookAttemptRepository: WebhookAttemptRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun selectTab(tab: LogTab) {
        mutableUiState.value = mutableUiState.value.copy(selectedTab = tab, offset = 0)
        refresh()
    }

    fun nextPage() {
        mutableUiState.value = mutableUiState.value.copy(offset = mutableUiState.value.offset + LOG_PAGE_SIZE)
        refresh()
    }

    fun previousPage() {
        mutableUiState.value = mutableUiState.value.copy(offset = (mutableUiState.value.offset - LOG_PAGE_SIZE).coerceAtLeast(0))
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val state = mutableUiState.value
            mutableUiState.value = state.copy(
                systemLogs = systemLogDao.queryFiltered(null, null, null, null, LOG_PAGE_SIZE, state.offset, "DESC"),
                auditLogs = requestAuditLogDao.queryFiltered(null, null, LOG_PAGE_SIZE, state.offset, "DESC"),
                webhookAttempts = webhookAttemptRepository.query(
                    WebhookAttemptQuery(limit = LOG_PAGE_SIZE, offset = state.offset, sortDirection = "DESC"),
                ).items,
            )
        }
    }
}

@Composable
fun LogsRoute(viewModel: LogsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LogsScreen(
        uiState = uiState,
        onSelectTab = viewModel::selectTab,
        onNextPage = viewModel::nextPage,
        onPreviousPage = viewModel::previousPage,
    )
}

@Composable
fun LogsScreen(
    uiState: LogsUiState,
    onSelectTab: (LogTab) -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Logs", style = MaterialTheme.typography.headlineSmall)
        TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
            LogTab.entries.forEach { tab ->
                Tab(selected = uiState.selectedTab == tab, onClick = { onSelectTab(tab) }, text = { Text(tab.name) })
            }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (uiState.selectedTab) {
                LogTab.SYSTEM -> items(uiState.systemLogs) { item ->
                    StatusCard(item.category, item.level, "${UiFormatters.localTimestamp(item.createdAt)} • ${item.message}")
                }
                LogTab.AUDIT -> items(uiState.auditLogs) { item ->
                    StatusCard(item.method, item.path, "${UiFormatters.localTimestamp(item.createdAt)} • ${item.responseCode} • ${item.durationMs}ms")
                }
                LogTab.WEBHOOK -> items(uiState.webhookAttempts) { item ->
                    StatusCard(
                        "SMS ${item.receivedSmsId} • Attempt ${item.attemptNumber}",
                        if (item.success) "Success" else (item.errorCode ?: "Failed"),
                        "${UiFormatters.localTimestamp(item.attemptedAt)} • HTTP ${item.responseCode ?: "n/a"} • ${UiFormatters.duration(item.durationMs)}",
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryOperationButton(text = "Previous", onClick = onPreviousPage, modifier = Modifier.weight(1f), enabled = uiState.offset > 0)
            SecondaryOperationButton(text = "Next", onClick = onNextPage, modifier = Modifier.weight(1f))
        }
    }
}
