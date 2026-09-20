# 默认 ProGuard 规则占位：本项目 release 未开启混淆，规则保持最小化。
# 保留 kotlinx.serialization 生成的序列化器（若后续开启混淆）
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.xixi.notes.** {
    *** Companion;
}
-keepclasseswithmembers class com.xixi.notes.** {
    kotlinx.serialization.KSerializer serializer(...);
}
