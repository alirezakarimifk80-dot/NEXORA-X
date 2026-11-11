# ==============================================================================
# NEXORA X — Production R8 / ProGuard Rules (Release APK & AAB)
# ==============================================================================

# 1. Keep official sing-box / gomobile JNI bridge classes & native callbacks intact
-keep class io.nekohasekai.libbox.** { *; }
-keep interface io.nekohasekai.libbox.** { *; }
-keep class go.** { *; }
-keep interface go.** { *; }
-dontwarn io.nekohasekai.libbox.**
-dontwarn go.**

# 2. Keep NEXORA X core engine, VpnService, and domain/data models
-keep class com.nexora.x.domain.model.** { *; }
-keep class com.nexora.x.core.model.** { *; }
-keep class com.nexora.x.core.engine.** { *; }
-keep class com.nexora.x.core.database.ProfileEntity { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# 3. Keep Room SQLite generated DAO implementations & schema metadata
-keep class * extends androidx.room.RoomOpenHelper
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# 4. Keep Hilt / Dagger dependency injection entry points
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn javax.annotation.**

# 5. Preserve Kotlin Coroutines & SourceFile line numbers for crash diagnostics
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile
-dontwarn kotlinx.coroutines.**

