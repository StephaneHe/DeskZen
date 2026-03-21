# DeskZen ProGuard Rules

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Room entities
-keep class com.deskzen.data.local.entity.** { *; }

# Keep data classes used with Room
-keepclassmembers class com.deskzen.domain.model.** { *; }
