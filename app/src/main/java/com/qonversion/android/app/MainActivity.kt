package com.qonversion.android.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.qonversion.android.sdk.Qonversion
import com.qonversion.android.sdk.QonversionCallback
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val skuDetailsMap = mutableMapOf<String, SkuDetails>()
    private val sku_purchase = "conversion_test_purchase"
    private val sku_subscription = "conversion_test_subscribe"
    private lateinit var billingClient : BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        billing_flow_purchase.setOnClickListener {
            launchBilling(sku_purchase, BillingClient.SkuType.INAPP)
        }

        billing_flow_subscription.setOnClickListener {
            launchBilling(sku_subscription, BillingClient.SkuType.SUBS)
        }

        initBilling()
    }

    private fun launchBilling(purchaseId: String, type: String) {
        var params = SkuDetailsParams.newBuilder()
            .setType(type)
            .setSkusList(listOf(purchaseId))
            .build()

        billingClient.querySkuDetailsAsync(params, object: SkuDetailsResponseListener {
            override fun onSkuDetailsResponse(
                billingResult: BillingResult?,
                skuDetailsList: MutableList<SkuDetails>?
            ) {
                if (billingResult!!.responseCode == 0) {
                    for (skuDetails in skuDetailsList!!) {
                        monitor.text = skuDetails.originalJson
                        skuDetailsMap[skuDetails.sku] = skuDetails
                    }
                    launchBillingFlow(purchaseId)
                }
            }
        })
    }

    private fun launchBillingFlow(purchaseId: String) {
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetailsMap[purchaseId])
            .build()
        billingClient.launchBillingFlow(this, billingFlowParams)
    }

    private fun initBilling() {
        billingClient = BillingClient
            .newBuilder(this)
            .enablePendingPurchases()
            .setListener { billingResult, purchases ->
            if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                // here purchase is accepted
                for (p in purchases) {
                    sendPurchase(p)
                }
            }
        }.build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                monitor.text = "Billing Connection failed"
            }

            override fun onBillingSetupFinished(billingResult: BillingResult?) {
                if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                    monitor.text = "Billing Connection successful"
                }
            }
        })
    }

    private fun sendPurchase(purchase: com.android.billingclient.api.Purchase) {
        clearMonitor()
        val details = skuDetailsMap[purchase.sku]
        if (details != null) {
            Qonversion.instance?.purchase(
                details,
                purchase,
                object: QonversionCallback {
                    override fun onSuccess(uid: String) {
                        monitor.text = "Success: ${uid}"
                    }

                    override fun onError(t: Throwable) {
                        monitor.text = "Failed: ${t.message}"
                    }
                }
            )
        }
    }

    private fun clearMonitor() {
        monitor.text = ""
    }
}