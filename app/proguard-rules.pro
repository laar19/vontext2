# ==========================================
# ProGuard / R8 Rules for Vontext Production
# ==========================================

# Preserve Line Numbers and Attributes for Crash Reporting
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# --- VOSK OFFLINE SPEECH ENGINE ---
-keep class org.vosk.** { *; }
-dontwarn org.vosk.**
-keepclassmembers class org.vosk.** { *; }

# --- ML KIT TEXT RECOGNITION (Embedded On-Device) ---
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.android.gms.tasks.**
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.common.**
-keep class com.google.android.odml.** { *; }
-dontwarn com.google.android.odml.**

# --- OKHTTP3 & OKIO ---
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep interface okhttp3.** { *; }
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- ROOM DATABASE ---
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# --- RETROFIT & MOSHI / JSON SERIALIZATION ---
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# --- APPLICATION DATA MODELS & ENTITIES ---
-keep class dev.laar19.vontext.data.** { *; }
-keep class dev.laar19.vontext.processor.** { *; }

# --- COROUTINES ---
-keepclassmembernames class kotlinx.coroutines.internal.MainDispatcherFactory {
    java.lang.String createDispatcher(java.util.List);
}
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
