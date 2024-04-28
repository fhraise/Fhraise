-keep class kotlinx.coroutines.swing.** { *; }
-keep class com.arkivanov.decompose.extensions.compose.** { *; }
-keep class io.ktor.client.engine.cio.CIOEngineContainer
-keep class xyz.xfqlittlefan.fhraise.routes.** { *; }
-keep @kotlinx.serialization.Serializable class **

-dontwarn jakarta.**
-dontwarn org.codehaus.**
-dontwarn org.slf4j.**
-dontwarn org.osgi.framework.**
