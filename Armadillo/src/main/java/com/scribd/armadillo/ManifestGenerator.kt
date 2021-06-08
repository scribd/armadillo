package com.scribd.armadillo

import android.content.Context
import android.net.Uri
import com.scribd.armadillo.time.Interval
import com.scribd.armadillo.time.Second
import java.io.File

interface ManifestGenerator {
    fun manifestUriFor(id: Int): Uri
    fun createManifest(audiobook: ManifestAudiobook)
}

data class ManifestAudiobook(val id: Int,
                             val duration: Interval<Second>,
                             val chapters: List<ManifestChapter>)

data class ManifestChapter(val duration: Interval<Second>, val uri: String)

class HlsManifestGenerator(private val context: Context) : ManifestGenerator {
    private companion object {
        const val MASTER_FILE_NAME = "master.m3u"
        const val PLAYLIST_FILE_NAME = "masterplaylist.m3u"
    }

    override fun manifestUriFor(id: Int): Uri = Uri.fromFile(File(context.filesDir, masterExtFor(id)))

    override fun createManifest(audiobook: ManifestAudiobook) {
        val masterFile = buildMasterFile(audiobook.id)
        context.openFileOutput(masterExtFor(audiobook.id), Context.MODE_PRIVATE).use {
            it.write(masterFile.toByteArray())
        }

        val playlistFile = buildPlaylistFile(audiobook)
        context.openFileOutput(playlistExtFor(audiobook.id), Context.MODE_PRIVATE).use {
            it.write(playlistFile.toByteArray())
        }
    }

    private fun masterExtFor(id: Int) = id.toString() + MASTER_FILE_NAME

    private fun playlistExtFor(id: Int) = id.toString() + PLAYLIST_FILE_NAME

    private fun playlistUriFor(id: Int) = Uri.fromFile(File(context.filesDir, playlistExtFor(id)))

    private fun buildPlaylistFile(audiobook: ManifestAudiobook): String {
        var playlistStr =
                """
                #EXTM3U
                #EXT-X-VERSION:7
                #EXT-X-TARGETDURATION:${audiobook.duration.value}
                """.trimIndent() + "\n"

        audiobook.chapters.forEach {
            playlistStr += """
                #EXTINF:${it.duration.value},
                ${it.uri}
                """.trimIndent() + "\n"
        }

        playlistStr += """
            #EXT-X-ENDLIST
            """.trimIndent()

        return playlistStr
    }

    private fun buildMasterFile(id: Int): String =
            """
            #EXTM3U
            #EXT-X-VERSION:7

            #EXT-X-STREAM-INF:BANDWIDTH=235000
            ${playlistUriFor(id)}
            """.trimIndent()
}