package com.example.vkvideoloader.ui.load

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoadViewModel : ViewModel() {

    private val _loadWhileInBackgroundCheck = MutableLiveData<Boolean>()
    val loadWhileInBackgroundCheck: LiveData<Boolean>
        get() = _loadWhileInBackgroundCheck

    fun changeLoadWhileInBackgroundCheck(value: Boolean? = null) {
        if (value != null) {
            _loadWhileInBackgroundCheck.value = value!!
        }
        _loadWhileInBackgroundCheck.value = !loadWhileInBackgroundCheck.value!!
    }
}