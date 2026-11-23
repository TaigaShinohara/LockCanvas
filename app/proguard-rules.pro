# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.lockcanvas.app.model.**$$serializer { *; }
-keepclassmembers class com.lockcanvas.app.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.lockcanvas.app.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
