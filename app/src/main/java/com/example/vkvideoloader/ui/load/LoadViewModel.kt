package com.example.vkvideoloader.ui.load

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import timber.log.Timber

class LoadViewModel : ViewModel() {

    val _videoLoadingPercentage = MutableLiveData(0)
    val videoLoadingPercentage: LiveData<Int>
        get() = _videoLoadingPercentage


    val _isUploading = MutableLiveData(true)
    val isUploading: LiveData<Boolean>
        get() = _isUploading

    val _isLoadingCanceled = MutableLiveData(false)
    val isLoadingCanceled: LiveData<Boolean>
        get() = _isLoadingCanceled

    private val _loadWhileInBackgroundCheck = MutableLiveData<Boolean>()
    val loadWhileInBackgroundCheck: LiveData<Boolean>
        get() = _loadWhileInBackgroundCheck

    fun changeLoadWhileInBackgroundCheck(value: Boolean? = null) {
        if (value != null) {
            _loadWhileInBackgroundCheck.value = value!!
        }
        _loadWhileInBackgroundCheck.value = !loadWhileInBackgroundCheck.value!!
    }

    fun changeUploadingStatus(value: Boolean? = null) {
        if (value != null) {
            _isUploading.value = value
        } else {
            _isUploading.value = !_isUploading.value!!
        }
        Timber.i("Uploading is ${_isUploading.value}")
    }
}