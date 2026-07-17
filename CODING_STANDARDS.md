# CODING_STANDARDS — Vitals (Android / Kotlin)

Reglas que el revisor de PRs debe **hacer cumplir**. Violar una de estas reglas es un
problema de prioridad **[alta]** aunque no provoque un crash: la arquitectura NO es "estilo".
Cada comentario debe **citar la regla** que se incumple (p. ej. "viola R-DEP-1").

---

## 1. Arquitectura de módulos (reglas duras)

- **R-DEP-1** — Grafo de dependencias estricto: `:app → :feature:* → :core`. Además
  `:app → :core` directo está permitido.
- **R-DEP-2** — `:feature:*` **nunca** importa de `:app`.
- **R-DEP-3** — `:core` **no depende** de ningún módulo interno (ni `:app` ni `:feature`).
- **R-DEP-4** — Toda feature nueva va como módulo `:feature:<nombre>`, **nunca** dentro de `:app`.
- **R-ARCH-1** — Clean Architecture: el dominio (`:core/domain`) es la fuente de verdad.
  La capa de datos **implementa** las interfaces del dominio, no al revés. El dominio no
  conoce Room, Retrofit ni tipos de Android framework.

## 2. DI con Hilt

- **R-DI-1** — La inyección se hace con Hilt. **Prohibido** instanciar dependencias a mano
  (`SomeRepo()`) dentro de clases de negocio; se inyectan por constructor.
- **R-DI-2** — Entry points correctos: `@AndroidEntryPoint` en Activity y Service,
  `@HiltViewModel` en ViewModels, `@HiltWorker` en Workers.
- **R-DI-3** — Scopes: `SingletonComponent` para app-scoped; `ViewModelComponent` para
  VM-scoped. No abusar de `@Singleton` para estado mutable.
- **R-DI-4** — **Prohibidos los singletons ad-hoc** (`object` con estado mutable, o
  `companion object` que expone instancia global). Eso es estado global oculto: usar DI.

## 3. Procesado de anotaciones y persistencia

- **R-KSP-1** — **Siempre KSP, nunca kapt** (Room y Hilt lo soportan del todo).
- **R-ROOM-1** — Room `exportSchema = false` solo en desarrollo; antes de release se activa
  y se commitea el esquema.
- **R-ROOM-2** — Nada de acceso a base de datos en el hilo principal: DAOs `suspend` o que
  devuelvan `Flow`.

## 4. Concurrencia (crítico en esta app: Service + Worker + FCM)

- **R-COR-1** — La colección de `Flow` va **siempre** en `viewModelScope` o en un dispatcher
  de background — **nunca en Main**.
- **R-COR-2** — Prohibido `GlobalScope`. Usar structured concurrency (scope con ciclo de
  vida claro) para que las corrutinas se cancelen.
- **R-COR-3** — Trabajo pesado (I/O, cálculo) fuera del hilo principal (`Dispatchers.IO`/
  `Default`). Nada de bloquear Main.

## 5. WorkManager y background

- **R-WM-1** — Inicialización manual de WorkManager vía `Configuration.Provider` en
  `VitalsApplication`; los Workers son `@HiltWorker` + `@AssistedInject`.

## 6. UI (Compose + MVVM)

- **R-MVVM-1** — El ViewModel expone `StateFlow`; Compose observa con `collectAsState()`.
  El estado de UI no se mantiene en el Composable a mano si pertenece al VM.
- **R-UI-1** — Single-Activity: `MainActivity` es el host; cada feature expone un
  `@Composable` de entrada.
- **R-CMP-1** — Sin side-effects dentro de la composición: usar `LaunchedEffect`,
  `remember`, `rememberCoroutineScope`. Nada de lanzar corrutinas o I/O en el cuerpo del Composable.

## 7. Notificaciones

- **R-NOTIF-1** — `NotificationHelper` es el **único** sitio donde se construyen
  notificaciones y se declaran IDs de canal. No crear canales/notificaciones sueltos.

## 8. Seguridad y secretos

- **R-SEC-1** — `google-services.json` solo en `app/`, **nunca** en submódulos, **nunca** en git.
- **R-SEC-2** — Cero secretos hardcodeados (API keys, tokens) en el código. Van en
  `local.properties`/variables de entorno/secrets, fuera de VCS.
- **R-SEC-3** — No loguear datos sensibles (tokens FCM, payloads de red con PII).

---

## Buenas prácticas Kotlin/Android (aportadas — el revisor también las vigila)

- **K-NULL-1** — Evitar `!!` (not-null assertion): es un NPE esperando. Usar `?.`, `?:`,
  `requireNotNull(x) { "mensaje" }` o manejo explícito del caso nulo.
- **K-NULL-2** — `lateinit` solo cuando la inicialización está garantizada antes del uso;
  nunca leerlo en un camino donde pueda no estar inicializado.
- **K-LEAK-1** — No guardar `Context`/`Activity`/`View` en campos long-lived ni en
  singletons: fuga de memoria. Para long-lived usar `applicationContext`.
- **K-NET-1** — Nada de tragar excepciones de red en silencio (`catch {}` vacío). Propagar
  o mapear a un estado de error observable.
- **K-ERR-1** — No `catch (e: Exception)` genérico que oculte fallos; capturar lo específico
  y registrar/propagar.
- **K-IMMUT-1** — Preferir `val` e inmutabilidad; `data class` para modelos de dominio.
- **K-TEST-1** — Lógica de dominio/repositorio testeable: sin dependencias de Android
  framework en `:core/domain`.

---

> Cómo lo usa el bot: `retrieve.py` inyecta este documento (y los archivos que definen los
> tipos del diff) en el prompt; `review_rag.py` obliga al modelo a marcar como **[alta]**
> cualquier violación citando la regla. El estándar es la fuente de la verdad del revisor.
