# 默认ProGuard规则
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class com.ailiveoverflow.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class okhttp3.** { *; }
-keep class org.json.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**