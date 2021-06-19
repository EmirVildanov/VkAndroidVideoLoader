package com.example.vkvideoloader.ui.load

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.vkvideoloader.R
import com.example.vkvideoloader.application.VkLoaderApp
import com.example.vkvideoloader.databinding.FragmentLoadBinding
import com.example.vkvideoloader.network.models.Video
import com.example.vkvideoloader.network.requests.VKVideoLoadCommand
import com.example.vkvideoloader.ui.ProgressBar
import com.example.vkvideoloader.utils.PathUtils
import com.example.vkvideoloader.utils.SharedPreferencesWorker
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import timber.log.Timber


class LoadFragment : Fragment() {

    private lateinit var viewModel: LoadViewModel
    private lateinit var videoUploadProgress: ProgressBar
    private lateinit var binding: FragmentLoadBinding

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (data != null && data.data != null) {
                    activity?.let {
                        val videoName = PathUtils.getName(requireActivity(), data.data!!)
                        loadVideo(data.data!!, videoName)
                    }
                } else {
                    Timber.i("No video was chosen")
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentLoadBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_load,
            container,
            false
        )

        viewModel = ViewModelProvider(this).get(LoadViewModel::class.java)

        binding.viewModel = viewModel

        this.binding = binding

        binding.cancelLoadButton.isEnabled = false
        binding.stopResumeButton.isEnabled = false

        videoUploadProgress = binding.videoUploadProgress

        val changeLoadWhileInBackgroundCheck = SharedPreferencesWorker.getBoolean(
            requireActivity(),
            resources.getBoolean(R.bool.saved_load_while_in_background_key)
        )
        binding.loadWhileInBackgroundSwitch.isChecked = changeLoadWhileInBackgroundCheck

        binding.loadWhileInBackgroundSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            binding.loadWhileInBackgroundSwitch.isChecked = isChecked
            activity?.let {
                SharedPreferencesWorker.putBooleans(
                    it,
                    mapOf(resources.getBoolean(R.bool.saved_load_while_in_background_key) to isChecked)
                )
            }
        }

        binding.loadNewVideoButton.setOnClickListener {
            requestVideo()
        }

        binding.stopResumeButton.setOnClickListener {
            viewModel.changeUploadingStatus()
        }

        binding.cancelLoadButton.setOnClickListener {
            viewModel._isLoadingCanceled.value = true
        }

        viewModel.videoLoadingPercentage.observe(requireActivity(), { percentage ->
//            Timber.i("Setting upload percentage: $percentage")
            setUploadingProgressBar(percentage)
        })

        return binding.root
    }

    private fun setUploadingProgressBar(percentage: Int) {
        videoUploadProgress.update(100, percentage)
        videoUploadProgress.invalidate()
    }

    private fun requestVideo() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    private fun loadVideo(uri: Uri, videoName: String = "EMPTY_NAME") {
        binding.cancelLoadButton.isEnabled = true
        binding.stopResumeButton.isEnabled = true

        binding.loadNewVideoButton.isEnabled = false;

        val videoLoadCommand = VKVideoLoadCommand(
            requireActivity().contentResolver,
            uri,
            videoName,
            viewModel
        )
        val callback = object : VKApiCallback<Int> {
            override fun success(result: Int) {
                if (result == 1) {
                    Toast.makeText(activity, "Unable to load video. Check your Internet connection", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "Video loaded", Toast.LENGTH_SHORT).show()
                    Timber.i("Successfully loaded video $videoName")
                    val app = requireActivity().applicationContext as VkLoaderApp
                    app.videos.add(Video(name = videoName, image = getVideoThumbnail(uri)))
                }
                resetButtons()
            }

            override fun fail(error: Exception) {
                Timber.e("Unable to load video: ")
                Timber.e(error.toString())
                resetButtons()
            }
        }

        VK.execute(videoLoadCommand, callback)
    }

    private fun getVideoThumbnail(videoUri: Uri): Bitmap? {
        val contentResolver = requireActivity().contentResolver

        return try {
            val id = ContentUris.parseId(videoUri)
            MediaStore.Video.Thumbnails.getThumbnail(
                contentResolver, id,
                MediaStore.Video.Thumbnails.MINI_KIND, null
            )
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun resetButtons() {
        binding.loadNewVideoButton.isEnabled = true
        binding.cancelLoadButton.isEnabled = false
        binding.stopResumeButton.isEnabled = false
    }

    override fun onStop() {
        super.onStop()
        if (!binding.loadWhileInBackgroundSwitch.isChecked) {
            viewModel.changeUploadingStatus(false)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!binding.loadWhileInBackgroundSwitch.isChecked) {
            viewModel.changeUploadingStatus(true)
        }
    }
}