# Project Armadillo Release Notes

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