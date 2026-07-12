package com.sanshare.smsgateway.ui.messages

import android.Manifest
import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sanshare.smsgateway.core.result.AppResult
import com.sanshare.smsgateway.data.local.entity.ReceivedSmsEntity
import com.sanshare.smsgateway.data.local.entity.SentSmsEntity
import com.sanshare.smsgateway.data.local.entity.WebhookAttemptEntity
import com.sanshare.smsgateway.domain.repository.ReceivedSmsQuery
import com.sanshare.smsgateway.domain.repository.ReceivedSmsRepository
import com.sanshare.smsgateway.domain.repository.SentSmsQuery
import com.sanshare.smsgateway.domain.repository.SentSmsRepository
import com.sanshare.smsgateway.domain.repository.WebhookAttemptRepository
import com.sanshare.smsgateway.domain.usecase.SendSmsCommand
import com.sanshare.smsgateway.domain.usecase.SendSmsUseCase
import com.sanshare.smsgateway.sms.WebhookScheduler
import com.sanshare.smsgateway.ui.UiFormatters
import com.sanshare.smsgateway.ui.component.PrimaryOperationButton
import com.sanshare.smsgateway.ui.component.SectionHeader
import com.sanshare.smsgateway.ui.component.SecondaryOperationButton
import com.sanshare.smsgateway.ui.component.StatusCard
import com.sanshare.smsgateway.ui.component.WarningBanner
import com.sanshare.smsgateway.util.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

enum class MessageTab { SENT, RECEIVED }
enum class SortDirection { DESC, ASC }

