-keep class libv2ray.** { *; }
-keep class go.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-dontwarn libv2ray.**
