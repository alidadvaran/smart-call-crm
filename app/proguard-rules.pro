# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class ir.smartcallcrm.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class ir.smartcallcrm.data.remote.dto.** { *; }

# --- Retrofit / OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions
-keep class retrofit2.** { *; }

# --- Room ---
-keep class androidx.room.** { *; }
-keep class ir.smartcallcrm.data.local.entity.** { *; }

# --- Coroutines ---
-dontwarn kotlinx.coroutines.**
