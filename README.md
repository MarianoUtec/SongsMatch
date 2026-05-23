# 🎵 MusicMatch — Backend de Recomendación Musical con SVD

**CS 2031 — Desarrollo Basado en Plataforma**

| Integrante | Código |
|---|---|
| [Nombre 1] | [Código 1] |
| [Nombre 2] | [Código 2] |
| [Nombre 3] | [Código 3] |
| [Nombre 4] | [Código 4] |

🔗 **Deployment:** `http://<tu-ip-ecs>:8080/api/v1`

---

## Índice

1. [Introducción](#introducción)
2. [Identificación del Problema](#identificación-del-problema)
3. [Descripción de la Solución](#descripción-de-la-solución)
4. [Modelo de Entidades](#modelo-de-entidades)
5. [Testing y Manejo de Errores](#testing-y-manejo-de-errores)
6. [Medidas de Seguridad](#medidas-de-seguridad)
7. [Eventos y Asincronía](#eventos-y-asincronía)
8. [GitHub & Management](#github--management)
9. [Conclusión](#conclusión)
10. [Apéndices](#apéndices)

---

## Introducción

### Contexto

El consumo de música digital ha crecido exponencialmente en los últimos años. Plataformas como Spotify o Apple Music manejan catálogos de millones de canciones y dependen de sistemas de recomendación sofisticados para personalizar la experiencia de cada usuario. Sin embargo, estos sistemas son cajas negras para los desarrolladores que los estudian. MusicMatch nace en el contexto del curso CS 2031 como una oportunidad para implementar desde cero un sistema de recomendación real, aplicando álgebra lineal (SVD) dentro de una arquitectura backend profesional en Spring Boot.

### Objetivos del Proyecto

- Implementar un backend REST completo con Spring Boot 3 y Java 21 siguiendo principios de arquitectura limpia.
- Aplicar Descomposición en Valores Singulares (SVD) como motor de recomendación musical, conectando matemática aplicada con ingeniería de software.
- Integrar la Spotify Web API para obtener canciones reales con metadatos de audio.
- Garantizar seguridad mediante JWT con refresh tokens, BCrypt y control de roles.
- Alcanzar cobertura de tests en repositorios, servicios y controladores usando JUnit 5, Mockito y TestContainers.

---

## Identificación del Problema

### Descripción del Problema

Los jóvenes universitarios descubren música nueva principalmente a través de algoritmos globales que priorizan canciones populares, sin considerar la compatibilidad real entre los gustos de usuarios individuales. No existe una plataforma que permita a un usuario encontrar a su "gemelo musical" — otra persona con preferencias latentes casi idénticas — y recibir recomendaciones basadas en lo que esa persona escucha, no en lo que escucha la mayoría.

### Justificación

Resolver este problema tiene valor académico y comercial. Académicamente, permite aplicar SVD — una técnica del álgebra lineal — en un contexto real de ciencia de datos. Comercialmente, un sistema de matching musical tiene potencial en nichos como eventos, comunidades de fans y plataformas sociales. Además, la arquitectura propuesta (un backend centralizado que expone un espacio latente 3D) es transferible a cualquier dominio de recomendación colaborativa.

---

## Descripción de la Solución

### Funcionalidades Implementadas

**1. Registro y autenticación JWT**
Los usuarios se registran con nombre, email y contraseña. El sistema valida email único y fortaleza de contraseña, codifica con BCrypt y devuelve un access token (24h) y refresh token (7d). Cualquier endpoint protegido requiere el header `Authorization: Bearer <token>`.

**2. Catálogo de canciones con integración Spotify**
El sistema pre-carga 10 canciones curadas. Adicionalmente, el endpoint `GET /api/v1/spotify/search?q=` consulta la Spotify Web API en tiempo real y persiste automáticamente las canciones encontradas con sus audio features (danceability, energy, valence, tempo).

**3. Sistema de calificaciones**
Los usuarios califican canciones del 1 al 5. Cada calificación activa de forma asíncrona el recálculo SVD completo. Si un usuario vuelve a calificar una canción ya puntuada, el score se actualiza.

**4. Motor de recomendación SVD**
Tras cada rating, el sistema construye la matriz A ∈ ℝ^(usuarios × canciones), aplica SVD (A = UΣVᵀ), reduce a k=3 dimensiones y actualiza el `LatentProfile` de cada usuario con sus coordenadas [x, y, z] en el espacio latente. La similitud coseno determina el usuario más cercano ("gemelo musical") y sus canciones mejor puntuadas no vistas por el usuario actual se guardan como `Recommendation`.

**5. Espacio latente 3D**
`GET /api/v1/users/latent-space` devuelve las coordenadas de todos los usuarios para que el frontend las visualice en 3D. Los usuarios más cercanos aparecen como puntos próximos en el gráfico.

**6. Recomendaciones personalizadas**
`GET /api/v1/recommendations/me` devuelve las 2 canciones recomendadas actuales junto con el nombre del gemelo musical y el porcentaje de compatibilidad.

**7. Panel de administración**
Los usuarios con rol `ADMIN` pueden listar todos los usuarios (paginados) y desactivar cuentas.

**8. Conversaciones y chat en tiempo real**
Se añadió soporte para crear conversaciones y enviar mensajes en tiempo real entre usuarios cercanos en el espacio latente. Cuando el sistema detecta un "gemelo musical" para un usuario, puede sugerir iniciar una conversación o crear automáticamente una `Conversation` entre ambos. El chat usa WebSockets (STOMP) para mensajes en tiempo real: `ChatController` expone endpoints REST y `WebSocketConfig` configura la comunicación en tiempo real. Las entidades `Conversation` y `Message` persisten historiales y permiten listar conversaciones y mensajes por usuario.

---

## Modelo de Entidades

```
┌──────────┐         ┌──────────┐         ┌──────────┐
│   User   │ 1     N │  Rating  │ N     1 │   Song   │
│──────────│─────────│──────────│─────────│──────────│
│ id       │         │ id       │         │ id       │
│ name     │         │ score    │         │ title    │
│ email    │         │ userId   │         │ artist   │
│ password │         │ songId   │         │ spotifyId│
│ role     │         │ createdAt│         │ coverUrl │
│ isActive │         │ updatedAt│         │ energy   │
└──────────┘         └──────────┘         │ valence  │
     │                                    └──────────┘
     │ 1                                       │ N
     │                                         │
     ▼ 1                                       ▼ N
┌──────────────┐                    ┌──────────────┐
│ LatentProfile│                    │    Genre     │
│──────────────│                    │──────────────│
│ coordX       │                    │ id           │
│ coordY       │                    │ name         │
│ coordZ       │                    └──────────────┘
│ closestUserId│
│ compatibility│
└──────────────┘
     │ 1
     ▼ N
┌──────────────┐         ┌──────────┐
│Recommendation│ N     N │   Song   │
│──────────────│─────────│          │
│ id           │         └──────────┘
│ basedOnUserId│
│ createdAt    │
└──────────────┘
     
     
    Additional entities for real-time chat:

┌──────────┐     N     N   ┌──────────────┐     1     N   ┌────────┐
│  User    │───────────────│ Conversation │───────────────│ Message│
│──────────│               │──────────────│               │────────│
│ id       │               │ id           │               │ id     │
│ name     │               │ title        │               │ text   │
│ ...      │               │ createdAt    │               │ sender │
└──────────┘               │ lastMessageAt│               │ sentAt │
                           └──────────────┘               └────────┘

(La relación `User` ⇄ `Conversation` es N:M — una conversación puede tener varios participantes y un usuario puede estar en varias conversaciones. `Message` pertenece a una `Conversation` y referencia al `sender`.)
```

### Descripción de Entidades

| Entidad | Descripción | Relaciones clave |
|---|---|---|
| `User` | Usuario registrado con credenciales y rol | 1:N con Rating, 1:1 con LatentProfile, 1:N con Recommendation, N:M con Conversation |
| `Song` | Canción con metadatos de Spotify y audio features | N:M con User vía Rating, N:M con Genre |
| `Rating` | Calificación 1–5 de un usuario a una canción (tabla pivote) | N:1 con User, N:1 con Song |
| `Artist` | Artista musical de una canción | 1:N con Song |
| `Genre` | Género musical | N:M con Song vía song_genres |
| `LatentProfile` | Coordenadas SVD [x,y,z] del usuario en espacio latente | 1:1 con User |
| `Recommendation` | Canciones sugeridas basadas en el gemelo musical | N:1 con User, N:M con Song |
| `Conversation` | Agrupación de participantes para chat en tiempo real; guarda metadatos (título, último mensaje) | N:M con User, 1:N con Message |
| `Message` | Mensaje enviado dentro de una `Conversation` (texto, remitente y timestamp) | N:1 con Conversation, N:1 con User (sender) |

---

## Testing y Manejo de Errores

### Niveles de Testing Realizados

**Tests de Repositorio (`@DataJpaTest` con H2)**
Se testean los tres repositorios principales: `UserRepository`, `SongRepository` y `RatingRepository`. Cada test cubre operaciones CRUD, queries personalizadas (`findByEmail`, `findSongsNotRatedByUser`, `findAllWithUserAndSong`) y edge cases como constraints de unicidad. Nomenclatura BDD: `shouldXxxWhenYyy`.

**Tests de Servicio (Mockito)**
Tests unitarios para `AuthService`, `RatingService` y `RecommendationService`. Las dependencias (repositorios, JwtService, PasswordEncoder, EventPublisher) son mockeadas con `@MockBean` y `@Mock`. Se prueban flujos felices, manejo de excepciones (`DuplicateResourceException`, `ResourceNotFoundException`, `UnauthorizedException`) y publicación de eventos.

**Tests de Controlador (`@WebMvcTest` con MockMvc)**
Tests de integración para los 6 controladores: `AuthController`, `RatingController`, `RecommendationController`, `SongController`, `UserController` y `ChatController`. Se verifican status codes HTTP, cuerpos de respuesta JSON, headers de autorización y comportamiento con requests inválidos. También existen pruebas que cubren el comportamiento de endpoints relacionados con conversaciones y mensajes.

**Tests de Integración (TestContainers + PostgreSQL real)**
Tres clases en el paquete `integration` usan `@Testcontainers` con un contenedor `postgres:15-alpine` real para verificar comportamiento real de la base de datos: constraints de unicidad a nivel DB, queries con JOINs complejos y ordenamiento.

### Resultados

Durante el desarrollo se detectaron y corrigieron los siguientes problemas mediante tests:
- La constraint `uk_user_song_rating` no se disparaba en H2 pero sí en PostgreSQL real — detectado por TestContainers.
- `findSongsNotRatedByUser` devolvía canciones ya calificadas cuando el usuario tenía ratings eliminados — corregido ajustando la query JPQL.
- `AuthService.login()` no lanzaba `UnauthorizedException` con el mensaje correcto — corregido tras el test de servicio.

### Manejo de Errores

El sistema centraliza todo el manejo de errores en `GlobalExceptionHandler` (`@RestControllerAdvice`). Todas las respuestas de error siguen el mismo formato:

```json
{
  "timestamp": "2026-05-20T15:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Song not found with id: 99",
  "path": "/api/v1/songs/99"
}
```

Las 8 excepciones personalizadas y sus status HTTP:

| Excepción | HTTP | Cuándo se lanza |
|---|---|---|
| `ResourceNotFoundException` | 404 | Entidad no encontrada por ID |
| `DuplicateResourceException` | 409 | Email ya registrado, spotify_id duplicado |
| `InvalidOperationException` | 400 | Operación inválida en el dominio |
| `UnauthorizedException` | 401 | Credenciales incorrectas, token inválido |
| `ForbiddenException` | 403 | Sin permisos para el recurso |
| `InsufficientRatingsException` | 422 | Pocos ratings para calcular SVD |
| `SpotifyApiException` | 502 | Fallo en llamada a Spotify API |
| `SvdComputationException` | 500 | Error en la factorización SVD |

---

## Medidas de Seguridad

### Seguridad de Datos

**Autenticación JWT stateless:** Los tokens se firman con HMAC-SHA256 usando una clave secreta almacenada en variables de entorno (`JWT_SECRET`). El `JwtAuthenticationFilter` extrae y valida el token en cada request antes de llegar al controlador. Los refresh tokens tienen TTL de 7 días y permiten renovar el access token sin re-login.

**BCrypt para contraseñas:** Las contraseñas nunca se almacenan en texto plano. `BCryptPasswordEncoder` aplica un salt aleatorio por usuario con factor de trabajo 10, haciendo ataques de fuerza bruta computacionalmente inviables.

**MapStruct sin fugas de datos:** Los mappers nunca exponen el campo `password` en ningún `UserResponse`. Los DTOs de respuesta están diseñados con el principio de mínimo privilegio.

**Variables de entorno:** Todos los secrets (JWT_SECRET, DB_PASSWORD, SPOTIFY_CLIENT_SECRET, MAIL_PASSWORD) se leen de variables de entorno. El `.gitignore` excluye `.env`. En producción se usan AWS Secrets Manager ARNs en la task definition de ECS.

### Prevención de Vulnerabilidades

**Inyección SQL:** Spring Data JPA con parámetros nombrados (`@Query("... WHERE u.id = :userId")`) previene inyección SQL. Nunca se construyen queries concatenando strings.

**CSRF:** Deshabilitado intencionalmente porque la API es stateless (JWT). CSRF solo es relevante para sesiones con cookies; con Bearer tokens no aplica.

**CORS:** Configurado explícitamente en `SecurityConfig` para aceptar solo los orígenes definidos en `CORS_ORIGINS`. En producción apunta únicamente al dominio del frontend.

**Autorización por capas:** `@PreAuthorize("hasRole('ADMIN')")` protege endpoints administrativos a nivel de método. El `SecurityContext` se consulta en los servicios para verificar que el usuario autenticado solo acceda a sus propios datos.

---

## Eventos y Asincronía

### Eventos Implementados

**`UserRegisteredEvent`**
Se publica en `AuthService.register()` inmediatamente después de persistir el nuevo usuario. El listener `MusicMatchEventListener.handleUserRegistered()` lo captura con `@EventListener` y dispara el envío del email de bienvenida de forma asíncrona. Desacoplamiento: `AuthService` no sabe nada de emails.

**`RatingSubmittedEvent`**
Se publica en `RatingService.rate()` después de guardar el rating. El listener usa `@TransactionalEventListener(phase = AFTER_COMMIT)` para garantizar que el SVD se recalcula solo si la transacción del rating fue exitosa. Si la transacción hace rollback, el SVD no se dispara innecesariamente.

**Eventos relacionados con conversación/chat**
Al crear una `Conversation` o enviar un `Message`, el sistema publica eventos internos que permiten notificar a componentes interesados (por ejemplo: métricas, historial o envío de notificaciones push). El envío de mensajes en tiempo real se realiza vía WebSocket y no bloquea la respuesta REST.

### Por qué deben ser asíncronos

**Correo electrónico:** El envío de un email puede tardar entre 200ms y 2 segundos dependiendo del servidor SMTP. Si fuera síncrono, el endpoint `POST /auth/register` tardaría ese tiempo en responder, degradando la experiencia. Con `@Async`, el registro responde en ~5ms y el email se envía en background.

**Recálculo SVD:** Construir la matriz de ratings, factorizarla y actualizar todos los `LatentProfile` puede tardar entre 50ms y 500ms según el número de usuarios. Si fuera síncrono en `POST /ratings`, el usuario esperaría ese tiempo por cada calificación. Con `@Async` y `@TransactionalEventListener(AFTER_COMMIT)`, el rating se guarda en ~2ms y el SVD se ejecuta en el `ThreadPoolTaskExecutor` configurado con 5 threads base y máximo 20.

**Mensajería en tiempo real:** El manejo de mensajes se realiza a través de WebSocket; la persistencia de `Message` se hace de forma rápida y, si se requieren tareas adicionales (notificaciones push, indexación), se delegan a procesos asíncronos.

---

## GitHub & Management

### Gestión de Tareas

El proyecto se gestionó usando **GitHub Projects** con tablero Kanban de 4 columnas: `Backlog`, `In Progress`, `In Review`, `Done`. Cada entidad o funcionalidad principal fue un issue etiquetado (`entity`, `service`, `controller`, `test`, `security`, `bug`). Los milestones correspondieron a las semanas del curso.

División por integrante:
- **Integrante 1:** Entidades, repositorios, DTOs, MapStruct
- **Integrante 2:** Seguridad JWT, AuthService, AuthController
- **Integrante 3:** SVD Algorithm, SvdComputationService, eventos
- **Integrante 4:** Tests (repositorios, servicios, controladores, TestContainers)

### Flujo de Git

```
main ← develop ← feature/entity-user
                ← feature/jwt-security
                ← feature/svd-algorithm
                ← feature/rating-service
                ← test/repository-tests
                ← test/controller-tests
```

Cada feature branch se mergea a `develop` vía Pull Request con al menos 1 aprobación. `main` solo recibe merges desde `develop` en hitos de entrega. Los commits siguen Conventional Commits: `feat:`, `fix:`, `test:`, `docs:`, `chore:`.

---

## Conclusión

### Logros del Proyecto

MusicMatch demuestra que es posible integrar matemática aplicada (SVD) dentro de una arquitectura Spring Boot profesional sin sacrificar la calidad del código. El sistema recomienda música real usando la Spotify API, protege todos los endpoints con JWT, maneja errores de forma consistente y está cubierto por tests en tres niveles incluyendo TestContainers contra PostgreSQL real. Además, se añadió soporte de conversaciones y mensajería en tiempo real para facilitar la interacción entre usuarios compatibles.

### Aprendizajes Clave

- La separación estricta entre capas (Controller → Service → Repository) facilita enormemente el testing unitario con Mockito.
- `@TransactionalEventListener(phase = AFTER_COMMIT)` es crítico cuando el listener depende de datos que deben estar comprometidos en la BD.
- SVD con Apache Commons Math es suficiente para datasets pequeños, pero en producción real requeriría estrategias incrementales (folding-in) para no recalcular toda la matriz por cada nuevo usuario.
- TestContainers reveló diferencias reales entre H2 y PostgreSQL (constraints, dialectos, comportamiento de índices) que los tests con H2 ocultaban.

### Trabajo Futuro

- **Folding-in de usuarios nuevos:** proyectar el vector del nuevo usuario al espacio latente con `user_vector · Vᵀ` sin recalcular el SVD completo.
- **Swagger/OpenAPI:** documentación interactiva de la API con `springdoc-openapi`.
- **CI/CD con GitHub Actions:** pipeline automático de build + test + push a ECR en cada merge a main.
- **Paginación en todos los endpoints de listado.**
- **Upload de foto de perfil a AWS S3.**
- **Mejoras en chat:** soporte para conversaciones grupales ricas, mensajes multimedia y notificaciones push.

---

## Apéndices

### Licencia

MIT License — ver `LICENSE` en la raíz del repositorio.

### Referencias

- Spring Boot Documentation 3.2: https://docs.spring.io/spring-boot/docs/3.2.5/reference/html/
- Apache Commons Math SVD: https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/linear/SingularValueDecomposition.html
- Spotify Web API Reference: https://developer.spotify.com/documentation/web-api
- jjwt Documentation: https://github.com/jwtk/jjwt
- TestContainers for Java: https://java.testcontainers.org/
- Koren, Y. (2009). Matrix Factorization Techniques for Recommender Systems. IEEE Computer.

---

## 🚀 Correr Localmente

```bash
git clone https://github.com/your-org/musicmatch.git
cd musicmatch
cp .env.example .env
# Edita .env con tus valores
docker compose up --build
# API disponible en http://localhost:8080/api/v1
```

## ⚙️ Variables de Entorno

| Variable | Descripción |
|---|---|
| `DB_USERNAME` | Usuario PostgreSQL |
| `DB_PASSWORD` | Contraseña PostgreSQL |
| `JWT_SECRET` | Clave HMAC-SHA256 (≥32 chars) |
| `JWT_EXPIRATION` | TTL access token en ms (default: 86400000) |
| `JWT_REFRESH_EXPIRATION` | TTL refresh token en ms (default: 604800000) |
| `SPOTIFY_CLIENT_ID` | Client ID de tu app en Spotify Developer |
| `SPOTIFY_CLIENT_SECRET` | Client Secret de tu app en Spotify Developer |
| `MAIL_HOST` | SMTP host (ej: smtp.resend.com) |
| `MAIL_PASSWORD` | API key de Resend o contraseña SMTP |
| `MAIL_FROM` | Dirección de remitente |
| `CORS_ORIGINS` | Orígenes permitidos separados por coma |
