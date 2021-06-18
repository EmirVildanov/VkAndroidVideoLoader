package com.example.vkvideoloader.ui.home

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.vkvideoloader.R
import com.example.vkvideoloader.WelcomeActivity
import com.example.vkvideoloader.databinding.FragmentHomeBinding
import com.example.vkvideoloader.network.models.VKUser
import com.example.vkvideoloader.network.models.Video
import com.example.vkvideoloader.network.requests.VKUsersCommand
import com.squareup.picasso.Picasso
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import timber.log.Timber

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentHomeBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_home,
            container,
            false
        )

        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        binding.viewModel = viewModel

        binding.logoutBtn.setOnClickListener {
            VK.logout()
            activity?.let { it1 -> WelcomeActivity.startFrom(it1) }
            activity?.finish()
        }

        requestUser(binding.nameTV, binding.avatarIV)

        val videos = listOf(Video(1), Video(2, "BABABA"), Video(3, "TESTBABATEST"))
        showVideos(binding.uploadedVideosRV, videos)

        return binding.root
    }

    private fun requestUser(nameTV: TextView, avatarIV: ImageView) {
        VK.execute(VKUsersCommand(), object : VKApiCallback<List<VKUser>> {
            override fun success(result: List<VKUser>) {
                if (activity != null && !activity!!.isFinishing && result.isNotEmpty()) {
                    val user = result[0]
                    nameTV.text = getString(R.string.user_info, user.firstName, user.lastName)
                    nameTV.setOnClickListener(createOnClickListener(user.id))

                    if (!TextUtils.isEmpty(user.photo)) {
                        Picasso.get()
                            .load(user.photo)
                            .error(R.drawable.ic_account_black_24dp)
                            .into(avatarIV)
                    } else {
                        avatarIV.setImageResource(R.drawable.ic_account_black_24dp)
                    }
                    avatarIV.setOnClickListener(createOnClickListener(user.id))
                }
            }

            override fun fail(error: Exception) {
                Timber.e(error.toString())
            }
        })
    }

    private fun showVideos(friendsRecyclerView: RecyclerView, videos: List<Video>) {
        friendsRecyclerView.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        val adapter = VideosAdapter()
        adapter.setData(videos)

        friendsRecyclerView.adapter = adapter
    }

    private fun createOnClickListener(userId: Int) = View.OnClickListener {
        VK.urlResolver.open(it.context, "https://vk.com/id$userId")
    }

    inner class VideosAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val videos: MutableList<Video> = arrayListOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VideoHolder(parent.context)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as VideoHolder).bind(videos[position])
        }

        fun setData(videos: List<Video>) {
            this.videos.clear()
            this.videos.addAll(videos)
            notifyDataSetChanged()
        }

        override fun getItemCount() = videos.size
    }

    inner class VideoHolder(context: Context?) : RecyclerView.ViewHolder(
        LayoutInflater.from(context).inflate(R.layout.item_loaded_video_info, null)
    ) {
        private val videoIV: ImageView = itemView.findViewById(R.id.loadedVideoImageView)
        private val nameTV: TextView = itemView.findViewById(R.id.loadedVideoNameTextView)

        fun bind(video: Video) {
            nameTV.text = "TEST_VIDEO_NAME"
//            if (!TextUtils.isEmpty(video.photo)) {
//                Picasso.get().load(video.photo).error(R.drawable.ic_home_black_24dp).into(videoIV)
//            } else {
//                videoIV.setImageResource(R.drawable.ic_home_black_24dp)
//            }
            videoIV.setImageResource(R.drawable.ic_home_black_24dp)
        }
    }

}