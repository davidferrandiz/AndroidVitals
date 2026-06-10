# Vitals — Android System Monitor

## Descripción
App de monitorización del sistema (CPU, RAM, threads) con lectura en tiempo real vía Foreground Service, recogida periódica vía WorkManager y comandos remotos vía FCM. Los datos se persisten en Room y se publican a un backend propio mediante Retrofit.

## Stack técnico

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| DI | Hilt 2.x (KSP) |
| Base de datos | Room 2.7.x (KSP) |
| Background | ForegroundService + WorkManager |
| Red | Retrofit + OkHttp + Moshi |
| Push | Firebase Cloud Messaging |
| Observabilidad | Firebase Crashlytics + Analytics |
| Build | AGP 9.x · Kotlin 2.2.x · Gradle Version Catalog |

## Estructura de módulos

```
:app              → Entry point: VitalsApplication, MainActivity, NavHost, AppModule
:core             → Data (Room, Retrofit), Domain (models, repo interfaces, UseCases), Notifications, DI
:feature:monitor  → VitalsForegroundService, VitalsWorker, VitalsFcmService, MonitorScreen + ViewModel
```

**Regla de dependencias (estricta):**
```
:app  ──►  :feature:monitor  ──►  :core
:app  ──────────────────────────►  :core
```
- `:feature:monitor` nunca importa de `:app`
- `:core` no depende de ningún módulo interno
- Añadir nuevas features siempre como `:feature:<nombre>`, nunca en `:app`

## Arquitectura

- **Clean Architecture**: el dominio (`:core/domain`) es la fuente de verdad; la capa de datos implementa sus interfaces
- **MVVM**: ViewModel expone `StateFlow`; Compose observa con `collectAsState()`
- **Single-Activity**: `MainActivity` es el host; cada feature expone un `@Composable` de entrada
- **Hilt entry points**: `@AndroidEntryPoint` en Activity y Service; `@HiltViewModel` en ViewModels; `@HiltWorker` en Workers

## Convenciones de código

- **Siempre KSP, nunca kapt** — Room y Hilt lo soportan completamente
- **Hilt scopes**: `SingletonComponent` para app-scoped, `ViewModelComponent` para VM-scoped
- **WorkManager**: inicialización manual vía `Configuration.Provider` en `VitalsApplication`; usar `@HiltWorker` + `@AssistedInject`
- **Room**: `exportSchema = false` durante desarrollo; activar y hacer commit del esquema antes de release
- **Corrutinas**: colección de Flow siempre dentro de `viewModelScope` o un dispatcher de background — nunca en Main
- **Notificaciones**: `NotificationHelper` es el único lugar donde se construyen notificaciones y se declaran IDs de canal
- **`google-services.json`**: solo en `app/` — nunca en submódulos, nunca en git

## Ubicaciones clave

| Artefacto | Ruta |
|---|---|
| Application class | `app/.../VitalsApplication.kt` |
| Hilt app module | `app/.../di/AppModule.kt` |
| Room DB + DAOs | `core/.../data/db/` |
| Contratos de dominio | `core/.../domain/` |
| Hilt modules (core) | `core/.../di/` |
| Foreground Service | `feature/monitor/.../service/VitalsForegroundService.kt` |
| WorkManager Worker | `feature/monitor/.../worker/VitalsWorker.kt` |
| FCM Service | `feature/monitor/.../fcm/VitalsFcmService.kt` |
| UI principal | `feature/monitor/.../ui/MonitorScreen.kt` |

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
