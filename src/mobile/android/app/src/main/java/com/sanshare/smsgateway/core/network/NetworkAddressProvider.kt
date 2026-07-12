package com.sanshare.smsgateway.core.network

import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkAddressProvider @Inject constructor() {
    fun bestAddress(): String? {
        val interfaces = runCatching { NetworkInterface.getNetworkInterfaces()?.toList().orEmpty() }.getOrDefault(emptyList())
        val preferred = interfaces.asSequence()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .sortedBy { priority(it.name) }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<Inet4Address>()
            .filterNot { it.isLoopbackAddress || it.isLinkLocalAddress || it.isAnyLocalAddress }
            .map { it.hostAddress }
            .firstOrNull()
        return preferred?.takeIf { it.isNotBlank() }
    }

    fun baseUrl(port: Int): String? = bestAddress()?.let { "http://$it:$port" }

    private fun priority(name: String?): Int {
        val normalized = name.orEmpty().lowercase()
        return when {
            normalized.startsWith("wlan") || normalized.startsWith("wifi") -> 0
            normalized.startsWith("eth") || normalized.startsWith("en") -> 1
            normalized.startsWith("rmnet") || normalized.startsWith("ccmni") -> 2
            else -> 3
        }
    }
}
