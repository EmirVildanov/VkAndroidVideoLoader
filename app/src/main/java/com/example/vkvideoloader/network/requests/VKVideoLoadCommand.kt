package com.example.vkvideoloader.network.requests

import android.content.ContentResolver
import android.net.Uri
import com.example.vkvideoloader.network.models.VKServerVideoUploadInfo
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
import java.io.InputStream
import java.net.UnknownHostException


class VKVideoLoadCommand(
    private val contentResolver: ContentResolver,
    private val videos: List<Uri> = listOf(),
    private val videoName: String,
) : ApiCommand<Int>() {
    override fun onExecute(manager: VKApiManager): Int {
        if (videos.size == 1) {
            val uploadUrl = getServerUploadInfo(manager, videoName)?.uploadUrl ?: return 1
            val videoUri = videos[0]

            val uploadFlag = 0
            when (uploadFlag) {
                0 -> easyVideoUpload(uploadUrl, videoUri)
                1 -> requestListenerVideoUpload1(uploadUrl, videoUri)
                2 -> requestListenerVideoUpload2(uploadUrl, videoUri)
            }
            return 0
        } else {
            throw RuntimeException("SIZE OF PASSED VIDEO ARRAY IS NOT 1")
        }
    }

    private fun easyVideoUpload(uploadUrl: String, videoUri: Uri) {
        val client = OkHttpClient.Builder().build()
        val byteArray = contentResolver.openInputStream(videoUri)!!.readBytes()

        val requestBody: RequestBody =
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

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }
            for ((name, value) in response.headers) {
                Timber.i("Header name: value: $name: $value")
            }
            Timber.i("Response body: ${response.body!!.string()}")
        }
    }


    private fun requestListenerVideoUpload1(uploadUrl: String, videoUri: Uri) {
        val byteArray = contentResolver.openInputStream(videoUri)!!.readBytes()
        val contentType = contentResolver.getType(videoUri);
        val fileDescriptor = contentResolver.openAssetFileDescriptor(videoUri, "r")!!;
        Timber.i("Length of uploading video is ${fileDescriptor.declaredLength} bytes and ${fileDescriptor.length} bytes")
        Timber.i("Content-type is $contentType")
        val countingProgressListener = object : CountingRequestBody1.Listener {
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
                    Timber.i("${100 * bytesWritten / contentLength}% uploading done\n")
                }
            }
        }

        val videoFile: RequestBody = object : RequestBody() {
            override fun writeTo(sink: BufferedSink) {
                fileDescriptor.createInputStream()
                    .use { inputStream -> sink.writeAll(inputStream.source().buffer()) }
//                fileDescriptor.createInputStream().use { inputStream ->
//                    val bytes = inputStream.readBytes()
//                    val bytesStep = 100000
//                    Timber.i("The length of bytes is ${bytes.size}")
//                    for (i in bytes.indices step bytesStep) {
//                        val currentBytesArray: ByteArray
//                        if (i + bytesStep < bytes.size) {
////                            inputStream.read(bytesBuffer, i, bytesStep)
//                            Timber.i("Current range is i is ${i until i + bytesStep}")
//                            currentBytesArray = bytes.slice(i until i + bytesStep).toByteArray()
//                            sink.write(currentBytesArray)
//                        } else {
//                            Timber.i("Current range is i is ${i until bytes.size}")
//                            currentBytesArray = bytes.slice(i until bytes.size).toByteArray()
//                            sink.write(currentBytesArray)
//                        }
//                        Timber.i("CurrentBytesArray: $currentBytesArray")
//                        Timber.i("The length of bytes is ${bytes.size}")
//                    }
//                }
            }

            override fun contentLength(): Long {
                return fileDescriptor.length
            }

            override fun contentType(): MediaType? {
                return contentType?.toMediaTypeOrNull()
            }
        }

        val chunkedRequestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "fname", RequestBody.create("video/mp4".toMediaTypeOrNull(), byteArray))
            .build()

        val countingRequestBody =
            CountingRequestBody1(chunkedRequestBody, countingProgressListener)

        val request: Request = Request.Builder()
            .url(uploadUrl)
            .post(countingRequestBody)
            .build()

        val client = OkHttpClient.Builder()
            .socketFactory(ProgressFriendlySocketFactory())
            .build()

//        client.newCall(request).execute().use { response ->
//            if (!response.isSuccessful) {
//                fileDescriptor.close()
//                throw IOException("Unexpected code $response")
//            }
//            fileDescriptor.close()
//        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                }
            }
        })
    }

    private fun requestListenerVideoUpload2(uploadUrl: String, videoUri: Uri) {
        val contentType = "video/mp4";
        val videoInputStream = contentResolver.openInputStream(videoUri)!!;
        val videoLength = videoInputStream.available().toLong()

        Timber.i("Video length is $videoLength bytes")

        val countingProgressListener = object : CountingRequestBody1.Listener {
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
                    Timber.i("${100 * bytesWritten / contentLength}% uploading done\n")
                }
            }
        }

        val videoFile: RequestBody = object : RequestBody() {
            override fun writeTo(sink: BufferedSink) {
                videoInputStream.use { inputStream -> sink.writeAll(inputStream.source().buffer()) }

//                videoInputStream.use { inputStream ->
//                    val bytes = inputStream.readBytes()
//                    val bytesBuffer: ByteArray
//                    val bytesStep = 100000
//                    Timber.i("The length of bytes is ${bytes.size}")
//                    for (i in bytes.indices step bytesStep) {
//                        val currentBytesArray: ByteArray
//                        if (i + bytesStep < bytes.size) {
////                            inputStream.read(bytesBuffer, i, bytesStep)
//                            Timber.i("Current range is i is ${i until i + bytesStep}")
//                            currentBytesArray = bytes.slice(i until i + bytesStep).toByteArray()
//                            sink.write(currentBytesArray)
//                        } else {
//                            Timber.i("Current range is i is ${i until bytes.size}")
//                            currentBytesArray = bytes.slice(i until bytes.size).toByteArray()
//                            sink.write(currentBytesArray)
//                        }
//                        Timber.i("CurrentBytesArray: $currentBytesArray")
//                        Timber.i("The length of bytes is ${bytes.size}")
//                    }
//                }

            }

            override fun contentLength(): Long {
                return videoLength
            }

            override fun contentType(): MediaType? {
                return contentType.toMediaTypeOrNull()
            }
        }

        val byteArray = contentResolver.openInputStream(videoUri)!!.readBytes()
        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "fname", RequestBody.create("video/mp4".toMediaTypeOrNull(), byteArray))
            .build()

        val countingRequestBody =
            CountingRequestBody1(requestBody, countingProgressListener)

        val request: Request = Request.Builder()
            .url(uploadUrl)
            .post(countingRequestBody)
//            .addHeader("Session-Id", videoUri.toString())
//            .addHeader("Content-type", contentType)
            .build()

        val client = OkHttpClient.Builder()
//            .socketFactory(ProgressFriendlySocketFactory())
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
//                Timber.e(response.toString())
                Timber.i("Request headers: ${request.headers}")
                Timber.i("Request: ${request}")
                IOException("Unexpected code $response").printStackTrace()
            }

            for ((name, value) in response.headers) {
                Timber.i("$name: $value")
            }

            Timber.i(response.body!!.string())
        }

//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                e.printStackTrace()
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                response.use {
//                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
//
//                }
//            }
//        })
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