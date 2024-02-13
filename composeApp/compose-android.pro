-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
#-dontwarn org.slf4j.impl.StaticLoggerBinder

-keep class org.slf4j.** { *; }
-keepclassmembers class xyz.xfqlittlefan.fhraise.oauth.MicrosoftApplicationModule$module$1
