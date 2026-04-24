# Project-specific R8/ProGuard rules.
#
# Keep this file intentionally minimal until release-only issues surface.
# The default optimized Android rules are already applied from the Gradle config.

# Netty and Ktor-related ProGuard rules
-dontwarn io.netty.**
-dontwarn io.ktor.**
-dontwarn java.lang.management.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.eclipse.jetty.npn.**
-dontwarn reactor.blockhound.**

# Keep Ktor and Netty members that might be accessed via reflection
-keep class io.ktor.** { *; }
-keep class io.netty.** { *; }

# General optimizations
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable
