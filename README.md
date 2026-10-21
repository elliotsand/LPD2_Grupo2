# Hotel Los Andes

Aplicación web para administrar clientes, habitaciones, reservas y reportes de un hotel.
Proyecto del curso **Lenguaje de Programación II (5484)** – Cibertec 2026-02 – Grupo 2.

**Tecnologías:** Java 17 · Spring Boot (MVC) · Thymeleaf · JPA/Hibernate · MySQL · JasperReports · GitHub Actions · Microsoft Azure.

## Cumplimiento de requisitos del curso

| Requisito | Implementación |
|---|---|
| Conexión con base de datos | MySQL con JPA/Hibernate (`BDHotel.sql`); H2 en memoria para las pruebas |
| Gestión de roles | `ADMIN` y `RECEPCIONISTA`; cada controlador valida el rol y el menú muestra solo lo permitido |
| Gestión de sesiones | Login con usuario en `HttpSession`, interceptor que protege todas las páginas, expiración a los 20 min, nueva sesión al ingresar (anti fijación), cierre de sesión y contraseñas cifradas con BCrypt |
| 3 mantenimientos | Clientes · Tipos de habitación · Habitaciones (con validaciones y control de duplicados) |
| 2 consultas | Disponibilidad de habitaciones por fechas y tipo · Reservas registradas con filtros |
| 2 reportes | JasperReports PDF: Reservas por período/estado · Habitaciones (+ proyección de ingresos) |
| 1 transacción | Registrar reserva: valida fechas y cruces, bloquea la habitación, guarda la reserva y cambia la habitación a `RESERVADA` en una sola transacción (rollback si algo falla). También cancelar y finalizar |
| Azure + GitHub Actions | `.github/workflows/azure-deploy.yml` |

### Reglas de la reserva
1. La entrada no puede ser anterior a hoy y la salida debe ser posterior a la entrada (máximo 30 noches).
2. La habitación no puede estar en `MANTENIMIENTO`.
3. No puede cruzarse con otra reserva `CONFIRMADA` de la misma habitación.
4. La fila de la habitación se bloquea durante la transacción para evitar dobles reservas simultáneas.
5. Al cancelar o finalizar, la habitación vuelve a `DISPONIBLE` si no tiene otra reserva vigente.

## Preparar la base de datos

1. Inicia MySQL y abre `BDHotel.sql` en MySQL Workbench.
2. Ejecuta el script completo. Crea las tablas que falten y agrega datos de ejemplo sin duplicar los existentes.
3. En Spring Tools Suite, configura la variable de entorno `DB_PASSWORD` (y si hace falta `DB_URL`, `DB_USER`) en la configuración de ejecución de `ProyectoFinalApplication`.

Usuarios de ejemplo incluidos en el SQL:

| Usuario | Contraseña inicial | Rol |
|---|---|---|
| `admin` | `123456` | Administrador |
| `recepcion` | `123456` | Recepcionista |

La contraseña se guarda cifrada con BCrypt la primera vez que el usuario inicia sesión. Son credenciales de demostración; cámbialas antes de usar el sistema con información real.

## Ejecutar

En Spring Tools Suite, importa el proyecto como **Existing Maven Project** y ejecuta `ProyectoFinalApplication` como **Spring Boot App**. La aplicación queda en `http://localhost:8083`.

## Pruebas

Las pruebas usan una base H2 temporal y no modifican la base MySQL local. En STS, ejecuta **Run As → Maven test**, o `mvnw.cmd test` en Windows.

## Despliegue en Azure

1. Crear **Azure Database for MySQL – Flexible Server**, crear la base `BDHotel` y ejecutar `BDHotel.sql`. En *Redes*, permitir el acceso desde servicios de Azure.
2. Crear un **App Service** Linux con pila **Java 17 (Java SE)**, con el nombre definido en `AZURE_WEBAPP_NAME` del workflow.
3. En App Service → *Configuración* → *Variables de entorno*, agregar:
   - `DB_URL` = `jdbc:mysql://<servidor>.mysql.database.azure.com:3306/BDHotel?useSSL=true&serverTimezone=America/Lima`
   - `DB_USER`, `DB_PASSWORD`
   - `PORT` = `80`
4. En App Service, descargar el **perfil de publicación** y guardarlo en GitHub → *Settings → Secrets and variables → Actions* como `AZURE_WEBAPP_PUBLISH_PROFILE`.
5. Hacer `git push` a `main`: GitHub Actions compila, ejecuta las pruebas y despliega.
