# Consumer rules for TinaIDE's embedded RikkaHub library.
#
# Keep these library-safe: global app-level options such as -dontobfuscate,
# -dontshrink, or -dontoptimize are invalid in consumer ProGuard files.

-keepattributes SourceFile,LineNumberTable

# keep kotlinx serializable classes
-keep @kotlinx.serialization.Serializable class * {*;}

# keep jlatexmath
-keep class org.scilab.forge.jlatexmath.** {*;}

-dontwarn com.google.re2j.**

# Ktor on Android references JVM-only java.lang.management classes
# through IntellijIdeaDebugDetector. Android does not provide them.
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# java.beans is not available on Android; Jackson references it only on JVM.
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient

# auth0/jackson: TypeReference subclasses rely on runtime generic signatures.
# R8 strips Signature/InnerClasses/EnclosingMethod by default, and its class
# merging/inlining optimizations can destroy the anonymous class hierarchy that
# TypeReference.<init> depends on via getClass().getGenericSuperclass().
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class com.fasterxml.jackson.** { *; }
-keep class com.auth0.jwt.** { *; }
