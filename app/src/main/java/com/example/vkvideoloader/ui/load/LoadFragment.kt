package com.example.vkvideoloader.ui.load

import android.app.Activity
import android.content.Intent
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
import com.example.vkvideoloader.databinding.FragmentLoadBinding
import com.example.vkvideoloader.network.requests.VKVideoLoadCommand
import com.example.vkvideoloader.utils.PathUtils
import com.example.vkvideoloader.utils.SharedPreferencesWorker
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import timber.log.Timber
import java.io.File

class LoadFragment : Fragment() {

    private lateinit var viewModel: LoadViewModel

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data != null && data.data != null) {
                activity?.let {
                    val videoName = PathUtils.getName(requireActivity(), data.data!!)
                    loadVideo(data.data, videoName)
                }
            } else {
                Timber.i("Sending post without photo")
                loadVideo()
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

        binding.loadWhileInBackgroundSwitch.setOnCheckedChangeListener { _ : CompoundButton, isChecked: Boolean ->
            viewModel.changeLoadWhileInBackgroundCheck(isChecked)
            activity?.let { SharedPreferencesWorker.putBooleans(it, mapOf(R.bool.saved_load_while_in_background_key.toString() to isChecked)) }
            Timber.i("Current <loadWhileInBackgroundCheck> is $isChecked")
        }

        binding.loadNewVideoButton.setOnClickListener {
            requestVideo()
        }

        return binding.root
    }

    fun requestVideo() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    private fun loadVideo(uri: Uri? = null, videoName: String = "EMPTY_NAME") {
        val videos = ArrayList<Uri>()
        uri?.let {
            videos.add(it)
        }
        VK.execute(VKVideoLoadCommand(requireActivity().contentResolver, videos, videoName), object :
            VKApiCallback<Int> {
            override fun success(result: Int) {
                if (result == 1) {
                    Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "Video loaded", Toast.LENGTH_SHORT).show()
                    Timber.i("Successfully loaded video $videoName")
                }
            }

            override fun fail(error: Exception) {
                Timber.e("Unable to load video")
                Timber.e(error.toString())
//                throw error
            }
        })
    }
}