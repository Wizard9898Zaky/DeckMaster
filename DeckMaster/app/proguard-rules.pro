# Keep Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep our app
-keep class com.deckmaster.** { *; }
-keepclassmembers class com.deckmaster.** { *; }

# Keep Kotlin metadata
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlin.Metadata { *; }

# Keep Activity
-keep public class * extends android.app.Activity
-keep public class * extends androidx.activity.ComponentActivity

# Keep ViewModel
-keep public class * extends androidx.lifecycle.ViewModel

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
