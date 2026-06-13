-- Create the database if it doesn't exist
DROP DATABASE IF EXISTS travel_agency_db;
CREATE DATABASE travel_agency_db;
USE travel_agency_db;

-- 1. User Entity (Handles both Admins and Clients)
CREATE TABLE user_entity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keycloak_id VARCHAR(255) UNIQUE, -- Link to Keycloak IAM
    full_name VARCHAR(255) NOT NULL,
    address_street VARCHAR(255),
    address_number VARCHAR(50),
    district VARCHAR(100),
    email VARCHAR(150) NOT NULL UNIQUE, -- Email must be unique
    password_hash VARCHAR(255),
    phone_number VARCHAR(20),
    id_document VARCHAR(50) UNIQUE,
    nationality VARCHAR(100),
    account_status VARCHAR(50), -- ACTIVE, INACTIVE, BLOCKED
    registration_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    birth_date DATE,
    user_role VARCHAR(50) -- ADMIN or CLIENT
);

-- 2. Travel Package Entity
CREATE TABLE travel_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    destination VARCHAR(200) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL, -- Must be after start_date
    duration_days INT,
    base_price DECIMAL(19,2) NOT NULL, -- Price must be > 0
    included_services TEXT,
    conditions TEXT,
    restrictions TEXT,
    total_slots INT NOT NULL, -- Slots must be > 0
    available_slots INT,
    travel_type VARCHAR(100), -- National, International, etc.
    season VARCHAR(100),
    category VARCHAR(100),
    package_status VARCHAR(50), -- AVAILABLE, SOLD_OUT, EXPIRED, CANCELED
    is_visible_web BOOLEAN DEFAULT TRUE
);

-- 3. Booking Entity
CREATE TABLE booking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    package_id BIGINT,
    booking_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    passenger_count INT NOT NULL, -- Must be > 0
    gross_amount DECIMAL(19,2),
    discount_amount DECIMAL(19,2),
    final_amount DECIMAL(19,2), -- Never negative
    booking_status VARCHAR(50), -- PENDING, CONFIRMED, CANCELED, EXPIRED
    payment_expiration_date DATETIME,
    booking_code VARCHAR(20) UNIQUE,
    FOREIGN KEY (user_id) REFERENCES user_entity(id),
    FOREIGN KEY (package_id) REFERENCES travel_package(id)
);

-- 4. Booking Passenger Entity
CREATE TABLE booking_passenger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT,
    full_name VARCHAR(255),
    id_document VARCHAR(50),
    birth_date DATE,
    nationality VARCHAR(100),
    observations TEXT,
    FOREIGN KEY (booking_id) REFERENCES booking(id)
);

-- 5. Payment Entity (Simulated)
CREATE TABLE payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT,
    payment_date DATETIME,
    amount_paid DECIMAL(19,2), -- Must match booking total
    payment_method VARCHAR(50), -- Simulated credit card
    payment_status VARCHAR(50), -- APPROVED
    internal_transaction_id VARCHAR(100),
    masked_card_number VARCHAR(20), -- e.g., **** **** **** 4444
    card_expiration_date VARCHAR(10),
    simulated_cvv VARCHAR(4),
    FOREIGN KEY (booking_id) REFERENCES booking(id)
);

-- 6. Invoice Entity
CREATE TABLE invoice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT,
    folio_number VARCHAR(50) UNIQUE,
    issue_date DATETIME,
    invoice_type VARCHAR(50), -- RECEIPT (Boleta) or INVOICE (Factura)
    snapshot_details TEXT, -- JSON/Text copy of transaction data
    FOREIGN KEY (payment_id) REFERENCES payment(id)
);

-- 7. Discount Configuration Entity
CREATE TABLE discount_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    description TEXT,
    value_type VARCHAR(50), -- PERCENTAGE or FIXED_AMOUNT
    discount_value DECIMAL(19,2),
    max_limit DECIMAL(19,2), -- Max allowed discount
    discount_status VARCHAR(50), -- ACTIVE or INACTIVE
    start_date DATETIME,
    end_date DATETIME,
    application_criteria VARCHAR(50), -- GROUP, FREQUENT, MULTI_PACKAGE, PROMO
    is_stackable BOOLEAN -- If discounts can be combined
);

-- 8. Booking-Discount Join Table
CREATE TABLE booking_discount (
    booking_id BIGINT,
    discount_id BIGINT,
    applied_amount DECIMAL(19,2),
    PRIMARY KEY (booking_id, discount_id),
    FOREIGN KEY (booking_id) REFERENCES booking(id),
    FOREIGN KEY (discount_id) REFERENCES discount_config(id)
);

-- 9. Initial Data for testing
INSERT INTO user_entity (full_name, email, user_role, account_status) 
VALUES ('Jordan Steve', 'jordan@travelagency.com', 'ADMIN', 'ACTIVE');

INSERT INTO travel_package (name, destination, start_date, end_date, base_price, total_slots, available_slots, package_status) 
VALUES ('Welcome to Chile', 'Santiago', '2026-06-01', '2026-06-15', 500.00, 20, 20, 'AVAILABLE');