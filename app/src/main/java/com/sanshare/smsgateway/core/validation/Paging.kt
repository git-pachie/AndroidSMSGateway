package com.sanshare.smsgateway.core.validation

data class PagingRequest(
    val limit: Int = DEFAULT_LIMIT,
    val offset: Int = 0,
) {
    init {
        require(limit in 1..MAX_LIMIT) { "limit must be 1..$MAX_LIMIT" }
        require(offset >= 0) { "offset must be non-negative" }
    }

    companion object {
        const val DEFAULT_LIMIT = 50
        const val MAX_LIMIT = 100

        fun normalize(limit: Int?, offset: Int?): PagingRequest {
            val normalizedLimit = (limit ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)
            val normalizedOffset = (offset ?: 0).coerceAtLeast(0)
            return PagingRequest(normalizedLimit, normalizedOffset)
        }
    }
}
