-- =====================================================================
-- TRAVEL AGENCY - DATABASE SCHEMA (DDL)
-- Solo definicion de estructura. No incluye datos.
-- Para datos de prueba, ejecutar db_mock.sql despues de este archivo.
-- =====================================================================

DROP DATABASE IF EXISTS travel_agency_db;
CREATE DATABASE travel_agency_db;
USE travel_agency_db;

-- ---------------------------------------------------------------------
-- 1. USER ENTITY
-- Guarda datos del NEGOCIO de cada usuario.
-- La autenticacion (contrasenas, login) la maneja Keycloak.
-- 'keycloak_id' es el puente entre Keycloak y esta tabla.
-- 'user_role' se mantiene como snapshot para reportes rapidos.
-- ---------------------------------------------------------------------
CREATE TABLE user_entity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keycloak_id VARCHAR(255) UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    id_document VARCHAR(50) UNIQUE,
    nationality VARCHAR(100),
    address_street VARCHAR(255),
    address_number VARCHAR(50),
    district VARCHAR(100),
    birth_date DATE,
    user_role VARCHAR(50) NOT NULL,            -- ADMIN o CLIENT
    account_status VARCHAR(50) NOT NULL,       -- ACTIVE, INACTIVE, BLOCKED
    registration_date DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- 2. TRAVEL PACKAGE
-- Catalogo de paquetes turisticos.
-- 'available_slots' se decrementa al confirmar reservas.
-- 'is_visible_web' permite ocultar sin eliminar (soft hide).
-- ---------------------------------------------------------------------
CREATE TABLE travel_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    destination VARCHAR(200) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    duration_days INT,
    base_price DECIMAL(19,2) NOT NULL,
    included_services TEXT,
    conditions TEXT,
    restrictions TEXT,
    total_slots INT NOT NULL,
    available_slots INT NOT NULL,
    travel_type VARCHAR(100),                  -- NATIONAL, INTERNATIONAL
    season VARCHAR(100),                       -- HIGH, LOW, MEDIUM
    category VARCHAR(100),                     -- ADVENTURE, FAMILY, BEACH, etc.
    package_status VARCHAR(50) NOT NULL,       -- AVAILABLE, SOLD_OUT, EXPIRED, CANCELED
    is_visible_web BOOLEAN DEFAULT TRUE
);

-- ---------------------------------------------------------------------
-- 3. BOOKING
-- Reserva: une un usuario con un paquete.
-- 'gross_amount'    = precio sin descuento
-- 'discount_amount' = suma de descuentos aplicados
-- 'final_amount'    = lo que paga el cliente
-- ---------------------------------------------------------------------
CREATE TABLE booking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    booking_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    passenger_count INT NOT NULL,
    gross_amount DECIMAL(19,2),
    discount_amount DECIMAL(19,2) DEFAULT 0,
    final_amount DECIMAL(19,2),
    booking_status VARCHAR(50) NOT NULL,       -- PENDING, CONFIRMED, CANCELED, EXPIRED
    payment_expiration_date DATETIME,
    booking_code VARCHAR(20) UNIQUE,
    FOREIGN KEY (user_id) REFERENCES user_entity(id),
    FOREIGN KEY (package_id) REFERENCES travel_package(id)
);

-- ---------------------------------------------------------------------
-- 4. BOOKING PASSENGER
-- Cada reserva puede tener varios pasajeros adicionales.
-- ---------------------------------------------------------------------
CREATE TABLE booking_passenger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    id_document VARCHAR(50),
    birth_date DATE,
    nationality VARCHAR(100),
    observations TEXT,
    FOREIGN KEY (booking_id) REFERENCES booking(id)
);

-- ---------------------------------------------------------------------
-- 5. PAYMENT (Simulado)
-- Pago de una reserva. Siempre se asume aprobado.
-- ---------------------------------------------------------------------
CREATE TABLE payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,         -- 1 pago por reserva
    payment_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    amount_paid DECIMAL(19,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,       -- CREDIT_CARD
    payment_status VARCHAR(50) NOT NULL,       -- APPROVED
    internal_transaction_id VARCHAR(100) UNIQUE,
    masked_card_number VARCHAR(20),            -- **** **** **** 4444
    card_expiration_date VARCHAR(10),
    simulated_cvv VARCHAR(4),
    FOREIGN KEY (booking_id) REFERENCES booking(id)
);

-- ---------------------------------------------------------------------
-- 6. INVOICE
-- Comprobante o factura generado tras el pago exitoso.
-- ---------------------------------------------------------------------
CREATE TABLE invoice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL UNIQUE,
    folio_number VARCHAR(50) UNIQUE NOT NULL,
    issue_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    invoice_type VARCHAR(50) NOT NULL,         -- RECEIPT, INVOICE
    snapshot_details TEXT,
    FOREIGN KEY (payment_id) REFERENCES payment(id)
);

-- ---------------------------------------------------------------------
-- 7. DISCOUNT CONFIG
-- Configuracion de descuentos disponibles (grupo, frecuente, promo).
-- ---------------------------------------------------------------------
CREATE TABLE discount_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    value_type VARCHAR(50) NOT NULL,           -- PERCENTAGE, FIXED_AMOUNT
    discount_value DECIMAL(19,2) NOT NULL,
    max_limit DECIMAL(19,2),
    discount_status VARCHAR(50) NOT NULL,      -- ACTIVE, INACTIVE
    start_date DATETIME,
    end_date DATETIME,
    application_criteria VARCHAR(50) NOT NULL, -- GROUP, FREQUENT, MULTI_PACKAGE, PROMO
    is_stackable BOOLEAN DEFAULT FALSE
);

-- ---------------------------------------------------------------------
-- 8. BOOKING DISCOUNT (Tabla intermedia)
-- Registra que descuentos se aplicaron a cada reserva.
-- ---------------------------------------------------------------------
CREATE TABLE booking_discount (
    booking_id BIGINT,
    discount_id BIGINT,
    applied_amount DECIMAL(19,2) NOT NULL,
    PRIMARY KEY (booking_id, discount_id),
    FOREIGN KEY (booking_id) REFERENCES booking(id),
    FOREIGN KEY (discount_id) REFERENCES discount_config(id)
);

-- ---------------------------------------------------------------------
-- INDICES para optimizar busquedas frecuentes
-- ---------------------------------------------------------------------
CREATE INDEX idx_user_email          ON user_entity(email);
CREATE INDEX idx_user_keycloak       ON user_entity(keycloak_id);
CREATE INDEX idx_package_status      ON travel_package(package_status);
CREATE INDEX idx_package_destination ON travel_package(destination);
CREATE INDEX idx_booking_user        ON booking(user_id);
CREATE INDEX idx_booking_package     ON booking(package_id);
CREATE INDEX idx_booking_status      ON booking(booking_status);