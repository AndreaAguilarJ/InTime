# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Appwrite SDK
-keep class io.appwrite.** { *; }
-dontwarn io.appwrite.**

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.momentummm.app.**$$serializer { *; }
-keepclassmembers class com.momentummm.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.momentummm.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes
-keep @kotlinx.serialization.Serializable class * { *; }
-keep class com.momentummm.app.data.** { *; }
-keep class com.momentummm.app.data.entity.** { *; }
-keep class com.momentummm.app.data.appwrite.** { *; }

# Keep Password Protection classes
-keep class com.momentummm.app.data.entity.PasswordProtection { *; }
-keep class com.momentummm.app.data.dao.PasswordProtectionDao { *; }
-keep class com.momentummm.app.data.repository.PasswordProtectionRepository { *; }
-keep class com.momentummm.app.data.repository.PasswordProtectionSettings { *; }
-keep class com.momentummm.app.data.repository.ProtectedFeature { *; }
-keep class com.momentummm.app.ui.password.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Keep Parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Service classes
-keep class com.momentummm.app.service.** { *; }
-keep class com.momentummm.app.receiver.** { *; }

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep data binding classes
-keep class androidx.databinding.** { *; }

# SLF4J warnings - ignorar clases de implementación faltantes
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
-dontwarn org.slf4j.impl.StaticMarkerBinder

# Keep SLF4J API
-keep class org.slf4j.** { *; }
-keepclassmembers class org.slf4j.** { *; }

# Mantener anotaciones
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Retain generic signatures
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
