package com.example.vkvideoloader.network.models

import android.graphics.Bitmap

data class Video(
    val image: Bitmap? = null,
    val name: String = "EMPTY_VIDEO_NAME",
)
