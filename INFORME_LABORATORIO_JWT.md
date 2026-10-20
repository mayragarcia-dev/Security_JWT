# Laboratorio de autenticación JWT y autorización por roles

## Caso 1 Ruta pública

Para `GET /api/test/all`, el servidor responde HTTP `200` con el texto `Public Content`. La respuesta está en `TestController.java`, dentro del método `allAccess()`. No requiere autenticación porque solo expone contenido de prueba y `SecurityConfig.java` incluye `/api/test/all` entre las rutas `permitAll()`. Si se quita esa regla, la ruta cae en `.anyRequest().authenticated()` y una petición sin JWT recibe `401`. `SecurityFilterChain` define y aplica las reglas de seguridad HTTP a cada solicitud.

## Caso 2 Acceso sin token

`GET /api/test/user` sin token responde HTTP `401` y el JSON `{"message":"Autenticacion requerida o token invalido"}`. `FilterChainProxy` despacha la cadena configurada; `JwtAuthFilter` comprueba el encabezado y no establece autenticación cuando no hay token. Después, la regla `hasAnyRole("USER", "MODERATOR", "ADMIN")` no permite el acceso anónimo. El método `userAccess()` no se ejecuta. Spring Security invoca el `AuthenticationEntryPoint` configurado porque falta una autenticación válida.

## Caso 3 Token inválido

Al modificar un JWT, la firma deja de coincidir y el parseo falla. `JwtUtils.isTokenValid()` invoca el parseo implementado en `parseClaims()`, en `JwtUtils.java`, usando JJWT 0.11.5. Según el defecto, JJWT puede lanzar `io.jsonwebtoken.security.SignatureException`, `MalformedJwtException` u otra subclase de `JwtException`; un token vencido produce `ExpiredJwtException`. `JwtAuthFilter` captura esas excepciones, limpia el `SecurityContext` y continúa como usuario anónimo. La ruta protegida responde entonces HTTP `401` con el mismo JSON que una solicitud sin token. Un token inexistente significa que no se envió credencial; uno inválido sí se envió, pero no se pudo verificar.

## Caso 4 Acceso con token válido

`GET /api/test/user` con un token válido de un usuario con alguno de los roles permitidos responde HTTP `200` y `User Content`; se ejecuta `TestController.userAccess()`. `JwtAuthFilter` extrae el Bearer, `JwtUtils` verifica firma y vencimiento y obtiene el email (subject), y `UserDetailsServiceImpl` carga el usuario y sus roles desde MySQL. El filtro crea un `UsernamePasswordAuthenticationToken` con sus autoridades y lo guarda en `SecurityContextHolder`. La cadena de autorización verifica los roles antes de invocar el controller.

Flujo: Postman → encabezado `Authorization: Bearer ...` → `JwtAuthFilter` → `JwtUtils` valida y extrae el subject → `UserDetailsServiceImpl` carga usuario y roles → `SecurityContextHolder` conserva la autenticación de la solicitud → autorización de `SecurityFilterChain` → controller. Si el JWT expiró, el parser produce `ExpiredJwtException`; se limpia el contexto y una ruta protegida devuelve `401`.

## Caso 5 Ruta ADMIN sin permisos

Un usuario con rol `USER` que intenta `GET /api/test/admin` recibe HTTP `403` y `{"message":"No tiene permisos para este recurso"}`. La regla está en `SecurityConfig.java`: `.requestMatchers("/api/test/admin").hasRole("ADMIN")`. `UserDetailsServiceImpl` lee el usuario y su conjunto de entidades `Role`; `UserDetailsImpl` los convierte a una colección de `GrantedAuthority`, por ejemplo `ROLE_USER`. Autenticación comprueba quién es el usuario; autorización determina qué puede hacer. La solicitud USER está autenticada, pero carece de `ROLE_ADMIN`; por eso Spring Security la deniega antes de ejecutar el método del controller.

## Caso 6 Ruta ADMIN con permisos

Un usuario autenticado cuya colección de autoridades contiene `ROLE_ADMIN` recibe HTTP `200` y `Admin Content`; se ejecuta `TestController.adminAccess()`. Antes se verificaron la firma y vigencia del JWT, la existencia de la cuenta, su autenticación y la autorización requerida. En `SecurityContextHolder` queda un `Authentication` con el principal del usuario y la autoridad `ROLE_ADMIN` durante la solicitud.

Flujo: inicio de sesión → `AuthenticationManager` verifica correo y contraseña contra `UserDetailsServiceImpl` → `JwtUtils` firma el token → el cliente lo envía como Bearer → `JwtAuthFilter` valida el token y vuelve a cargar las autoridades → `SecurityContextHolder` guarda `Authentication` → `SecurityFilterChain` comprueba `ROLE_ADMIN` → `adminAccess()` responde. `JwtUtils` genera el JWT.

## Observaciones

1. MySQL debe estar activo y la base `security_db` debe existir antes de iniciar la aplicación.
2. La aplicación inicializa los tres roles al arrancar; los usuarios se guardan con contraseñas BCrypt, no en texto plano.
3. Un token transporta una identidad firmada, pero no cifra sus claims; no deben incluirse secretos dentro de él.
4. Los cambios de rol no actualizan tokens existentes; el cliente debe iniciar sesión de nuevo para recibir las autoridades actuales.
5. El registro asigna siempre `USER`; la promoción a `ADMIN` queda restringida a una operación administrativa en la base de datos.

## Conclusiones

1. JWT permite autenticar solicitudes sin almacenar una sesión HTTP en el servidor.
2. La firma y la fecha de expiración permiten rechazar tokens alterados o vencidos antes de autorizar el acceso.
3. Spring Security diferencia autenticación de autorización: un usuario puede estar identificado y aun así no tener el rol requerido.
4. El `SecurityContextHolder` enlaza la autenticación verificada con las decisiones de autorización durante cada solicitud.
5. Aceptar roles de privilegio enviados desde un formulario público permitiría autoasignarse permisos; por eso el registro entrega `USER` y la asignación administrativa se hace por un canal controlado.
