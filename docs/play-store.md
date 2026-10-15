# Play Store — Data safety y ficha (versión del rediseño)

Este documento recoge lo que hay que cambiar a mano en Play Console. El código de la rama
`feature/redesign` y la política de privacidad (`docs/privacy-policy.html`) ya cuentan esto mismo.

## 1. Data safety (Play Console › Contenido de la app › Seguridad de los datos)

Hasta ahora la ficha decía "No data collected", pero la app incluye Firebase Analytics y Crashlytics.
Respuestas a marcar:

| Pregunta | Respuesta |
| --- | --- |
| ¿Tu app recoge o comparte datos de usuario? | **Sí** |
| ¿Los datos se cifran en tránsito? | **Sí** (Firebase usa HTTPS) |
| ¿Los usuarios pueden pedir que se eliminen? | **Sí**: pueden desactivar la recogida en Ajustes › Privacidad y escribir al correo de soporte |

Tipos de datos recogidos (ninguno compartido con terceros con fines propios, todos **opcionales**
porque el usuario los desactiva en Ajustes):

| Categoría | Tipo | Finalidad |
| --- | --- | --- |
| Información y rendimiento de la app | Registros de fallos | Analítica, diagnóstico |
| Información y rendimiento de la app | Diagnósticos | Analítica |
| Actividad en la app | Interacciones con la app | Analítica |
| Identificadores de dispositivo u otros | ID de instalación de Firebase | Analítica |

Sobre AD_ID: Firebase Analytics añade el permiso `com.google.android.gms.permission.AD_ID`.
Ya lo declaraste en Play Console (commit `a2d01aa`); mantén esa declaración coherente con lo de arriba.

## 2. Descripción corta (máx. 80 caracteres)

- ES: `Monitor de batería, CPU, temperatura y hardware. Ligero, sin anuncios.`
- EN: `Battery, CPU, temperature and hardware monitor. Lightweight, no ads.`

## 3. Descripción larga (ES)

> Vitals te enseña de un vistazo cómo está tu móvil, con datos reales y explicados.
>
> **Batería**: nivel, potencia de carga en vatios, corriente, voltaje, temperatura, tiempo hasta carga
> completa o autonomía estimada, ciclos de carga (Android 14+) y tipo de cargador. Historial de las
> últimas 24 h, descarga media por hora y **salud estimada** de la batería a partir de tus cargas.
>
> **Avisos y widget**: aviso si la batería se calienta demasiado o el almacenamiento está casi lleno, y
> un widget con batería, temperatura, RAM y espacio libre en la pantalla de inicio.
>
> **Rendimiento**: carga estimada de la CPU con gráfica en vivo, frecuencia de cada núcleo con su nombre
> (Cortex-A55, Cortex-X3…) y aviso si el sistema está limitando el rendimiento por temperatura.
>
> **Hardware**: nombre real del procesador (Helio G85, Snapdragon, Exynos, Tensor…), GPU y Vulkan,
> pantalla y HDR, nivel de Widevine para streaming en HD, sensores en vivo e informe para compartir.
>
> **Memoria y almacenamiento**: RAM, zRAM, espacio libre y avisos cuando queda poco.
>
> **Red**: velocidad de bajada y subida, banda Wi-Fi (2,4 / 5 / 6 GHz), estándar y calidad de señal.
>
> Sin anuncios. Ligera: con la app cerrada solo hace una lectura cada 15 minutos. Funciona sin
> conexión. Tus lecturas y tu historial nunca salen del móvil; las estadísticas anónimas de uso se
> pueden desactivar en Ajustes.

Quitar de la ficha actual: "Process Manager" y "no data collection" (ver el análisis del rediseño).

## 4. Traducciones de la ficha

Por usuarios activos: inglés, alemán, persa, hindi, francés (Canadá), italiano y español.
