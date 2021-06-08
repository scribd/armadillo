package com.scribd.armadillo.extensions

import com.scribd.armadillo.StateStore

internal fun StateStore.Provider.getCurrentlyPlayingId() = currentState.playbackInfo?.audioPlayable?.id