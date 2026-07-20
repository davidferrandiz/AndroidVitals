# Vitals — Android System Monitor

App de monitorización del sistema (CPU, RAM, threads) con lectura en tiempo real vía
**Foreground Service**, recogida periódica vía **WorkManager** y comandos remotos vía **FCM**.
Los datos se persisten en **Room** y se publican a un backend propio mediante **Retrofit**.

## Stack técnico

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| DI | Hilt (KSP) |
| Base de datos | Room (KSP) |
| Background | ForegroundService + WorkManager |
| Red | Retrofit + OkHttp + Moshi |
| Push | Firebase Cloud Messaging |
| Observabilidad | Firebase Crashlytics + Analytics |
| Build | AGP 9.x · Kotlin 2.2.x · Gradle Version Catalog |

## Estructura de módulos

```
:app              → Entry point: VitalsApplication, MainActivity, NavHost, AppModule
:core             → Data (Room, Retrofit), Domain (models, repos, UseCases), Notifications, DI
:feature:monitor  → VitalsForegroundService, VitalsWorker, VitalsFcmService, MonitorScreen + ViewModel
```

Regla de dependencias: `:app → :feature:* → :core` · `:core` no depende de nadie.

## Compilar

```bash
./gradlew build       # compila
./gradlew test        # tests unitarios
./gradlew lint        # lint
```

---

## 📚 Documentación

### El proyecto
- **[CLAUDE.md](CLAUDE.md)** — guía de arquitectura, convenciones y ubicaciones clave del proyecto.
- **[CODING_STANDARDS.md](CODING_STANDARDS.md)** — reglas del equipo (DI, concurrencia, Room,
  seguridad…). Es la fuente de verdad del revisor de PRs.

### PR bot — revisor de código con IA local
Un revisor automático que corre 100% en local con Ollama, revisa según los estándares vía RAG,
se evalúa con golden tests y comenta cada PR desde GitHub Actions.

- **[pr-bot/README.md](pr-bot/README.md)** — la herramienta: requisitos, estructura, uso local
  y deploy en CI.
- **[Supertutorial: montar el bot de 0 a CI](.claude/SUPER_TUTORIAL_PR_BOT.md)** — la guía
  completa paso a paso (revisor → RAG → evaluación → CI).
- **[Referencia de las skills del PR bot](.claude/skills/README.md)** — qué hace cada skill,
  cuándo y cómo usarla, con ejemplos (versión técnica del tutorial de skills).

### Skills del proyecto (`.claude/skills/`)
Skills que montan y mejoran el PR bot. Cada una se invoca en lenguaje natural.

| Skill | Qué hace |
|---|---|
| [pr-bot-generator](.claude/skills/pr-bot-generator/SKILL.md) | Monta el bot de 0 a CI |
| [pr-bot-comentarios-en-linea](.claude/skills/pr-bot-comentarios-en-linea/SKILL.md) | Comentarios línea a línea + glosario de reglas |
| [pr-bot-mejorar](.claude/skills/pr-bot-mejorar/SKILL.md) | Orquestador: diagnostica y lanza las demás |
| [pr-bot-ampliar-evaluacion](.claude/skills/pr-bot-ampliar-evaluacion/SKILL.md) | Amplía el golden set |
| [pr-bot-afinar-prompt](.claude/skills/pr-bot-afinar-prompt/SKILL.md) | Afina el prompt midiendo (bucle) |
| [pr-bot-actualizar-estandares](.claude/skills/pr-bot-actualizar-estandares/SKILL.md) | Sincroniza CODING_STANDARDS con el linter |
| [pr-bot-rag-embeddings](.claude/skills/pr-bot-rag-embeddings/SKILL.md) | RAG por significado (embeddings) |
| [pr-bot-comparar-modelos](.claude/skills/pr-bot-comparar-modelos/SKILL.md) | Benchmark de LLMs en tu golden set |
| [pr-bot-seguridad-inyeccion](.claude/skills/pr-bot-seguridad-inyeccion/SKILL.md) | Red-team de prompt injection |
| [pr-bot-recoger-feedback](.claude/skills/pr-bot-recoger-feedback/SKILL.md) | accept/reject → dataset DPO/SFT |

> La IA es un **primer revisor, no la última palabra**: nunca auto-merge, el humano decide.
