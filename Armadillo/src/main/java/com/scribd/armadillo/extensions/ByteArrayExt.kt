package com.scribd.armadillo.extensions

import java.nio.ByteBuffer

internal fun ByteArray.decodeToInt(): Int = ByteBuffer.wrap(this).int