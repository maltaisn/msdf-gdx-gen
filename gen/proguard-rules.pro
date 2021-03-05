# note: sorry if build is not easily reproduceable, Proguard is a major pain
# to make work correctly (outside of Android). It will need to find everything
# in the Java standard library. The rest can be ignored with -dontwarn.

# Java runtime library
# This works for me with Open JDK 11 on Ubuntu, but will probably fail for a variety
# of configurations (Oracle JDK, version != 11, different OS, etc)
-libraryjars <java.home>/jmods/(!**.jar;!module-info.class)

-dontobfuscate

# JCommander
-keep class com.beust.jcommander.** { *; }

# pngtastic
-dontwarn com.googlecode.pngtastic.ant.PngOptimizerTask

# libGDX
-dontwarn org.lwjgl.**
-dontwarn com.badlogic.gdx.**

# Kotlin coroutines
-dontwarn kotlinx.coroutines.flow.**
-dontwarn kotlinx.coroutines.debug.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Enums
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Entry points
-keep public class com.maltaisn.msdfgdx.gen.MainKt {
    public static void main(java.lang.String[]);
}
-keepclassmembers class com.maltaisn.msdfgdx.gen.Parameters { *; }
