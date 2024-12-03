# Project Armadillo Release Notes

## 1.7.0
- Fixes the playback progress being incorrect between a seek finishing and the next playback update. The progress position now updates 
  immediately to the seek target when the seek finishes

## 1.6.9
- Improves the crash fix from 1.6.8, so that even retries on EncryptedSharedPreferences does not crash.

## 1.6.8
- Fixes an app startup crash to EncryptedSharedPreference faults. 
- Adds resilience to playing unencrypted content if it is optionally drm enabled.

## 1.6.7
- Adds additional data in audio player errors: HttpResponseCodeException, DownloadFailed
- Add new ParsingException for internal ParserException
- Add internet connectivity check to error metrics

## 1.6.6
- Adds graceful exponential backoff toward internet connectivity errors.
- Retries streaming parsing exceptions, in case the network has not succeeded in retreiving valid data.

## 1.6.5
- ArmadilloPlayer handles client calls from any thread appropriately, without blocking. For those recently updating since 1.5.1, this 
  should resolve any strange bugs from client behavior.

## 1.6.4
- Resolves download issue resulting in downloaded storage often being out of alignment

## 1.6.3
- Prevents downloaded content with rotating URLs from being lost in the download system after the URL moves.
- Prevents devices that fail to initialize encrypted storage from crashing during initialization. 

## 1.6.2
- Prevents fatal crashing for actions are being performed before the player is initialized

## 1.6.1 
- Prevents fatal crashing when content fails to load

## 1.6.0
- Encrypts widevine drm keys. The Android minSDK has been changed to version 23 in order to support this feature.

## 1.5.4
- Ensured that ArmadilloPlayer.armadilloStateObservable has a state as soon as the player is initialized
- Fixed UnknownHostException being mapped to a HTTP status code issue rather than a Connectivity issue.

## 1.5.3
- Attempts to renew the widevine license of downloaded DRM content when playback begins, similarly to how streaming does it.
- Fixes ANR issue in the Reducer.

## 1.5.2 
- Fixes "Failed to Bind Service" issue introduced in 1.4, affecting MediaBrowser services.

## 1.5.1
- Adds DrmState to ArmadilloState, giving full visibility into DRM status to the client, including the expiration date for content.
- Splits SocketTimeout from HttpResponseCodeException, into ConnectivityException to better differentiate connectivity difficulties from 
  developer error.
- Ensures that ArmadilloState is updated from the main thread consistently.

## 1.5
- Fixes Error and Exception handling to not hide underlying exceptions, and to clearly explain the nature of errors.

## 1.4.1
- Fixes Download Service issues on Android 14. 
- Removes the Exoplayer 2.19.1 `core` module that does not support Android 14. This module must now be included separately by your 
  project for this version.

## 1.4.0
- Targets Android 14 (SDK 34), with appropriate service permissions.

## 1.3.3
- Reverted fix in 1.3.2 as it may have affected listening progress not being correctly reported
- Added support for passing in load control parameters via ArmadilloConfiguration to the exo player instance

## 1.3.2
- Added a fix for order of dispatched actions during seek related events

## 1.3.1
- Adds support for offline DRM for downloaded MPEG-DASH audio

## 1.3.0
- Adds support for DRM when playing MPEG-DASH audio

## 1.2.0
- Adds support for MPEG-DASH audio

## 1.1.1
- Added a fix for seek from notification player

## 1.1.0
- Target and Compile SDK updated to 33.

## 1.0.10
- Added foreground service type.

## 1.0.9
- Add support for updating playback headers during playback.

## 1.0.8
- Download removal is now handled by DownloadManager rather than DownloadService

## 1.0.7
- Added error state support for ForegroundServiceStartNotAllowedException

## 1.0.6
- Fixed issue with ArmadilloState events not being emitted on Android 12+

## 1.0.5
- Fixed issue with headers not properly being applied to Api Requests

## 1.0.4
- Upgraded Exoplayer version to 2.17.1
- Upgraded Java version to 11
- Upgraded Androidx Media version to 1.6.0
- Upgraded Kotlin version to 1.6.0
- Upgraded Gradle version to 7.4.2
- Upgraded Gradle plugin version to 7.2.0
- Added support for Android 12

## 1.0.3
- Upgrade exoplayer version to 2.15.1

## 1.0.2
- Upgrade exoplayer version to 2.15.0

## 1.0.1
- Updated mockito-kotlin version.
- Updated `MaxAgeCacheEvictor` to use lru behavior after evicting expired content.

## 1.0.0
* First Public Release!