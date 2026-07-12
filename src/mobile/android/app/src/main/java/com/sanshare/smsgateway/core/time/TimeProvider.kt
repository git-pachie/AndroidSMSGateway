package com.sanshare.smsgateway.core.time

import javax.inject.Inject

class TimeProvider @Inject constructor() {
    fun nowMillis(): Long = System.currentTimeMillis()
}