data class MessagesUiState(
    val selectedTab: MessageTab = MessageTab.SENT,
    val destination: String = "",
    val message: String = "",
    val clientReference: String = "",
    val subscriptionId: String = "",
    val canSendSms: Boolean = false,
    val canReceiveSms: Boolean = false,
    val isSending: Boolean = false,
    val currentMessageId: Long? = null,
    val currentStatus: String? = null,
    val errorMessage: String? = null,
    val characterCount: Int = 0,
    val estimatedSegments: Int = 1,
    val searchFilter: String = "",
    val statusFilter: String = "",
    val sortDirection: String = SortDirection.DESC.name,
    val sentOffset: Int = 0,
    val receivedOffset: Int = 0,
    val sentItems: List<SentSmsEntity> = emptyList(),
    val receivedItems: List<ReceivedSmsEntity> = emptyList(),
    val selectedSentItem: SentSmsEntity? = null,
    val selectedReceivedItem: ReceivedSmsEntity? = null,
    val selectedAttempts: List<WebhookAttemptEntity> = emptyList(),
    val retryScheduled: Boolean = false,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MessagesViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val sendSmsUseCase: SendSmsUseCase,
    private val sentSmsRepository: SentSmsRepository,
    private val receivedSmsRepository: ReceivedSmsRepository,
    private val webhookAttemptRepository: WebhookAttemptRepository,
    private val webhookScheduler: WebhookScheduler,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val destination = MutableStateFlow(savedStateHandle["destination"] ?: "")
    private val message = MutableStateFlow(savedStateHandle["message"] ?: "")
    private val clientReference = MutableStateFlow(savedStateHandle["clientReference"] ?: "")
    private val subscriptionId = MutableStateFlow(savedStateHandle["subscriptionId"] ?: "")
    private val isSending = MutableStateFlow(false)
    private val currentMessageId = MutableStateFlow<Long?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val selectedTab = MutableStateFlow(MessageTab.valueOf(savedStateHandle["selectedTab"] ?: MessageTab.SENT.name))
    private val searchFilter = MutableStateFlow(savedStateHandle["searchFilter"] ?: "")
    private val statusFilter = MutableStateFlow(savedStateHandle["statusFilter"] ?: "")
    private val sortDirection = MutableStateFlow(SortDirection.valueOf(savedStateHandle["sortDirection"] ?: SortDirection.DESC.name))
    private val sentOffset = MutableStateFlow(savedStateHandle["sentOffset"] ?: 0)
    private val receivedOffset = MutableStateFlow(savedStateHandle["receivedOffset"] ?: 0)
    private val selectedSentId = MutableStateFlow<Long?>(null)
    private val selectedReceivedId = MutableStateFlow<Long?>(null)

    private val sentItems = combine(searchFilter, statusFilter, sortDirection, sentOffset) { search, status, sort, offset ->
        sentSmsRepository.query(
            SentSmsQuery(
                to = search.trim().ifBlank { null },
                status = status.trim().ifBlank { null },
                limit = PAGE_SIZE,
                offset = offset,
                sortDirection = sort.name,
            ),
        ).items
    }

    private val receivedItems = combine(searchFilter, statusFilter, sortDirection, receivedOffset) { search, status, sort, offset ->
        receivedSmsRepository.query(
            ReceivedSmsQuery(
                from = search.trim().ifBlank { null },
                forwardStatus = status.trim().ifBlank { null },
                limit = PAGE_SIZE,
                offset = offset,
                sortDirection = sort.name,
            ),
        ).items
    }

    private val currentStatus = currentMessageId.flatMapLatest { messageId ->
        if (messageId == null) flowOf(null) else sentSmsRepository.observeById(messageId)
    }

    private val selectedSentItem = selectedSentId.flatMapLatest { messageId ->
        if (messageId == null) flowOf(null) else sentSmsRepository.observeById(messageId)
    }

    private val selectedReceivedItem = selectedReceivedId.flatMapLatest { smsId ->
        if (smsId == null) flowOf(null) else receivedSmsRepository.observeById(smsId)
    }

    private val selectedAttempts = selectedReceivedId.flatMapLatest { smsId ->
        if (smsId == null) flowOf(emptyList()) else flow { emit(webhookAttemptRepository.listBySmsId(smsId, limit = 20)) }
    }

    private val retryScheduled = selectedReceivedId.flatMapLatest { smsId ->
        if (smsId == null) flowOf(false) else webhookScheduler.observeScheduled(smsId)
    }

    private val editorIdentity = combine(
        selectedTab,
        destination,
        message,
        clientReference,
        subscriptionId,
    ) { selectedTab, destination, message, clientReference, subscriptionId ->
        EditorIdentityState(
            selectedTab = selectedTab,
            destination = destination,
            message = message,
            clientReference = clientReference,
            subscriptionId = subscriptionId,
        )
    }

    private val editorDelivery = combine(
        isSending,
        currentMessageId,
        errorMessage,
        currentStatus,
    ) { isSending, currentMessageId, errorMessage, currentStatus ->
        EditorDeliveryState(
            isSending = isSending,
            currentMessageId = currentMessageId,
            errorMessage = errorMessage,
            currentStatus = currentStatus,
        )
    }

    private val messageEditorState = combine(
        editorIdentity,
        editorDelivery,
    ) { identity, delivery ->
        MessageEditorState(
            selectedTab = identity.selectedTab,
            destination = identity.destination,
            message = identity.message,
            clientReference = identity.clientReference,
            subscriptionId = identity.subscriptionId,
            isSending = delivery.isSending,
            currentMessageId = delivery.currentMessageId,
            errorMessage = delivery.errorMessage,
            currentStatus = delivery.currentStatus,
        )
    }

    private val listState = combine(
        searchFilter,
        statusFilter,
        sortDirection,
        sentOffset,
        receivedOffset,
        sentItems,
        receivedItems,
        selectedSentItem,
        selectedReceivedItem,
        selectedAttempts,
        retryScheduled,
    ) { values ->
        MessageListState(
            searchFilter = values[0] as String,
            statusFilter = values[1] as String,
            sortDirection = values[2] as SortDirection,
            sentOffset = values[3] as Int,
            receivedOffset = values[4] as Int,
            sentItems = values[5] as List<SentSmsEntity>,
            receivedItems = values[6] as List<ReceivedSmsEntity>,
            selectedSentItem = values[7] as SentSmsEntity?,
            selectedReceivedItem = values[8] as ReceivedSmsEntity?,
            selectedAttempts = values[9] as List<WebhookAttemptEntity>,
            retryScheduled = values[10] as Boolean,
        )
    }

    val uiState: StateFlow<MessagesUiState> = combine(
        messageEditorState,
        listState,
    ) { editorState, listState ->
        val permissions = PermissionUtils.gatewayPermissionState(appContext)
        MessagesUiState(
            selectedTab = editorState.selectedTab,
            destination = editorState.destination,
            message = editorState.message,
            clientReference = editorState.clientReference,
            subscriptionId = editorState.subscriptionId,
            canSendSms = permissions.canSendSms,
            canReceiveSms = permissions.canReceiveSms,
            isSending = editorState.isSending,
            currentMessageId = editorState.currentMessageId,
            currentStatus = editorState.currentStatus?.status,
            errorMessage = editorState.errorMessage,
            characterCount = editorState.message.length,
            estimatedSegments = estimateSegments(editorState.message),
            searchFilter = listState.searchFilter,
            statusFilter = listState.statusFilter,
            sortDirection = listState.sortDirection.name,
            sentOffset = listState.sentOffset,
            receivedOffset = listState.receivedOffset,
            sentItems = listState.sentItems,
            receivedItems = listState.receivedItems,
            selectedSentItem = listState.selectedSentItem,
            selectedReceivedItem = listState.selectedReceivedItem,
            selectedAttempts = listState.selectedAttempts,
            retryScheduled = listState.retryScheduled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MessagesUiState())

    fun selectTab(tab: MessageTab) { selectedTab.value = tab }
    fun updateDestination(value: String) { destination.value = value }
    fun updateMessage(value: String) { message.value = value }
    fun updateClientReference(value: String) { clientReference.value = value }
    fun updateSubscriptionId(value: String) { subscriptionId.value = value }
    fun updateSearchFilter(value: String) { searchFilter.value = value; sentOffset.value = 0; receivedOffset.value = 0 }
    fun updateStatusFilter(value: String) { statusFilter.value = value; sentOffset.value = 0; receivedOffset.value = 0 }
    fun toggleSortDirection() { sortDirection.value = if (sortDirection.value == SortDirection.DESC) SortDirection.ASC else SortDirection.DESC }
    fun nextPage() { if (selectedTab.value == MessageTab.SENT) sentOffset.value += PAGE_SIZE else receivedOffset.value += PAGE_SIZE }
    fun previousPage() {
        if (selectedTab.value == MessageTab.SENT) sentOffset.value = (sentOffset.value - PAGE_SIZE).coerceAtLeast(0)
        else receivedOffset.value = (receivedOffset.value - PAGE_SIZE).coerceAtLeast(0)
    }
    fun selectSentMessage(id: Long) { selectedSentId.value = id }
    fun clearSelectedSentMessage() { selectedSentId.value = null }
    fun selectReceivedMessage(id: Long) { selectedReceivedId.value = id }
    fun clearSelectedReceivedMessage() { selectedReceivedId.value = null }
    fun refreshPermissionState() { errorMessage.value = null }

    fun retrySelectedReceivedMessage() {
        val smsId = selectedReceivedId.value ?: return
        viewModelScope.launch { webhookScheduler.scheduleReceivedSms(smsId, replaceExisting = true) }
    }

    fun sendTestSms() {
        if (isSending.value) return
        viewModelScope.launch {
            isSending.value = true
            errorMessage.value = null
            when (val result = sendSmsUseCase(
                SendSmsCommand(
                    to = destination.value,
                    message = message.value,
                    clientReference = clientReference.value.ifBlank { null },
                    subscriptionId = subscriptionId.value.toIntOrNull(),
                ),
            )) {
                is AppResult.Success -> currentMessageId.value = result.value.messageId
                is AppResult.Failure -> errorMessage.value = result.error.message
            }
            isSending.value = false
        }
    }

    private fun estimateSegments(message: String): Int {
        if (message.isBlank()) return 1
        if (message.length <= 160) return 1
        return ceil(message.length / 153.0).toInt()
    }
}

@Composable
fun MessagesRoute(
    onCopyText: (String, String) -> Unit,
    onShowSnackbar: suspend (String) -> Unit,
    viewModel: MessagesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sendPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshPermissionState()
    }
    val receivePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshPermissionState()
    }
    MessagesScreen(
        uiState = uiState,
        onSelectTab = viewModel::selectTab,
        onDestinationChange = viewModel::updateDestination,
        onMessageChange = viewModel::updateMessage,
        onClientReferenceChange = viewModel::updateClientReference,
        onSubscriptionIdChange = viewModel::updateSubscriptionId,
        onSend = viewModel::sendTestSms,
        onRequestSendPermission = { sendPermissionLauncher.launch(Manifest.permission.SEND_SMS) },
        onRequestReceivePermission = { receivePermissionLauncher.launch(Manifest.permission.RECEIVE_SMS) },
        onSearchFilterChange = viewModel::updateSearchFilter,
        onStatusFilterChange = viewModel::updateStatusFilter,
        onToggleSortDirection = viewModel::toggleSortDirection,
        onPreviousPage = viewModel::previousPage,
        onNextPage = viewModel::nextPage,
        onSelectSentMessage = viewModel::selectSentMessage,
        onClearSelectedSentMessage = viewModel::clearSelectedSentMessage,
        onSelectReceivedMessage = viewModel::selectReceivedMessage,
        onClearSelectedReceivedMessage = viewModel::clearSelectedReceivedMessage,
        onRetrySelectedReceivedMessage = viewModel::retrySelectedReceivedMessage,
        onCopyText = onCopyText,
    )
}

