package com.scribd.armadillotestapp.presentation

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.scribd.armadillo.ArmadilloPlayer
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillotestapp.R
import javax.inject.Inject

class ContentAdapter(application: AudioPlayerApplication, private val content: List<AudioPlayable>)
    : RecyclerView.Adapter<ContentAdapter.ViewHolder>() {

    @Inject
    internal lateinit var armadilloPlayer: ArmadilloPlayer

    init {
        application.mainComponent.inject(this)
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.title)
        val downloadButton: View = view.findViewById(R.id.download_button)
        val removeDownloadButton: View = view.findViewById(R.id.remove_download_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.playable_view_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playable = content[position]
        holder.textView.text = playable.title
        holder.view.setOnClickListener {
            launchAudioPlayer(it.context, playable)
        }

        holder.downloadButton.setOnClickListener {
            armadilloPlayer.beginDownload(playable)
        }

        holder.removeDownloadButton.setOnClickListener {
            armadilloPlayer.removeDownload(playable)
        }
    }

    override fun getItemCount() = content.size

    private fun launchAudioPlayer(context: Context, audiobook: AudioPlayable) {
        val intent = Intent(context, AudioPlayerActivity::class.java)
        intent.putExtra(MainActivity.AUDIOBOOK_EXTRA, audiobook)
        startActivity(context, intent, null)
    }
}