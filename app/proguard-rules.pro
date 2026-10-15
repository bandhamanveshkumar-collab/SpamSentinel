# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Data models serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class com.spamsentinel.app.domain.model.** { *; }

# Glance
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
