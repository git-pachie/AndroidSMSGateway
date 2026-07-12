package com.sanshare.smsgateway.domain.repository

import com.sanshare.smsgateway.domain.model.GatewayServiceState
import kotlinx.coroutines.flow.StateFlow

interface GatewayStateRepository {
    val state: StateFlow<GatewayServiceState>
    fun setState(state: GatewayServiceState)
}
