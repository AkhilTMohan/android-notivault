# ProGuard / R8 Rules for NotiVault
# Configured for safe shrinking, optimization, and obfuscation without runtime crashes.

# Keep general reflection attributes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# -----------------------------------------------------------------------------
# Google Mobile Ads (AdMob)
# -----------------------------------------------------------------------------
-keep public class com.google.android.gms.ads.** {
    public *;
}
-keep public class com.google.ads.** {
    public *;
}
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.ads.**

# Keep our internal Ad management and configuration
-keep class com.bc.notivault.ads.** { *; }

# -----------------------------------------------------------------------------
# Room Database & Models
# -----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}
-keep class com.bc.notivault.data.model.** { *; }
-keep class com.bc.notivault.data.db.** { *; }

# -----------------------------------------------------------------------------
# Android Components & Services
# -----------------------------------------------------------------------------
-keep public class com.bc.notivault.NotiVaultApplication { *; }
-keep public class com.bc.notivault.MainActivity { *; }
-keep public class com.bc.notivault.service.NotiVaultListenerService { *; }
-keep class com.bc.notivault.di.** { *; }

# -----------------------------------------------------------------------------
# Jetpack Compose
# -----------------------------------------------------------------------------
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# -----------------------------------------------------------------------------
# Coroutines & DataStore
# -----------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keep class com.bc.notivault.data.preferences.** { *; }

# -----------------------------------------------------------------------------
# Coil (Image Loading)
# -----------------------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**
