package com.scribd.armadillo.playback.error

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ParserException
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ArmadilloHttpErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy(DEFAULT_MIN_LOADABLE_RETRY_COUNT) {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        return if (loadErrorInfo.exception is UnknownHostException || loadErrorInfo.exception is SocketTimeoutException) {
            //retry every 10 seconds for potential loss of internet -keep buffering - internet may later succeed.
            if (loadErrorInfo.errorCount > 6) {
                C.TIME_UNSET //stop retrying after around 10 minutes
            } else {
                //exponential backoff based on a 10 second interval
                (1 shl (loadErrorInfo.errorCount - 1)) * 10 * 1000L
            }
        } else if (loadErrorInfo.exception is ParserException) {
            /*
              Exoplayer by default assumes ParserExceptions only occur because source content is malformed,
              so Exoplayer will never retry ParserExceptions.
              We care about content failing to checksum correctly over the internet, so we wish to retry these.
             */
            if (loadErrorInfo.errorCount > 3) {
                C.TIME_UNSET //stop retrying, the content is likely malformed
            } else {
                //This is exponential backoff based on a 1 second interval
                (1 shl (loadErrorInfo.errorCount - 1)) * 1000L
            }
        } else {
            super.getRetryDelayMsFor(loadErrorInfo)
        }
    }
}