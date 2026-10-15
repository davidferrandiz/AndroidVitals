# Vitals — Android System Monitor

## Descripción
Monitor del sistema para Android (batería, CPU, temperatura, memoria, red y hardware). Todas las
lecturas se hacen en el dispositivo. Con la pantalla visible se lee en vivo; con la app cerrada solo
corre `VitalsWorker` cada ~15 min (historial de batería y avisos, ambos desactivables) y el widget,
sin servicio en primer plano. El historial nunca sale del móvil. Firebase Analytics y Crashlytics se
usan para estadísticas anónimas y fallos, y el usuario puede desactivarlos en Ajustes › Privacidad.

Usuarios reales (Analytics, oct 2026): sobre todo móviles de gama baja con MediaTek/Unisoc
(Moto E14, Redmi 12C, Galaxy A8 con Android 9) en India, Alemania, Irán, Canadá, Argentina, EE. UU.
e Italia. Diseñar para 360 dp, texto grande, RTL (persa) y APIs que faltan en Android antiguo.

## Stack técnico

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3, Navigation Compose |
| DI | Hilt 2.x (KSP) |
| Persistencia | DataStore (ajustes, estado de avisos) · Room 2.7.x (`battery_samples`, 30 días) |
| Background | WorkManager (solo `VitalsWorker`: historial, avisos y refresco del widget) |
| Widget | Glance 1.1 (`:feature:widget`) |
| Observabilidad | Firebase Crashlytics + Analytics (detrás de `AnalyticsTracker`) |
| Build | AGP 9.x · Kotlin 2.x · Gradle Version Catalog · R8 en release |

## Estructura de módulos

```
:app               → VitalsApplication, MainActivity, navegación (VitalsApp), Firebase (AnalyticsTracker)
:core              → Dominio (modelos, parsers puros, use cases, interfaces), datos (DataStore, Room, /proc y /sys), DI
:designsystem      → Tema (claro/oscuro/dinámico, VitalsPalette), iconos vectoriales, componentes, formateadores
:feature:monitor   → Panel (MonitorScreen + ViewModel), VitalsWorker + VitalsWorkScheduler
:feature:battery   → Pantalla Batería (en vivo, historial 24 h, salud estimada)
:feature:hardware  → Pantalla Hardware
:feature:settings  → Pantalla Ajustes
:feature:widget    → Widget Glance (implementa WidgetUpdater de :core)
```

**Regla de dependencias (estricta):**
```
:app  ──►  :feature:*  ──►  :designsystem  ──►  :core
:app  ──►  :designsystem, :core          :feature:*  ──►  :core
```
- `:feature:*` nunca importa de `:app` ni de otra feature
- `:core` no depende de ningún módulo interno
- `:designsystem` solo depende de `:core` (enums de ajustes para los formateadores)
- Añadir nuevas features siempre como `:feature:<nombre>`, nunca en `:app`

## Arquitectura

- **Clean Architecture**: el dominio (`:core/domain`) es la fuente de verdad; la capa de datos implementa sus interfaces
- **MVVM**: ViewModel expone `StateFlow` con `stateIn(WhileSubscribed(5_000))`; Compose observa con `collectAsStateWithLifecycle()` (nunca `collectAsState()`: seguiría leyendo /proc en segundo plano)
- **Single-Activity**: `MainActivity` es el host; cada feature expone un `@Composable` de entrada
- **Hilt entry points**: `@AndroidEntryPoint` en Activity y Service; `@HiltViewModel` en ViewModels; `@HiltWorker` en Workers
- **Analítica**: las features inyectan `AnalyticsTracker` (interfaz en `:core`); la implementación Firebase vive en `:app`. Nunca enviar valores de métricas del usuario

## Convenciones de código

