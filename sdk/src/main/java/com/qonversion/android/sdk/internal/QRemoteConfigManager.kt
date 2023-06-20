package com.qonversion.android.sdk.internal

import com.qonversion.android.sdk.dto.QRemoteConfig
import com.qonversion.android.sdk.dto.QonversionError
import com.qonversion.android.sdk.internal.services.QRemoteConfigService
import com.qonversion.android.sdk.internal.services.QUserInfoService
import com.qonversion.android.sdk.listeners.QonversionExperimentAttachCallback
import com.qonversion.android.sdk.listeners.QonversionRemoteConfigCallback
import javax.inject.Inject
import kotlin.math.exp

internal class QRemoteConfigManager @Inject constructor(
    private val remoteConfigService: QRemoteConfigService,
    private val userInfoService: QUserInfoService,
    private val internalConfig: InternalConfig
) {
    private var currentRemoteConfig: QRemoteConfig? = null
    private var isLaunchFinished: Boolean = false
    private var remoteConfigCallbacks = mutableListOf<QonversionRemoteConfigCallback>()
    private var isRequestInProgress: Boolean = false

    fun onLaunchFinished(success: Boolean) {
        isLaunchFinished = success

        if (success && remoteConfigCallbacks.isNotEmpty() && !isRequestInProgress) {
            loadRemoteConfig(null)
        }
    }

    fun loadRemoteConfig(callback: QonversionRemoteConfigCallback?) {
        currentRemoteConfig?.let {
            callback?.onSuccess(it)
            return
        }

        if (!isLaunchFinished || isRequestInProgress) {
            callback?.let {
                remoteConfigCallbacks.add(it)
                return
            }
        }

        isRequestInProgress = true
        remoteConfigService.loadRemoteConfig(internalConfig.uid, object : QonversionRemoteConfigCallback {
            override fun onSuccess(remoteConfig: QRemoteConfig) {
                isRequestInProgress = false
                currentRemoteConfig = remoteConfig
                fireToCallbacks { onSuccess(remoteConfig) }
            }

            override fun onError(error: QonversionError) {
                isRequestInProgress = false
                fireToCallbacks { onError(error) }
            }

        })
    }

    fun attachUserToExperiment(experimentId: String, groupId: String, callback: QonversionExperimentAttachCallback) {
        val userId = userInfoService.obtainUserID()
        remoteConfigService.attachUserToExperiment(experimentId, groupId, userId, callback)
    }

    fun detachUserToExperiment(experimentId: String, callback: QonversionExperimentAttachCallback) {
        val userId = userInfoService.obtainUserID()
        remoteConfigService.detachUserToExperiment(experimentId, userId, callback)
    }

    private fun fireToCallbacks(action: QonversionRemoteConfigCallback.() -> Unit) {
        val callbacks = remoteConfigCallbacks.toList()
        callbacks.forEach { it.action() }
        remoteConfigCallbacks.clear()
    }
}
