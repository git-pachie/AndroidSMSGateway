package com.sanshare.smsgateway.core.network

import javax.inject.Inject

class NetworkAddressProvider @Inject constructor() {
    fun placeholderAddress(): String = "Server pending"
}
