# Proguard rules for SGA Docente
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class ec.edu.uteq.sga.docente.data.remote.dto.** { *; }
-keep class ec.edu.uteq.sga.docente.data.local.entity.** { *; }
-keep class ec.edu.uteq.sga.docente.domain.model.** { *; }
