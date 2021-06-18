package com.example.vkvideoloader.application

import android.app.Activity
import android.app.Application
import com.example.vkvideoloader.WelcomeActivity
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKTokenExpiredHandler
import timber.log.Timber


class VkLoaderApp: Application() {

    var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        VK.addTokenExpiredHandler(tokenTracker)
        registerActivityLifecycleCallbacks(FocusCallback())
    }

    private val tokenTracker = object: VKTokenExpiredHandler {
        override fun onTokenExpired() {
            WelcomeActivity.startFrom(this@VkLoaderApp)
        }
    }

}