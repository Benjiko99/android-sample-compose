# R8 rules for the minified release build (see `release` in app/build.gradle.kts).
#
# Almost nothing belongs here. Every library this app depends on ships its own consumer rules
# inside its artifact, and R8 merges them automatically: Retrofit, OkHttp, kotlinx.serialization
# (which contributes R8-full-mode-specific rules of its own), coroutines, Hilt/Dagger, DataStore,
# Media3 and the whole of Compose. The merged result is written to
# build/outputs/mapping/release/configuration.txt — read that before adding anything below, since
# a hand-written -keep almost always duplicates a rule that is already there and only costs size.

# Retrace production crashes through mapping.txt.
#
# R8 discards both attributes unless asked, and AGP's proguard-android-optimize.txt preset does not
# ask, so without this a release stack trace arrives with no line numbers. Renaming the source file
# attribute keeps the original .kt names out of the shipped APK while leaving the trace retraceable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
