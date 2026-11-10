# Add project specific ProGuard rules here.
# Keep domain models, Hilt generated classes, and gomobile io.nekohasekai.libbox JNI bridges intact.
-keep class com.nexora.x.domain.model.** { *; }
-keep class com.nexora.x.core.model.** { *; }
-keep class com.nexora.x.core.engine.** { *; }
-keep class io.nekohasekai.libbox.** { *; }
-keep class go.** { *; }
-dontwarn javax.annotation.**
-dontwarn io.nekohasekai.libbox.**
