package com.sanshare.smsgateway.data.repository

import com.sanshare.smsgateway.domain.model.GatewayServiceState
import com.sanshare.smsgateway.domain.repository.GatewayStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryGatewayStateRepository @Inject constructor() : GatewayStateRepository {
    private val mutableState = MutableStateFlow(GatewayServiceState.STOPPED)
    override val state: StateFlow<GatewayServiceState> = mutableState

    override fun setState(state: GatewayServiceState) {
        mutableState.value = state
    }
}
