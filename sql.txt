CREATE DATABASE IF NOT EXISTS BDHotel;
USE BDHotel;


CREATE TABLE IF NOT EXISTS usuarios (
    idUsuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    rol VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS clientes (
    idCliente INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    documento VARCHAR(20) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    correo VARCHAR(100)
);


CREATE TABLE IF NOT EXISTS tipos_habitacion (
    idTipoHabitacion INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    precio DECIMAL(10,2) NOT NULL
);


CREATE TABLE IF NOT EXISTS habitaciones (
    idHabitacion INT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(10) NOT NULL UNIQUE,
    piso INT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    idTipoHabitacion INT NOT NULL,

    CONSTRAINT fk_habitacion_tipo
        FOREIGN KEY (idTipoHabitacion)
        REFERENCES tipos_habitacion(idTipoHabitacion)
);

CREATE TABLE IF NOT EXISTS reservas (
    idReserva INT AUTO_INCREMENT PRIMARY KEY,
    fechaEntrada DATE NOT NULL,
    fechaSalida DATE NOT NULL,
    estado VARCHAR(20) NOT NULL,
    idCliente INT NOT NULL,
    idHabitacion INT NOT NULL,

    CONSTRAINT fk_reserva_cliente
        FOREIGN KEY (idCliente)
        REFERENCES clientes(idCliente),

    CONSTRAINT fk_reserva_habitacion
        FOREIGN KEY (idHabitacion)
        REFERENCES habitaciones(idHabitacion)
);

-- USUARIOS
INSERT IGNORE INTO usuarios (nombre, username, password, rol)
VALUES
('Administrador', 'admin', '123456', 'ADMIN'),
('Recepcionista', 'recepcion', '123456', 'RECEPCIONISTA');


-- CLIENTES
INSERT IGNORE INTO clientes (nombre, documento, telefono, correo)
VALUES
('Juan Perez', '12345678', '987654321', 'juan@gmail.com'),
('Maria Lopez', '87654321', '912345678', 'maria@gmail.com'),
('Carlos Ramirez Soto', '45678912', '956123478', 'carlos.ramirez@gmail.com'),
('Lucia Fernandez Diaz', '70123456', '944556677', 'lucia.fd@hotmail.com'),
('John Smith', 'PA1234567', '+15551234567', 'john.smith@mail.com');


-- TIPOS DE HABITACIÓN
INSERT INTO tipos_habitacion (nombre, precio)
SELECT tipos.nombre, tipos.precio
FROM (
    SELECT 'Simple' AS nombre, 120.00 AS precio
    UNION ALL SELECT 'Doble', 180.00
    UNION ALL SELECT 'Matrimonial', 220.00
    UNION ALL SELECT 'Suite', 300.00
) AS tipos
WHERE NOT EXISTS (
    SELECT 1 FROM tipos_habitacion existente WHERE existente.nombre = tipos.nombre
);


-- HABITACIONES
INSERT IGNORE INTO habitaciones (numero, piso, estado, idTipoHabitacion)
SELECT habitaciones.numero, habitaciones.piso, habitaciones.estado,
       tipo.idTipoHabitacion
FROM (
    SELECT '101' AS numero, 1 AS piso, 'DISPONIBLE' AS estado, 'Simple' AS tipo
    UNION ALL SELECT '102', 1, 'DISPONIBLE', 'Simple'
    UNION ALL SELECT '201', 2, 'DISPONIBLE', 'Doble'
    UNION ALL SELECT '103', 1, 'MANTENIMIENTO', 'Simple'
    UNION ALL SELECT '202', 2, 'DISPONIBLE', 'Doble'
    UNION ALL SELECT '203', 2, 'DISPONIBLE', 'Matrimonial'
    UNION ALL SELECT '204', 2, 'DISPONIBLE', 'Matrimonial'
    UNION ALL SELECT '301', 3, 'DISPONIBLE', 'Suite'
    UNION ALL SELECT '302', 3, 'DISPONIBLE', 'Suite'
) AS habitaciones
JOIN tipos_habitacion tipo ON tipo.nombre = habitaciones.tipo;


-- RESERVAS DE EJEMPLO (fechas relativas al día en que se ejecuta el script)
INSERT INTO reservas (fechaEntrada, fechaSalida, estado, idCliente, idHabitacion)
SELECT datos.entrada, datos.salida, datos.estado, c.idCliente, h.idHabitacion
FROM (
    SELECT DATE_SUB(CURDATE(), INTERVAL 20 DAY) AS entrada, DATE_SUB(CURDATE(), INTERVAL 17 DAY) AS salida,
           'FINALIZADA' AS estado, '12345678' AS documento, '201' AS numero
    UNION ALL SELECT DATE_SUB(CURDATE(), INTERVAL 12 DAY), DATE_SUB(CURDATE(), INTERVAL 10 DAY), 'FINALIZADA', '87654321', '301'
    UNION ALL SELECT DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_SUB(CURDATE(), INTERVAL 4 DAY), 'CANCELADA', '45678912', '102'
    UNION ALL SELECT DATE_ADD(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'CONFIRMADA', '70123456', '203'
    UNION ALL SELECT DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY), 'CONFIRMADA', 'PA1234567', '302'
) AS datos
JOIN clientes c ON c.documento = datos.documento
JOIN habitaciones h ON h.numero = datos.numero
WHERE NOT EXISTS (
    SELECT 1 FROM reservas r WHERE r.idCliente = c.idCliente AND r.idHabitacion = h.idHabitacion
);

-- Las habitaciones con reserva confirmada quedan como RESERVADA
SET SQL_SAFE_UPDATES = 0;
UPDATE habitaciones h
SET h.estado = 'RESERVADA'
WHERE h.estado = 'DISPONIBLE'
  AND EXISTS (SELECT 1 FROM reservas r
              WHERE r.idHabitacion = h.idHabitacion AND r.estado = 'CONFIRMADA' AND r.fechaSalida > CURDATE());
SET SQL_SAFE_UPDATES = 1;

-- Nota: las contraseñas '123456' se cifran con BCrypt automáticamente en el primer inicio de sesión.

SHOW TABLES;
