package com.qonversion.android.sdk.old.billing

import com.android.billingclient.api.BillingClient

data class BillingError(
    @BillingClient.BillingResponseCode val billingResponseCode: Int,
    val message: String
)
