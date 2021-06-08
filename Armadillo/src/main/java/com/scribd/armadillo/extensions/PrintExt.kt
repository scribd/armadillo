package com.scribd.armadillo.extensions

import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.Chapter


fun AudioPlayable.toPrint() : String = """
        AudioPlayable(
            $id,
            "$title",
            ${request.toPrint()},
            ${chapters.toPrint()}
        )
    """.trim()


fun AudioPlayable.MediaRequest.toPrint() : String {
    return if (headers.isNotEmpty()) {
        val headersMapForPrint = headers.keys.foldIndexed("") { i, result, key ->

            var map = """"$key" to "${headers[key]}""""
            if (i < headers.size - 1) {
                map += ",\n"
            }
            result + map
        }

        """AudioPlayable.MediaRequest.createHttpUri("$url", mapOf($headersMapForPrint))"""
    } else {
        """AudioPlayable.MediaRequest.createHttpUri("$url")"""
    }
}

fun Chapter.toPrint() : String = """Chapter("$title", $part, $chapter, ${startTime.longValue}.milliseconds, ${duration.longValue}.milliseconds)"""

fun List<Chapter>.toPrint(): String {
    val chaptersForPrint = foldIndexed("") {i, result, chapter->
        var printChapter = chapter.toPrint()
        if (i < size - 1) {
            printChapter += ",\n"
        }
        result + printChapter
    }
    return "listOf($chaptersForPrint)"
}