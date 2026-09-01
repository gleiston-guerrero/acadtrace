# Proguard rules for SGA Representante
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class ec.edu.uteq.sga.representante.data.remote.dto.** { *; }
-keep class ec.edu.uteq.sga.representante.data.local.entity.** { *; }
-keep class ec.edu.uteq.sga.representante.domain.model.** { *; }

# AndroidX Security/Tink references compile-time Error Prone annotations only.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
