package com.example.vkvideoloader.network.requests

import com.example.vkvideoloader.ui.load.LoadViewModel
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.*
import timber.log.Timber
import java.io.IOException


class CountingRequestBody(
    private var delegate: RequestBody,
    private var listener: Listener,
    private var viewModel: LoadViewModel
) : RequestBody() {

    private var countingSink: CountingSink? = null

    override fun contentType(): MediaType? {
        return delegate.contentType()
    }

    override fun contentLength(): Long {
        try {
            return delegate.contentLength()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return -1
    }

    override fun writeTo(sink: BufferedSink) {
        countingSink = CountingSink(sink)
        val bufferedSink = countingSink!!.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
        private var bytesWritten: Long = 0

        override fun write(source: Buffer, byteCount: Long) {
            while (!viewModel._isUploading.value!!) {
                Timber.i("Not uploading video because process is stopped")
                Thread.sleep(UPLOADING_RESUME_WAIT_TIME_MILLISECONDS)
            }
            super.write(source, byteCount)
            bytesWritten += byteCount
            listener.onRequestProgress(bytesWritten, contentLength())
        }
    }

    interface Listener {
        fun onRequestProgress(bytesWritten: Long, contentLength: Long)
    }

    companion object {
        const val UPLOADING_RESUME_WAIT_TIME_MILLISECONDS = 500L
    }
}