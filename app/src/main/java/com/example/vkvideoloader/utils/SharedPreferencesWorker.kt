package com.example.vkvideoloader.utils

import android.app.Activity
import androidx.preference.PreferenceManager
import com.example.vkvideoloader.R

object SharedPreferencesWorker {
    fun putStrings(activity: Activity, items: Map<String, String>) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        with(sharedPreferences.edit()) {
            for (item in items) {
                putString(item.key, item.value)
            }
            apply()
        }
    }

    fun putBooleans(activity: Activity, items: Map<Boolean, Boolean>) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        with(sharedPreferences.edit()) {
            for (item in items) {
                putBoolean(item.key.toString(), item.value)
            }
            apply()
        }
    }

    fun getString(activity: Activity, key: String): String? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        return sharedPreferences.getString(key, null)
    }

    fun getBoolean(activity: Activity, key: Boolean): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        return sharedPreferences.getBoolean(key.toString(), false)
    }
}