@Composable
fun MessagesScreen(
    uiState: MessagesUiState,
    onSelectTab: (MessageTab) -> Unit,
    onDestinationChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onClientReferenceChange: (String) -> Unit,
    onSubscriptionIdChange: (String) -> Unit,
    onSend: () -> Unit,
    onRequestSendPermission: () -> Unit,
    onRequestReceivePermission: () -> Unit,
    onSearchFilterChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onToggleSortDirection: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onSelectSentMessage: (Long) -> Unit,
    onClearSelectedSentMessage: () -> Unit,
    onSelectReceivedMessage: (Long) -> Unit,
    onClearSelectedReceivedMessage: () -> Unit,
    onRetrySelectedReceivedMessage: () -> Unit,
    onCopyText: (String, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Messages", style = MaterialTheme.typography.headlineSmall)
        if (!uiState.canSendSms) {
            WarningBanner("SEND_SMS permission is required for test sends and outbound API messages.")
            PrimaryOperationButton(text = "Request SMS Permission", onClick = onRequestSendPermission, modifier = Modifier.fillMaxWidth())
        }
        if (!uiState.canReceiveSms) {
            WarningBanner("RECEIVE_SMS permission is required to populate the inbox and webhook queue.")
            PrimaryOperationButton(text = "Request Receive Permission", onClick = onRequestReceivePermission, modifier = Modifier.fillMaxWidth())
        }

        SectionHeader("Test SMS")
        OutlinedTextField(value = uiState.destination, onValueChange = onDestinationChange, label = { Text("Destination") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = uiState.message, onValueChange = onMessageChange, label = { Text("Message") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
        OutlinedTextField(value = uiState.clientReference, onValueChange = onClientReferenceChange, label = { Text("Client reference") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = uiState.subscriptionId,
            onValueChange = onSubscriptionIdChange,
            label = { Text("Subscription ID") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        StatusCard(
            title = "Message estimate",
            value = "${uiState.characterCount} chars",
            detail = "Estimated segments ${uiState.estimatedSegments}",
        )
        PrimaryOperationButton(text = if (uiState.isSending) "Sending..." else "Send Test SMS", onClick = onSend, enabled = !uiState.isSending, modifier = Modifier.fillMaxWidth())
        uiState.currentMessageId?.let {
            StatusCard("Latest send", "Message #$it", "Current status ${uiState.currentStatus ?: "PENDING"}")
        }
        uiState.errorMessage?.let { WarningBanner(it) }

        TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
            MessageTab.entries.forEach { tab ->
                Tab(selected = uiState.selectedTab == tab, onClick = { onSelectTab(tab) }, text = { Text(tab.name) })
            }
        }

        SectionHeader("Filters")
        OutlinedTextField(value = uiState.searchFilter, onValueChange = onSearchFilterChange, label = { Text(if (uiState.selectedTab == MessageTab.SENT) "Destination search" else "Sender search") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = uiState.statusFilter, onValueChange = onStatusFilterChange, label = { Text("Status filter") }, modifier = Modifier.fillMaxWidth())
        SecondaryOperationButton(text = "Sort: ${uiState.sortDirection}", onClick = onToggleSortDirection, modifier = Modifier.fillMaxWidth())

        if (uiState.selectedTab == MessageTab.SENT) {
            SectionHeader("Sent Messages")
            if (uiState.sentItems.isEmpty()) {
                StatusCard("Sent queue", "No sent messages", "API and manual sends will appear here.")
            } else {
                uiState.sentItems.forEach { item ->
                    StatusCard(
                        title = UiFormatters.phonePreview(item.toNumber),
                        value = item.status,
                        detail = "${UiFormatters.messagePreview(item.message)} • ${UiFormatters.localTimestamp(item.createdAt)} • Segments ${item.segmentCount}",
                        modifier = Modifier.clickable { onSelectSentMessage(item.id) },
                    )
                }
            }
        } else {
            SectionHeader("Received Messages")
            if (uiState.receivedItems.isEmpty()) {
                StatusCard("Inbox", "No received messages", "Incoming SMS and webhook queue items will appear here.")
            } else {
                uiState.receivedItems.forEach { item ->
                    StatusCard(
                        title = item.fromNumber,
                        value = item.forwardStatus,
                        detail = "${UiFormatters.messagePreview(item.message)} • ${UiFormatters.localTimestamp(item.receivedAt)} • Retries ${item.retryCount}",
                        modifier = Modifier.clickable { onSelectReceivedMessage(item.id) },
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryOperationButton(text = "Previous", onClick = onPreviousPage, enabled = if (uiState.selectedTab == MessageTab.SENT) uiState.sentOffset > 0 else uiState.receivedOffset > 0, modifier = Modifier.weight(1f))
            SecondaryOperationButton(text = "Next", onClick = onNextPage, enabled = if (uiState.selectedTab == MessageTab.SENT) uiState.sentItems.size == PAGE_SIZE else uiState.receivedItems.size == PAGE_SIZE, modifier = Modifier.weight(1f))
        }

        uiState.selectedSentItem?.let { selected ->
            SectionHeader("Sent Detail")
            StatusCard("Destination", selected.toNumber, "Created ${UiFormatters.localTimestamp(selected.createdAt)}")
            StatusCard("Message", selected.message, "Client reference ${selected.clientReference ?: "n/a"}")
            StatusCard(
                "Timeline",
                selected.status,
                "Sending ${UiFormatters.localTimestamp(selected.sendingAt)} • Sent ${UiFormatters.localTimestamp(selected.sentAt)} • Delivered ${UiFormatters.localTimestamp(selected.deliveredAt)} • Failed ${UiFormatters.localTimestamp(selected.failedAt)}",
            )
            StatusCard(
                "Routing",
                "Subscription ${selected.subscriptionId ?: "n/a"}",
                "SIM slot ${selected.simSlot ?: "n/a"} • Segments ${selected.segmentCount} • Error ${selected.errorCode ?: "none"}",
            )
            selected.errorMessage?.let { WarningBanner(it) }
            SecondaryOperationButton(text = "Copy Message", onClick = { onCopyText("Message", selected.message) }, modifier = Modifier.fillMaxWidth())
            SecondaryOperationButton(text = "Close Detail", onClick = onClearSelectedSentMessage, modifier = Modifier.fillMaxWidth())
        }

        uiState.selectedReceivedItem?.let { selected ->
            SectionHeader("Received Detail")
            StatusCard("Sender", selected.fromNumber, "Received ${UiFormatters.localTimestamp(selected.receivedAt)}")
            StatusCard("Message", selected.message, "Subscription ${selected.subscriptionId ?: "n/a"} • SIM slot ${selected.simSlot ?: "n/a"}")
            StatusCard(
                "Webhook state",
                selected.forwardStatus,
                "Retries ${selected.retryCount} • Last ${UiFormatters.localTimestamp(selected.lastForwardAttemptAt)} • Next ${UiFormatters.localTimestamp(selected.nextRetryAt)} • HTTP ${selected.webhookResponseCode ?: "n/a"}",
            )
            selected.errorCode?.let { WarningBanner("${selected.errorCode}: ${selected.errorMessage ?: "No additional details"}") }
            if (uiState.selectedAttempts.isEmpty()) {
                StatusCard("Attempt history", "No attempts recorded", "This message may still be queued or webhook forwarding may be disabled.")
            } else {
                uiState.selectedAttempts.forEach { attempt ->
                    StatusCard(
                        title = "Attempt ${attempt.attemptNumber}",
                        value = if (attempt.success) "Success" else (attempt.errorCode ?: "Failed"),
                        detail = "${UiFormatters.localTimestamp(attempt.attemptedAt)} • HTTP ${attempt.responseCode ?: "n/a"} • ${UiFormatters.duration(attempt.durationMs)} • ${attempt.responseBodySummary ?: "No body"}",
                    )
                }
            }
            SecondaryOperationButton(
                text = if (uiState.retryScheduled) "Retry Scheduled" else "Retry Webhook",
                onClick = onRetrySelectedReceivedMessage,
                enabled = !uiState.retryScheduled && selected.forwardStatus != "FORWARDED",
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryOperationButton(text = "Close Detail", onClick = onClearSelectedReceivedMessage, modifier = Modifier.fillMaxWidth())
        }
    }
}

private data class MessageEditorState(
    val selectedTab: MessageTab,
    val destination: String,
    val message: String,
    val clientReference: String,
    val subscriptionId: String,
    val isSending: Boolean,
    val currentMessageId: Long?,
    val errorMessage: String?,
    val currentStatus: SentSmsEntity?,
)

private data class MessageListState(
    val searchFilter: String,
    val statusFilter: String,
    val sortDirection: SortDirection,
    val sentOffset: Int,
    val receivedOffset: Int,
    val sentItems: List<SentSmsEntity>,
    val receivedItems: List<ReceivedSmsEntity>,
    val selectedSentItem: SentSmsEntity?,
    val selectedReceivedItem: ReceivedSmsEntity?,
    val selectedAttempts: List<WebhookAttemptEntity>,
    val retryScheduled: Boolean,
)

private data class EditorIdentityState(
    val selectedTab: MessageTab,
    val destination: String,
    val message: String,
    val clientReference: String,
    val subscriptionId: String,
)

private data class EditorDeliveryState(
    val isSending: Boolean,
    val currentMessageId: Long?,
    val errorMessage: String?,
    val currentStatus: SentSmsEntity?,
)
