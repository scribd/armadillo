package com.scribd.armadillo.models

import android.os.Parcel
import android.os.Parcelable
import com.scribd.armadillo.Milliseconds
import com.scribd.armadillo.extensions.toPrint
import com.scribd.armadillo.time.milliseconds
import java.io.Serializable

data class AudioPlayable(val id: Int,
                         val title: String,
                         val request: MediaRequest,
                         val chapters: List<Chapter>) : Serializable {

    /**
     * Provider-specific metadata required to access their audio streams
     */
    class MediaRequest private constructor(
        /**
         * A provider URL
         */
        val url: String,
        /**
         * Additional provider-specific metadata required to access the URL (e.g. HTTP authentication headers)
         */
        val headers: Map<String, String> = emptyMap()
    ) : Serializable {
        companion object {
            // Just roughing in a factory pattern in case we need to generate more complex accessors in the future.
            fun createHttpUri(url: String, headers: Map<String, String> = emptyMap()) = MediaRequest(url, headers)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MediaRequest

            if (url != other.url) return false
            if (headers != other.headers) return false

            return true
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + headers.hashCode()
            return result
        }

        override fun toString() = toPrint()
    }

    override fun toString() = toPrint()

    val duration: Milliseconds
        get() {
            return if (chapters.isNotEmpty()) {
                chapters.last().endTime
            } else {
                0.milliseconds
            }
        }

    /**
     * Find the chapter being played at a given listening offset in a playlist.
     */
    fun getChapterAtOffset(listeningPosition: Milliseconds): Chapter? {
        if (chapters.isEmpty()) return null
        // chapters times / durations are rounded down so it is possible for the listening position to pass end of audioPlayable
        val audioPlayableFinished = chapters.last().startTime + chapters.last().duration <= listeningPosition
        if (audioPlayableFinished) {
            return chapters.last()
        }

        if (listeningPosition <= 0.milliseconds) {
            //potential rounding error from doubles could make position negative.
            return chapters.first()
        }

        return chapters.first {
            listeningPosition >= it.startTime
                && listeningPosition < it.startTime + it.duration
        }
    }

    fun getChapterIndexAtOffset(listeningPosition: Milliseconds): Int {
        val chapter = getChapterAtOffset(listeningPosition)
        return chapters.indexOf(chapter)
    }

    fun getNextChapter(chapter: Chapter): Chapter? = chapters.getOrNull(chapters.indexOf(chapter) + 1)

    fun getPreviousChapter(chapter: Chapter): Chapter? = chapters.getOrNull(chapters.indexOf(chapter) - 1)
}

data class Chapter(
    val title: String,
    val part: Int,
    val chapter: Int,
    val startTime: Milliseconds,
    val duration: Milliseconds) : Serializable, Parcelable {
    val endTime = startTime + duration

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readLong().milliseconds,
        parcel.readLong().milliseconds) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeInt(part)
        parcel.writeInt(chapter)
        parcel.writeLong(startTime.longValue)
        parcel.writeLong(duration.longValue)
    }

    override fun toString() = toPrint()

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Chapter> {
        override fun createFromParcel(parcel: Parcel): Chapter {
            return Chapter(parcel)
        }

        override fun newArray(size: Int): Array<Chapter?> {
            return arrayOfNulls(size)
        }
    }
}