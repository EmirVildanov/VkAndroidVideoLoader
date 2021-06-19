package com.example.vkvideoloader.network.requests

import android.content.ContentResolver
import android.net.Uri
import com.example.vkvideoloader.network.models.VKServerVideoUploadInfo
import com.example.vkvideoloader.ui.load.LoadViewModel
import com.vk.api.sdk.VKApiManager
import com.vk.api.sdk.VKApiResponseParser
import com.vk.api.sdk.VKMethodCall
import com.vk.api.sdk.exceptions.VKApiIllegalResponseException
import com.vk.api.sdk.internal.ApiCommand
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okio.*
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.net.UnknownHostException
import javax.net.ssl.SSLException


class VKVideoLoadCommand(
    private val contentResolver: ContentResolver,
    private val videoUri: Uri,
    private val videoName: String,
    private val viewModel: LoadViewModel
) : ApiCommand<Int>() {

    private val countingProgressListener = object : CountingRequestBody.Listener {
        var firstUpdate = true

        override fun onRequestProgress(bytesWritten: Long, contentLength: Long) {
            if (firstUpdate) {
                firstUpdate = false
                if (contentLength == -1L) {
                    Timber.i("Content-length: unknown")
                } else {
                    Timber.i("Content-length: $contentLength\n")
                }
            }
            if (contentLength != -1L) {
                val percentage = 100 * bytesWritten / contentLength
                viewModel._videoLoadingPercentage.postValue((percentage.toInt()))
                Timber.i("$percentage% video uploading done\n")
            }
        }
    }

    override fun onExecute(manager: VKApiManager): Int {
        val uploadUrl = getServerUploadInfo(manager, videoName)?.uploadUrl ?: return 1

        try {
            uploadVideoWithInputStream(uploadUrl, videoUri)
        } catch (e: SSLException) {
            e.printStackTrace()
            return 1
        }
        return 0
    }

    private fun uploadVideoWithInputStream(uploadUrl: String, videoUri: Uri) {
        val contentType = "video/mp4";
        val videoInputStream = contentResolver.openInputStream(videoUri)!!;
        val videoLength = videoInputStream.available().toLong()

        val videoFile: RequestBody = object : RequestBody() {
            override fun contentLength(): Long {
                return videoLength
            }

            override fun contentType(): MediaType? {
                return contentType.toMediaTypeOrNull()
            }

            override fun writeTo(sink: BufferedSink) {
                videoInputStream.use { sink.writeAll(it.source().buffer()) }
            }
        }

        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "filename",
                videoFile
            )
            .build()

        val countingRequestBody =
            CountingRequestBody(requestBody, countingProgressListener, viewModel)

        val request: Request = Request.Builder()
            .url(uploadUrl)
            .post(countingRequestBody)
            // This header can help to continue uploading after network break
            //.addHeader("Session-Id", videoUri.toString())
            .build()

        val client = OkHttpClient.Builder()
            .socketFactory(ProgressFriendlySocketFactory())
            .build()

        val videoUploadCall = client.newCall(request)
        var videoUploadCallFinished = false

        videoUploadCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                videoUploadCallFinished = true
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    videoUploadCallFinished = true
                }
            }
        })

        while (true) {
            if (viewModel._isLoadingCanceled.value!! or videoUploadCallFinished) {
                videoUploadCall.cancel()
                viewModel._isLoadingCanceled.postValue(false)
                return
            }
        }
    }

    private fun getServerUploadInfo(
        manager: VKApiManager,
        videoName: String
    ): VKServerVideoUploadInfo? {
        val uploadInfoCall = VKMethodCall.Builder()
            .method("video.save")
            .args("name", videoName)
            .version(manager.config.version)
            .build()
        return try {
            manager.execute(uploadInfoCall, UploadedVideoServerInfoParser())
        } catch (e: UnknownHostException) {
            null
        }
    }

    private class UploadedVideoServerInfoParser : VKApiResponseParser<VKServerVideoUploadInfo> {
        override fun parse(response: String): VKServerVideoUploadInfo {
            try {
                val joResponse = JSONObject(response).getJSONObject("response")
                return VKServerVideoUploadInfo(
                    uploadUrl = joResponse.getString("upload_url")
                )
            } catch (ex: JSONException) {
                throw VKApiIllegalResponseException(ex)
            }
        }
    }

}