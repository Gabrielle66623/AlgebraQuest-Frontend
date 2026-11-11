# Mapa de arquitectura – AlgebraQuest (para defensa individual)

Rutas relativas a `C:\Users\Gabriel\AndroidStudioProjects\`. Proyectos separados: `AlgrebraQuest\` (frontend KMP) y `backend\` (Ktor Server).

## 1. JSON de usuarios (backend)
- Archivo: `backend\src\main\resources\users.json` (username, password, displayName, role).
- Roles: `ADMINISTRADOR`, `SUPERVISOR`, `ALUMNO`.
- Lo lee `backend\src\main\kotlin\com\algebraquest\backend\Application.kt` (`Application.module`, `resource("users.json")` + `decodeFromString<List<StoredUser>>`).
- Se valida y se usa en `Store.kt` (`GameStore.findUser`) y se expone en `POST /auth/login`, que responde `{ token, user:{username, displayName, role} }`.
- Mundos/preguntas: `backend\src\main\resources\worlds.json`.

## 2. Serializador `kotlinx.serialization`
- **Backend**: `backend\build.gradle.kts` (plugin `kotlin("plugin.serialization")`, `ktor-serialization-kotlinx-json`); `install(ContentNegotiation) { json(...) }` en `Application.kt`; modelos `@Serializable` en `Models.kt`.
- **Frontend**: `AlgrebraQuest\shared\build.gradle.kts` (`alias(libs.plugins.kotlinSerialization)`, `ktor.client.contentNegotiation`, `ktor.serialization.kotlinxJson`, `kotlinx.serialization.json`); versiones en `AlgrebraQuest\gradle\libs.versions.toml`; `install(ContentNegotiation) { json(...) }` en `ApiClient.kt`.

## 3. Petición HTTP (Ktor Client) y URL/puerto
- Cliente: `AlgrebraQuest\shared\src\commonMain\kotlin\com\example\algrebraquest\data\ApiClient.kt` (`ApiClient.login` → `POST /auth/login`).
- URL base por plataforma (puerto 8080, backend en `main()` de `Application.kt`):
  - Android (emulador): `shared\src\androidMain\...\platform\PlatformServices.android.kt` → `http://10.0.2.2:8080`
  - iOS: `shared\src\iosMain\...\platform\PlatformServices.ios.kt` → `http://localhost:8080`
  - Editable en la pantalla de login ("Configurar servidor Ktor") → `AppViewModel.setServerUrl`.
- `androidApp\src\main\AndroidManifest.xml`: permiso `INTERNET` y `usesCleartextTraffic="true"` (HTTP sin TLS).

## 4. DTOs que empatan frontend ↔ backend
| Frontend (`data\ApiModels.kt`) | Backend (`Models.kt`) |
|---|---|
| `LoginRequest(username, password)` | `LoginRequest` |
| `LoginResponse(token, user)` | respuesta de `openSession` |
| `UserDto(username, displayName, role)` | usuario de `users.json` (`StoredUser` sin password) |
| `ApiError(message)` | `ApiError` |
| `PlayerState`, `PlayerResponse`, `RankingEntry`, `AdminOverview`, … | equivalentes en `Models.kt` |

## 5. Ruteo por roles tras el login
- `ui\AppViewModel.kt` (~líneas 55-62): `Role.ADMINISTRADOR → Route.Administrator`, `Role.SUPERVISOR → Route.Supervisor`, `Role.ALUMNO → Route.Home`; se ejecuta en `login()` al recibir `LoginResponse`.
- `AlgebraQuestApp.kt`: `when (route)` dibuja `AdministradorScreen`, `SupervisorScreen` o `AlumnoScreen`.
- Rutas definidas en `domain\Route.kt`. El backend también restringe por rol con `requireAuth(store, ROLE_...)`.

## Capas del frontend (`AlgrebraQuest\shared\src\commonMain\kotlin\com\example\algrebraquest\`)
- `data\` ApiClient + DTOs · `domain\` GameEngine, WorldsCatalog, Route · `platform\` sensores biométricos (expect/actual) · `ui\` ViewModels (`StateFlow`), pantallas y tema (`ui\theme\AqTheme.kt`).
