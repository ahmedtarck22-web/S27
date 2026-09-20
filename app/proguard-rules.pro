# Keep ARCore classes and prevent stack map / obfuscation issues with D8/R8
-keep class com.google.ar.core.** { *; }
-dontwarn com.google.ar.core.**
-dontwarn com.google.ar.core.dependencies.**

# Keep Filament native and JNI bindings
-keep class com.google.android.filament.** { *; }
-dontwarn com.google.android.filament.**
