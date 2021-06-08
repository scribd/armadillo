package com.scribd.armadillo.extensions

import java.nio.ByteBuffer

internal fun Int.encodeInByteArray(): ByteArray {
    // Creates a ByteBuffer instance with a 4 bytes capacity(32/8).
    // If you put more than one integer (or 4 bytes) in the ByteBuffer instance, a java.nio.BufferOverflowException is thrown.
    return ByteBuffer.allocate(Integer.SIZE / 8).apply {
        putInt(this@encodeInByteArray)
    }.array()
}