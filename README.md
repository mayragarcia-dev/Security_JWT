# Demo de autenticación JWT

Backend Spring Boot para registro, inicio de sesión y autorización basada en roles. La API guarda usuarios en MySQL, cifra las contraseñas con BCrypt y valida tokens Bearer sin mantener sesiones de servidor.

## Requisitos y ejecución

- Java 21
- MySQL disponible localmente
- Maven Wrapper incluido

Cree la base de datos una vez:

```sql
CREATE DATABASE security_db;
```

Configure estas variables de entorno si su MySQL no usa los valores locales:

| Variable | Valor local predeterminado |
| --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/security_db` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | vacío |
| `JWT_SECRET` | clave local de demostración de 48 bytes |
| `JWT_EXPIRATION` | `3600000` milisegundos |

Reemplace `JWT_SECRET` con una clave aleatoria de al menos 32 bytes fuera de una demostración local. No use la clave predeterminada en un entorno compartido o desplegado.

En Windows, inicie la aplicación con:

```powershell
.\mvnw.cmd spring-boot:run
```

Hibernate crea o actualiza las tablas y la aplicación inserta los roles `USER`, `MODERATOR` y `ADMIN` si todavía no existen.

## Endpoints

| Método | Endpoint | Acceso | Resultado |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | Público | Registra una cuenta con rol `USER` |
| POST | `/api/auth/signin` | Público | Devuelve un JWT Bearer |
| GET | `/api/test/all` | Público | `Public Content` |
| GET | `/api/test/user` | `USER`, `MODERATOR` o `ADMIN` | `User Content` |
| GET | `/api/test/admin` | `ADMIN` | `Admin Content` |

Ejemplo de registro:

```json
{"username":"user1","email":"user@test.com","password":"12345678"}
```

El campo `roles` del ejemplo del laboratorio se acepta para compatibilidad, pero se ignora por seguridad; todas las altas públicas reciben únicamente `USER`.

Ejemplo de inicio de sesión:

```json
{"email":"user@test.com","password":"12345678"}
```

En las rutas privadas, envíe el encabezado `Authorization: Bearer <accessToken>`. El token dura una hora por defecto. Las credenciales incorrectas, la ausencia de token y un token inválido producen `401`; un usuario válido sin el rol requerido recibe `403`.

### Crear una cuenta ADMIN para la práctica

El registro público no acepta roles enviados por el cliente, para impedir que cualquier persona se otorgue privilegios. Registre una cuenta por `/api/auth/signup` y luego asígnele el rol desde MySQL:

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u CROSS JOIN roles r
WHERE u.email = 'admin@test.com' AND r.name = 'ADMIN';
```

Inicie sesión de nuevo con esa cuenta para obtener un token que incluya `ROLE_ADMIN`. El cambio de rol no modifica tokens que ya se emitieron; vuelva a iniciar sesión.

## Archivos principales

- `config/SecurityConfig.java`: reglas públicas, roles y manejo de `401`/`403`.
- `jwt/JwtAuthFilter.java`: lectura del encabezado Bearer y carga de autenticación.
- `jwt/JwtUtils.java`: creación y validación del JWT con JJWT.
- `controller/AuthController.java`: registro e inicio de sesión.
- `controller/TestController.java`: endpoints usados en el laboratorio.
- `INFORME_LABORATORIO_JWT.md`: respuestas desarrolladas a los casos de análisis, observaciones y conclusiones.
