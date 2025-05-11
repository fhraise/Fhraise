-keep class kotlinx.coroutines.swing.** { *; }
-keep class ch.qos.logback.classic.spi.LogbackServiceProvider { *; }
-keep class com.arkivanov.decompose.extensions.compose.** { *; }
-keep class io.ktor.client.engine.cio.CIOEngineContainer
-keep class xyz.xfqlittlefan.fhraise.routes.** { *; }
-keep @kotlinx.serialization.Serializable class **

-dontwarn android.**
-dontwarn com.jogamp.common.os.Platform
-dontwarn com.jogamp.opencl.**
-dontwarn com.jogamp.opengl.**
-dontwarn io.ktor.**
-dontwarn jakarta.**
-dontwarn javafx.scene.**
-dontwarn org.apache.maven.**
-dontwarn org.bytedeco.cpython.**
-dontwarn org.bytedeco.javacpp.**
-dontwarn org.bytedeco.numpy.**
-dontwarn org.codehaus.**
-dontwarn org.slf4j.**
-dontwarn org.osgi.annotation.**
-dontwarn org.osgi.framework.**

# JavaCV
-keep class org.bytedeco.** { *; }

#-keep @org.bytedeco.javacpp.annotation interface * {
#    *;
#}
#
#-keep @org.bytedeco.javacpp.annotation.Platform public class *
#
#-keepclasseswithmembernames class * {
#    @org.bytedeco.* <fields>;
#}
#
#-keepclasseswithmembernames class * {
#    @org.bytedeco.* <methods>;
#}
#
#-keep @interface org.bytedeco.javacpp.annotation.*
#-keep class org.bytedeco.javacpp.** {*;}
#-keepattributes *Annotation*, Exceptions, Signature, Deprecated, SourceFile, SourceDir, LineNumberTable, LocalVariableTable, LocalVariableTypeTable, Synthetic, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations, AnnotationDefault, InnerClasses
# JavaCV
#-keep @org.bytedeco.javacpp.annotation interface * {
#    *;
#}
#
#-keep @org.bytedeco.javacpp.annotation.Platform public class *
#
#-keepclasseswithmembernames class * {
#    @org.bytedeco.* <fields>;
#}
#
#-keepclasseswithmembernames class * {
#    @org.bytedeco.* <methods>;
#}
#
#-keepattributes EnclosingMethod
#-keep @interface org.bytedeco.javacpp.annotation.*,javax.inject.*
#
#-keepattributes *Annotation*, Exceptions, Signature, Deprecated, SourceFile, SourceDir, LineNumberTable, LocalVariableTable, LocalVariableTypeTable, Synthetic, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations, AnnotationDefault, InnerClasses
#-keep class org.bytedeco.javacpp.** {*;}
#-dontwarn java.awt.**
#-dontwarn org.bytedeco.javacv.**
#-dontwarn org.bytedeco.javacpp.**
#
## end javacv
