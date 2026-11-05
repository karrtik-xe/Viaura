# Only applied when the CloudStream switch is ON (app/build.gradle.kts: enableCloudstream).
# Extensions (.cs3) are loaded by class name at runtime, so the CloudStream library classes must not be renamed or removed.
-keep class com.lagradost.** { *; }
