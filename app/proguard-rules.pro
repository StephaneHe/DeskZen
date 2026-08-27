# DeskZen ProGuard Rules

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Room entities
-keep class com.deskzen.data.local.entity.** { *; }

# Keep data classes used with Room
-keepclassmembers class com.deskzen.domain.model.** { *; }

# Strip verbose logging from release builds (defense in depth; the app
# uses Timber, which is only planted when BuildConfig.DEBUG is true)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