- **Siempre KSP, nunca kapt** — Room y Hilt lo soportan completamente
- **Hilt scopes**: `SingletonComponent` para app-scoped, `ViewModelComponent` para VM-scoped
- **WorkManager**: inicialización manual vía `Configuration.Provider` en `VitalsApplication`; usar `@HiltWorker` + `@AssistedInject`
- **Room**: `exportSchema = true` desde la v2; el esquema se genera en `core/schemas` y va en git. Cada cambio de esquema sube la versión y lleva una `Migration` con su test
- **Segundo plano**: todo pasa por `VitalsWorker`; `VitalsWorkScheduler.needsWorker()` decide si existe según los ajustes. Nada de servicios ni alarmas exactas
- **Entre features**: si una feature necesita algo de otra (el worker refresca el widget), la interfaz va en `:core` y la implementación se enlaza con Hilt en la feature que la tiene
- **Corrutinas**: colección de Flow siempre dentro de `viewModelScope` o un dispatcher de background — nunca en Main
- **Lecturas del sistema**: /proc y /sys pueden estar bloqueados por SELinux según fabricante. Leer con `SysFs` (devuelve null), parsear con funciones puras de `ProcParsers`/`CpuMath`/`BatteryNormalizer` y testearlas con ficheros de ejemplo
- **Dato no disponible**: nunca una sección vacía ni "No data": usar `UnavailableRow` con el motivo y reportar con `MetricUnavailableReporter`
- **Nombres de hardware**: códigos de SoC y núcleos en `HardwareNames`; ampliar la tabla con lo que salga en `metric_unavailable`
- **UI**: colores de categoría con `VitalsTheme.palette`, iconos de `:designsystem` (nada de emojis), números con cifras tabulares, áreas táctiles de 44–48 dp
- **Textos**: inglés en `values/`, español en `values-es/` y los otros 18 idiomas en `values-<loc>/`. Lint falla si falta una cadena: toda cadena nueva se traduce en el mismo cambio
- **Notificaciones**: `NotificationHelper` es el único lugar donde se construyen notificaciones y se declaran IDs de canal
- **`google-services.json`**: solo en `app/` — nunca en submódulos, nunca en git

## Ubicaciones clave

| Artefacto | Ruta |
|---|---|
| Application class | `app/.../VitalsApplication.kt` |
| Navegación | `app/.../navigation/VitalsApp.kt` |
| Analítica (Firebase) | `app/.../analytics/FirebaseAnalyticsTracker.kt` |
| Contratos de dominio | `core/.../domain/` |
| Parsers puros + tests | `core/.../domain/parser`, `cpu`, `battery`, `hardware` · `core/src/test` |
| Use cases | `core/.../domain/usecase/` |
| Ajustes (DataStore) | `core/.../data/repository/SettingsRepositoryImpl.kt` |
| Tema y componentes | `designsystem/.../theme`, `component`, `format` |
| Panel | `feature/monitor/.../ui/MonitorScreen.kt` |
| Batería / Hardware / Ajustes | `feature/battery`, `feature/hardware`, `feature/settings` |
| Ficha y Data safety | `docs/play-store.md` · `docs/privacy-policy.html` |

## Notas de versiones

Las versiones en `libs.versions.toml` (KSP, Hilt, Room, Firebase BOM) son las recomendadas en el momento de la creación del proyecto. Verificar compatibilidad en el repositorio oficial si el build falla al resolver dependencias.
KSP debe alinearse con la versión exacta de Kotlin: `ksp = "{kotlin_version}-{ksp_patch}"`.

---

## vitals-tracker.html — instrucciones de actualización

El fichero `vitals-tracker.html` en la raíz del proyecto es el **tracker visual del proyecto**. Tanto Claude Code como Claude (Cowork) deben actualizarlo cuando haya cambios significativos, con criterio propio — no hace falta que el usuario lo pida.

### Cuándo actualizar
- Crear o modificar un módulo
- Añadir o cambiar una dependencia
- Implementar una feature (Service, Worker, DAO, ViewModel, Screen...)
- Cambiar configuración de Gradle o Manifest
- Resolver un error de build importante
- Completar un paso de CI/CD

No actualizar por cambios triviales (renombrar variable, ajuste de estilo, comentario).

### Cómo actualizar el HTML

**Estado de un item** — cambiar clases CSS:
- Punto: `dot-pending` → `dot-progress` → `dot-done`
- Badge: `ib-pending` → `ib-progress` → `ib-done`
- Texto completado: añadir clase `done` al `.item-text`

**Barra de progreso de módulo** — ajustar `width` del `.module-bar`:
- 0% pendiente · ~30-70% en progreso · 100% completo

**Historial** — añadir un `<div class="timeline-item">` nuevo al principio de la sección historial:
```html
<div class="timeline-item">
  <div class="tl-dot"></div>
  <div>
    <div class="tl-text">Qué se hizo y por qué (1-2 frases, tono técnico).</div>
    <div class="tl-date">Junio 2026</div>
  </div>
</div>
```

**Learning path** — actualizar estado del módulo activo si se completó.

### Tono del historial
Breve y técnico. Qué + por qué. Ejemplo:
"Implementado VitalsForegroundService con canal de notificación propio. Requerido por Android 8+ para que el SO no mate el proceso en background."
