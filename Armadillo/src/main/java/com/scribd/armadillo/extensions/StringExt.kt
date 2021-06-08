package com.scribd.armadillo.extensions

import android.net.Uri

fun String.toUri(): Uri = Uri.parse(this)