package com.sanshare.smsgateway.receiver

object SmsReceiverContract {
    const val ACTION_SMS_SENT = "com.sanshare.smsgateway.ACTION_SMS_SENT"
    const val ACTION_SMS_DELIVERED = "com.sanshare.smsgateway.ACTION_SMS_DELIVERED"

    const val EXTRA_MESSAGE_ID = "message_id"
    const val EXTRA_SEGMENT_INDEX = "segment_index"
    const val EXTRA_TOTAL_SEGMENTS = "total_segments"
}
