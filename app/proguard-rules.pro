# Reglas de R8 propias de la app.

# La librería de valoraciones de Play (play-core-ktx) referencia esta anotación de Play Services, que no
# viaja en el AAR. Es solo una anotación: no hace falta en tiempo de ejecución.
# (R8 de AGP 9 falla el build con "Missing class" si falta; regla sugerida en missing_rules.txt.)
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite

# R8 (optimización completa de AGP 9) dejaba los registradores de Firebase sin constructor:
# "Could not instantiate ...Registrar" -> Crashlytics no se registraba y VitalsApplication.onCreate
# crasheaba al abrir la app (NPE "FirebaseCrashlytics component is not present").
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
-keep class * implements com.google.firebase.components.ComponentRegistrar
