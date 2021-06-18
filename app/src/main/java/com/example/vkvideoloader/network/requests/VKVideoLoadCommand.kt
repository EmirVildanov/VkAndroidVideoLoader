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


class VKVideoLoadCommand(
    private val contentResolver: ContentResolver,
    private val videos: List<Uri> = listOf(),
    private val videoName: String,
    private val viewModel: LoadViewModel
) : ApiCommand<Int>() {
    override fun onExecute(manager: VKApiManager): Int {
        if (videos.size == 1) {
            val uploadUrl = getServerUploadInfo(manager, videoName)?.uploadUrl ?: return 1
            val videoUri = videos[0]

            val uploadFlag = 2
            when (uploadFlag) {
                0 -> easyVideoUpload(uploadUrl, videoUri)
                2 -> uploadVideoWithByteArray(uploadUrl, videoUri)
            }
            return 0
        } else {
            throw RuntimeException("SIZE OF PASSED VIDEO ARRAY IS NOT 1")
        }
    }

    private fun easyVideoUpload(uploadUrl: String, videoUri: Uri) {
        val client = OkHttpClient.Builder().build()
        val byteArray = contentResolver.openInputStream(videoUri)!!.readBytes()

        val requestBody =
            MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", "TEST_FILE_NAME",
                    RequestBody.create("video/mp4".toMediaTypeOrNull(), byteArray)
                )
                .build()


        val request: Request = Request.Builder()
            .url(uploadUrl)
            .post(requestBody)
            .build()

        Timber.i(request.toString())
        Timber.i(requestBody.toString())

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }
        }
    }


    private fun uploadVideoWithByteArray(uploadUrl: String, videoUri: Uri) {
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

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                videoInputStream.use { sink.writeAll(it.source().buffer()) }
            }
        }

        Timber.i("Video length is $videoLength bytes")

        val countingProgressListener = object : CountingRequestBody.Listener {
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
                Timber.i("Bytes written: $bytesWritten")
                if (contentLength != -1L) {
                    val percentage = 100 * bytesWritten / contentLength
                    viewModel._videoLoadingPercentage.postValue((percentage.toInt()))
                    Timber.i("$percentage% uploading done\n")
                }
            }
        }

        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "fname",
                videoFile
            )
            .build()

        val countingRequestBody =
            CountingRequestBody(requestBody, countingProgressListener)

        val request: Request = Request.Builder()
            .url(uploadUrl)
            .post(countingRequestBody)
            .addHeader("Session-Id", videoUri.toString())
            .build()

        val client = OkHttpClient.Builder()
            .socketFactory(ProgressFriendlySocketFactory())
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                IOException("Unexpected code $response").printStackTrace()
            }

            Timber.i("Response headers:")
            for ((name, value) in response.headers) {
                Timber.i("$name: $value")
            }
            Timber.i("Response body:")
            Timber.i(response.body!!.string())
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