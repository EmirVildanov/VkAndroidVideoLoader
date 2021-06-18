package com.example.vkvideoloader.ui.welcome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.vkvideoloader.application.VkLoaderApp
import com.vk.api.sdk.VK
import com.vk.api.sdk.auth.VKScope
import timber.log.Timber

class WelcomeViewModel(application: Application) : AndroidViewModel(application) {
    fun logIn() {
        val activity = getApplication<VkLoaderApp>().currentActivity
        if (activity != null) {
            VK.login(activity, arrayListOf(VKScope.WALL, VKScope.PHOTOS, VKScope.VIDEO))
        }
        else {
            Timber.i("Current activity is null")
        }
    }
}