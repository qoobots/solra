# Solra Android ProGuard 规则

# ── 保留 Kotlin 序列化 ──
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ── 保留 gRPC / Protobuf ──
-keep class com.solra.proto.** { *; }
-keep class com.solra.apis.** { *; }
-dontwarn com.solra.proto.**
-dontwarn com.solra.apis.**

# ── 保留 Firebase ──
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── 保留 Solra 内部类 ──
-keep class com.solra.app.** { *; }
-keep class com.solra.android.** { *; }

# ── JNI 桥接 ──
-keepclasseswithmembernames class * {
    native <methods>;
}

# ── Compose ──
-dontwarn androidx.compose.**
