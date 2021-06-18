package com.example.vkvideoloader.application

import android.app.Activity
import android.app.Application
import android.os.Bundle

class FocusCallback() : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {
        val app = activity.applicationContext as VkLoaderApp
        app.currentActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {
        val app = activity.applicationContext as VkLoaderApp
        app.currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        val app = activity.applicationContext as VkLoaderApp
        app.currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        clearReferences(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        clearReferences(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        clearReferences(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        clearReferences(activity)
    }

    private fun clearReferences(activity: Activity) {
        val app = activity.applicationContext as VkLoaderApp
        val currActivity: Activity? = app.currentActivity
        if (activity == currActivity) {
            app.currentActivity = null
        }
    }
}