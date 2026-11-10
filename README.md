# Álgebra Quest — Proyecto Android Studio (Compose Multiplatform) + Backend Ktor

Aplicación móvil educativa de Álgebra Universitaria con autenticación por roles (`ADMINISTRADOR`, `SUPERVISOR`, `ALUMNO`), sensor biométrico (**Huella dactilar** y **Reconocimiento facial** con `androidx.biometric:biometric` + Android Keystore AES-GCM) y servidor **Ktor (Netty :8080)**.

## 1. Cuentas de prueba por rol (`users.json`)

| Rol | Usuario | Contraseña | Acceso Biométrico |
| :--- | :--- | :--- | :--- |
| **ADMINISTRADOR** | `admin` | `Admin123!` | Huella dactilar y Reconocimiento facial |
| **SUPERVISOR** | `supervisor` | `Supervisor123!` | Huella dactilar y Reconocimiento facial |
| **ALUMNO** | `alumno` | `Alumno123!` | Huella dactilar y Reconocimiento facial |
| **ALUMNO** | `ana` | `Ana12345!` | Huella dactilar y Reconocimiento facial |
| **ALUMNO** | `luis` | `Luis12345!` | Huella dactilar y Reconocimiento facial |

## 2. Rutas de navegación implementadas (R-01 a R-12)

- **R-01 (`LoginScreen.kt` / `AlgebraQuestApp.kt`)**: Nueva interfaz principal de inicio de sesión con selector por rol (`Alumno`, `Supervisor`, `Administrador`), chips de autocompletado, validación de estado del servidor (`/health`) y panel de **Sensor Biométrico** con botones dedicados para **Huella dactilar** y **Reconocimiento facial**.
- **R-02 (`AdministradorScreen.kt`)**: Resumen de cuentas, estado de los 4 mundos, control/revocación de sesiones activas, reinicio de progreso de alumnos y configuración biométrica del administrador.
- **R-03 (`TeacherRankingScreen.kt` / `SupervisorScreen`)**: Ranking del aula filtrado por mundo, avance individual de alumnos por mundo y bitácora de respuestas en tiempo real.
- **R-04 (`HomeScreen.kt` / `AlumnoScreen`)**: Perfil activo del alumno, ejercicio interactivo de práctica (`2x + 3 = 11`), barra de progreso general, configuración biométrica y botón **Jugar**.
- **R-05 (`MenuScreen.kt` + `TopHudBar.kt`)**: Barra superior con vidas, XP y monedas, panel de misión recomendada, acompañante de aventura (`personaje2.jpg`) y accesos a Mundos, Tutoriales, Tienda, Logros, Ranking, Reto relámpago y Ajustes.
- **R-06 (`WorldsScreen.kt` + `WorldsCatalog.kt`)**: Selección de los 4 mundos encadenados (*Factorización*, *Trigonometría*, *Ecuaciones Diferenciales* y *Álgebra Lineal*).
- **R-07 (`LevelSelectScreen.kt` / `LevelIntroScreen.kt`)**: Selección de los 5 niveles de cada mundo, teoría, ejemplo resuelto y acceso al tutorial o partida.
- **R-08 (`GameScreen.kt` + `GameEngine.kt`)**: Motor de juego con preguntas de opción múltiple (`opcion`) y arrastrar/colocar (`arrastrar`), cronómetro en vivo, racha, pistas, desbloqueo de mundos y recuperación de vidas.
- **R-09 (`TutorialsListScreen.kt` / `TutorialScreen.kt`)**: Biblioteca de los 20 temas con buscador y filtros por mundo, reproductor guiado de 3 pasos, video de apoyo en YouTube y reto rápido de práctica.
- **R-10 (`StoreScreen.kt` / `AchievementsScreen.kt`)**: Tienda de recompensas (canje de 25 XP por vida extra y compra de los 4 trofeos: *Bronce*, *Plata*, *Oro* y *Campeón*) y vitrina de trofeos + 6 medallas de logros.
- **R-11 (`RankingScreen.kt` / `SettingsScreen.kt`)**: Tabla general de posiciones y pantalla de configuración (cambio de nombre visible, biometría huella/rostro, URL del servidor y reinicio de progreso).
- **R-12 (`AlgebraQuestApp.kt` / `AuthViewModel.kt` / `AppViewModel.kt`)**: Cierre de sesión que revoca el token en el backend Ktor y limpia la sesión local.

## 3. Rutas del Backend Ktor (A-01 a A-14)

- **A-01**: `GET /health` (`Application.kt`)
- **A-02**: `POST /auth/login` (`Application.kt`)
- **A-03**: `GET /auth/me` (`Application.kt`)
- **A-04**: `POST /auth/logout` (`Application.kt`)
- **Biometría**: `POST /auth/biometric/enroll`, `POST /auth/biometric/login`, `DELETE /auth/biometric` (`Application.kt`)
- **A-05**: `POST /api/jugadores/login` (`PlayerRoutes.kt`)
- **A-06**: `POST /api/jugadores/guardar` (`PlayerRoutes.kt`)
- **A-07**: `POST /api/jugadores/renombrar` (`PlayerRoutes.kt`)
- **A-08**: `POST /api/jugadores/reset` (`PlayerRoutes.kt`)
- **A-09**: `POST /api/juego/completar-nivel` (`GameProgressRoutes.kt`)
- **A-10**: `POST /api/juego/registrar-respuesta` (`GameProgressRoutes.kt`)
- **A-11**: `POST /api/juego/restaurar-vida` (`GameProgressRoutes.kt`)
- **A-12**: `POST /api/tienda/comprar-trofeo` (`GameProgressRoutes.kt`)
- **A-13**: `GET /api/ranking` (`RankingRoutes.kt`)
- **A-14**: `GET /api/ranking/aula` (`RankingRoutes.kt`)

## 4. Ejecución

1. **Iniciar el servidor Ktor (`backend`)**:
   ```bash
   cd backend
   ./gradlew run
   ```
   El servidor escucha en `http://0.0.0.0:8080` (desde el emulador de Android se conecta automáticamente a `http://10.0.2.2:8080`).

2. **Ejecutar la app en Android Studio**:
   Abre la carpeta raíz `AlgrebraQuest` en Android Studio y ejecuta la configuración `androidApp`.