# Huellitas sin Hogar - Backend

API REST para la adopción de perros y gatos en Tecámac, Estado de México. Los refugios y particulares publican mascotas, los adoptantes envían solicitudes (con una encuesta de compatibilidad opcional) y un administrador modera las cuentas.

Hecho con Spring Boot 4.1.1, Spring Security (JWT), Spring Data JPA y PostgreSQL en Supabase. Las fotos se guardan en Supabase Storage.

## Requisitos

- Java 21
- Maven 3.9 o superior (o el wrapper incluido: `mvnw` / `mvnw.cmd`)
- Un proyecto de Supabase con las tablas ya creadas (`ddl-auto=validate` no crea tablas)

## Variables de entorno

| Variable | Obligatoria | Para qué sirve |
|---|---|---|
| `DB_URL` | Sí | URL JDBC del **Session pooler** de Supabase, por ejemplo `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres` |
| `DB_USER` | Sí | Usuario de la base (`postgres.<ref-del-proyecto>`) |
| `DB_PASSWORD` | Sí | Contraseña de la base de datos |
| `JWT_SECRET` | Recomendada | Clave para firmar los tokens (32 caracteres o más). Si falta, se genera una al arrancar y las sesiones se pierden al reiniciar |
| `SUPABASE_SERVICE_KEY` | Para subir fotos | Clave secret de Supabase (`sb_secret_...`). Sin ella, la subida de fotos responde 503 |
| `CORS_ORIGINS` | En producción | Orígenes del frontend permitidos, separados por comas. Por defecto `http://localhost:4200` |
| `FRONTEND_URL` | En producción | URL del frontend para los enlaces de "restablecer contraseña". Por defecto `http://localhost:4200` |
| `ADMIN_CORREO` | Opcional | Correo del administrador inicial |
| `ADMIN_PASSWORD` | Opcional | Contraseña del administrador inicial (se crea solo si ambos tienen valor y aún no hay ninguno) |
| `PORT` | No | Puerto HTTP. Railway lo define solo; en local es 8080 |

`.env.example` tiene la lista con valores de ejemplo. Nunca subas valores reales al repositorio.

## Correr en local (PowerShell)

1. Copia el archivo de ejemplo y llena tus valores:

   ```powershell
   Copy-Item .env.example .env
   notepad .env
   ```

   La aplicación lee `.env` automáticamente al arrancar.

2. Compila y arranca:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   O, con Maven instalado: `mvn spring-boot:run`.

3. La API queda en `http://localhost:8080/api`.

También puedes definir las variables en la sesión en lugar de usar `.env`:

```powershell
$env:DB_URL = "jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres"
$env:DB_USER = "postgres.<ref-del-proyecto>"
$env:DB_PASSWORD = "<tu-contraseña>"
.\mvnw.cmd spring-boot:run
```

## Despliegue

El backend se despliega en **Railway**. Define las variables de la tabla anterior en el panel del servicio (no hace falta `PORT`). Usa la URL del Session pooler de Supabase, porque la conexión directa `db.<ref>.supabase.co` solo funciona por IPv6.
