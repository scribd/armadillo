package com.scribd.armadillo

import com.scribd.armadillo.extensions.decodeToInt
import com.scribd.armadillo.extensions.encodeInByteArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ExtensionsTest {
    @Test
    fun encodeId_encodesProperly() {
        val expectedId = 356898828
        val encoded = expectedId.encodeInByteArray()
        val decodedId = encoded.decodeToInt()
        assertThat(expectedId).isEqualTo(decodedId)
    }
}