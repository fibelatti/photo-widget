# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-obfuscationdictionary proguard-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt
-classobfuscationdictionary proguard-dictionary.txt

# Debugging
-keepattributes LineNumberTable, SourceFile

# Common attributes
-keepattributes Signature, Exceptions, InnerClasses, EnclosingMethod, *Annotation*

# Kotlin
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

-dontnote kotlin.coroutines.jvm.internal.DebugMetadataKt**
-dontnote kotlin.internal.PlatformImplementationsKt
-dontnote kotlin.jvm.internal.Reflection
-dontnote kotlin.reflect.jvm.internal.KClassImpl**
-dontwarn kotlinx.atomicfu.AtomicBoolean

# Coroutines (remaining rules are shipped in coroutines consumer rules)
-dontwarn kotlinx.coroutines.flow.**inlined**

# Material
-dontnote com.google.android.material.**
-dontnote android.widget.**

# Compose
-assumevalues public class androidx.compose.runtime.ComposeRuntimeFlags {
    static boolean isLinkBufferComposerEnabled return true;
}
