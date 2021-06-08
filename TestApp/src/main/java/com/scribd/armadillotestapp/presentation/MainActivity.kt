package com.scribd.armadillotestapp.presentation

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.scribd.armadillo.Constants
import com.scribd.armadillotestapp.R
import com.scribd.armadillotestapp.data.Content
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = "MainActivity"
        const val AUDIOBOOK_EXTRA = "audiobook_extra"
    }

    private val armadilloVersion: TextView by lazy { findViewById(R.id.armadilloVersion) }
    private val recyclerView: RecyclerView by lazy { findViewById(R.id.recyclerView) }

    private val compositeDisposable = CompositeDisposable()

    @Inject
    lateinit var content: Content

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (application as AudioPlayerApplication).mainComponent.inject(this)

        armadilloVersion.text = Constants.LIBRARY_VERSION
        recyclerView.apply {
            val linearLayoutManager = LinearLayoutManager(this.context)
            layoutManager = linearLayoutManager
            adapter = ContentAdapter(application as AudioPlayerApplication, content.playables)
            addItemDecoration(DividerItemDecoration(this.context, linearLayoutManager.orientation))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}
