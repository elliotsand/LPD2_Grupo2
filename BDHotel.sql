CREATE DATABASE BDHotel;
USE BDHotel;


CREATE TABLE usuarios (
    idUsuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    rol VARCHAR(30) NOT NULL
);

CREATE TABLE clientes (
    idCliente INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    documento VARCHAR(20) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    correo VARCHAR(100)
);


CREATE TABLE tipos_habitacion (
    idTipoHabitacion INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    precio DECIMAL(10,2) NOT NULL
);


CREATE TABLE habitaciones (
    idHabitacion INT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(10) NOT NULL UNIQUE,
    piso INT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    idTipoHabitacion INT NOT NULL,

    CONSTRAINT fk_habitacion_tipo
        FOREIGN KEY (idTipoHabitacion)
        REFERENCES tipos_habitacion(idTipoHabitacion)
);

CREATE TABLE reservas (
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
INSERT INTO usuarios (nombre, username, password, rol)
VALUES
('Administrador', 'admin', '123456', 'ADMIN'),
('Recepcionista', 'recepcion', '123456', 'RECEPCIONISTA');


-- CLIENTES
INSERT INTO clientes (nombre, documento, telefono, correo)
VALUES
('Juan Perez', '12345678', '987654321', 'juan@gmail.com'),
('Maria Lopez', '87654321', '912345678', 'maria@gmail.com');


-- TIPOS DE HABITACIÓN
INSERT INTO tipos_habitacion (nombre, precio)
VALUES
('Simple', 120.00),
('Doble', 180.00),
('Suite', 300.00);


-- HABITACIONES
INSERT INTO habitaciones
(numero, piso, estado, idTipoHabitacion)
VALUES
('101', 1, 'DISPONIBLE', 1),
('102', 1, 'DISPONIBLE', 1),
('201', 2, 'DISPONIBLE', 2),
('202', 2, 'DISPONIBLE', 2),
('301', 3, 'DISPONIBLE', 3);

SHOW TABLES;