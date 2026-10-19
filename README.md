# Hotel Los Andes

Aplicación web para administrar clientes, habitaciones, reservas y reportes de un hotel. Está construida con Java, Spring Boot, Thymeleaf y MySQL.

## Preparar la base de datos

1. Inicia MySQL y abre `BDHotel.sql` en MySQL Workbench.
2. Ejecuta el script completo. Crea las tablas que falten y agrega datos de ejemplo sin duplicar los existentes.
3. En Spring Tools Suite, configura la variable de entorno `DB_PASSWORD` en la configuración de ejecución de `ProyectoFinalApplication` con la contraseña de tu usuario MySQL.

La URL y el usuario de la conexión se definen en `src/main/resources/application.properties`. El proyecto usa `spring.jpa.hibernate.ddl-auto=none`; por eso, las tablas se crean ejecutando el SQL.

Usuarios de ejemplo incluidos en el SQL:

| Usuario | Contraseña inicial | Rol |
|---|---|---|
| `admin` | `123456` | Administrador |
| `recepcion` | `123456` | Recepcionista |

Son credenciales de demostración; cámbialas antes de utilizar el sistema con información real.

## Ejecutar

En Spring Tools Suite, importa el proyecto como **Existing Maven Project** y ejecuta `ProyectoFinalApplication` como **Spring Boot App**. La aplicación queda en `http://localhost:8083`.

## Reservas y reportes

- El administrador y el recepcionista pueden consultar y registrar reservas.
- La disponibilidad se calcula para el intervalo elegido y excluye habitaciones con reservas superpuestas o en mantenimiento.
- Las reservas confirmadas actualizan el estado de la habitación a `RESERVADA`.
- El administrador puede abrir el listado de reservas y la proyección de ingresos en PDF desde el panel principal. La proyección usa la tarifa por noche y no representa pagos registrados.

## Pruebas

Las pruebas de integración usan una base H2 temporal y no modifican la base MySQL local. En STS, ejecuta **Run As → Maven test**. También puedes usar `mvnw.cmd test` desde Windows.
