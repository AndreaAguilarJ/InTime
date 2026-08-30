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

# -----------------------------------------------------------------------------
# Endurecimiento de producción: reflexión, serialización y componentes Android
# -----------------------------------------------------------------------------

# Room inspecciona anotaciones y genera implementaciones cuyos contratos no deben
# perder miembros. Las entidades ya se conservan arriba; faltaban DAOs, converters
# y la implementación generada de la base de datos.
-keep @androidx.room.Dao interface com.momentummm.app.** { *; }
-keepclassmembers class com.momentummm.app.** {
    @androidx.room.TypeConverter <methods>;
}
-keep class com.momentummm.app.data.AppDatabase_Impl { *; }

# kotlinx.serialization necesita anotaciones, clases internas y serializers
# generados. Se conservan globalmente porque también los SDK pueden generarlos.
-keepattributes RuntimeInvisibleAnnotations,AnnotationDefault,EnclosingMethod
-keep,includedescriptorclasses class **$$serializer { *; }

# MotivationalMessageGenerator obtiene este campo con getField("GEMINI_API_KEY").
# Sin esta regla R8 puede renombrarlo o eliminarlo aunque compile correctamente.
-keep class com.momentummm.app.BuildConfig {
    public static final java.lang.String GEMINI_API_KEY;
}

# AppValidator carga estas clases mediante Class.forName con nombres literales.
-keep class com.momentummm.app.MomentumApplication { *; }
-keep class com.momentummm.app.data.AppDatabase { *; }
-keep class com.momentummm.app.data.appwrite.AppwriteService { *; }

# Los widgets construyen intents con el nombre literal de MainActivity; Glance y
# AppWidgetManager instancian los receivers desde el manifiesto.
-keepnames class com.momentummm.app.MainActivity
-keep class com.momentummm.app.widget.** { *; }

# Los modelos internos de Gemini se serializan dentro del SDK. Appwrite ya se
# conserva arriba; Billing usa clases Parcelable/Binder que cruzan el límite IPC.
-keep,allowoptimization class com.google.ai.client.generativeai.** { *; }
-keep,allowoptimization class com.android.billingclient.api.** { *; }

# iText registra fábricas y proveedores por nombre en distintas configuraciones.
# Se permite optimización, pero no eliminación ni renombrado de esas clases.
-keep,allowoptimization class com.itextpdf.** { *; }

# ============================================================================
# iText7: clases referenciadas que NO EXISTEN en Android
# ----------------------------------------------------------------------------
# Sin este bloque, `:app:minifyReleaseWithR8` FALLA con "Missing classes
# detected while running R8" y no se puede generar ningun artefacto de release.
# Verificado: la compilacion de release fallaba antes de anadirlo.
#
# Por que es seguro silenciarlas y no un parche que oculta un problema:
#
#  1) java.awt.* y javax.imageio.* son Java de escritorio y no forman parte del
#     SDK de Android; nunca van a existir en el dispositivo. iText las referencia
#     solo en rutas de generacion de imagenes AWT y codigos de barras
#     (Barcode128.createAwtImage, AwtImageDataFactory, PdfImageXObject
#     .getBufferedImage) que esta app no invoca: aqui iText se usa para exportar
#     PDF, no para producir imagenes AWT. Si algun dia se llamara a esas rutas,
#     fallarian en tiempo de ejecucion en cualquier caso, con o sin esta regla.
#
#  2) com.fasterxml.jackson.* lo usa el modulo commons de iText en utilidades
#     JSON OPCIONALES (JsonUtil). Jackson no es dependencia de este proyecto, asi
#     que ese codigo es inalcanzable.
#
# La lista es exactamente la que genero R8 en
# app/build/outputs/mapping/release/missing_rules.txt: deliberadamente NO se usa
# un comodin del tipo -dontwarn java.** para no enmascarar futuras clases que si
# importen.
-dontwarn com.fasterxml.jackson.annotation.JsonInclude$Include
-dontwarn com.fasterxml.jackson.core.JsonGenerator$Feature
-dontwarn com.fasterxml.jackson.core.JsonProcessingException
-dontwarn com.fasterxml.jackson.core.PrettyPrinter
-dontwarn com.fasterxml.jackson.core.type.TypeReference
-dontwarn com.fasterxml.jackson.core.util.DefaultIndenter
-dontwarn com.fasterxml.jackson.core.util.DefaultPrettyPrinter$Indenter
-dontwarn com.fasterxml.jackson.core.util.DefaultPrettyPrinter
-dontwarn com.fasterxml.jackson.databind.DeserializationFeature
-dontwarn com.fasterxml.jackson.databind.JavaType
-dontwarn com.fasterxml.jackson.databind.JsonNode
-dontwarn com.fasterxml.jackson.databind.ObjectMapper
-dontwarn com.fasterxml.jackson.databind.ObjectWriter
-dontwarn com.fasterxml.jackson.databind.SerializationFeature
-dontwarn java.awt.Canvas
-dontwarn java.awt.Color
-dontwarn java.awt.Image
-dontwarn java.awt.image.BufferedImage
-dontwarn java.awt.image.ColorModel
-dontwarn java.awt.image.ImageProducer
-dontwarn java.awt.image.MemoryImageSource
-dontwarn java.awt.image.PixelGrabber
-dontwarn javax.imageio.ImageIO
