-- =====================================================================
-- TRAVEL AGENCY - MOCK DATA (DML)
-- Datos de prueba para desarrollo y demostracion.
-- Ejecutar DESPUES de db_schema.sql.
-- =====================================================================

USE travel_agency_db;

-- ---------------------------------------------------------------------
-- 1. USUARIOS
-- 'keycloak_id' es un placeholder. En produccion vendria del JWT.
-- ---------------------------------------------------------------------
INSERT INTO user_entity (keycloak_id, full_name, email, phone_number, id_document, nationality, district, user_role, account_status) VALUES
('kc-admin-001',  'Jordan Steve',  'jordan@travelagency.com', '+56911111111', '11111111-1', 'Chilena', 'Santiago Centro', 'ADMIN',  'ACTIVE'),
('kc-admin-002',  'Diego Perez',   'diego@travelagency.com',  '+56922222222', '22222222-2', 'Chilena', 'Providencia',     'ADMIN',  'ACTIVE'),
('kc-client-001', 'Juan Lopez',    'juan@gmail.com',          '+56933333333', '33333333-3', 'Chilena', 'Las Condes',      'CLIENT', 'ACTIVE'),
('kc-client-002', 'Sofia Ramirez', 'sofia@gmail.com',         '+56944444444', '44444444-4', 'Chilena', 'Nunoa',           'CLIENT', 'ACTIVE'),
('kc-client-003', 'Maria Torres',  'maria@hotmail.com',       '+56955555555', '55555555-5', 'Argentina', 'Vitacura',      'CLIENT', 'ACTIVE');

-- ---------------------------------------------------------------------
-- 2. PAQUETES TURISTICOS
-- ---------------------------------------------------------------------
INSERT INTO travel_package (name, destination, description, start_date, end_date, duration_days, base_price, included_services, total_slots, available_slots, travel_type, season, category, package_status, is_visible_web) VALUES
('Aventura en Torres del Paine', 'Patagonia, Chile', 'Trekking de 5 dias por las Torres del Paine, hospedaje en refugios incluido', '2026-09-01', '2026-09-05', 5, 850000.00, 'Transporte, alojamiento, guia', 15, 15, 'NATIONAL', 'HIGH', 'ADVENTURE', 'AVAILABLE', TRUE),
('Playas de Cancun',             'Cancun, Mexico',  'Resort 5 estrellas todo incluido frente al mar Caribe',                       '2026-10-15', '2026-10-22', 7, 1500000.00, 'Vuelos, hotel, comidas, traslados', 20, 20, 'INTERNATIONAL', 'HIGH', 'BEACH', 'AVAILABLE', TRUE),
('Tour por San Pedro de Atacama', 'San Pedro, Chile', 'Recorrido por el desierto mas seco del mundo, valles y geisers',             '2026-08-10', '2026-08-14', 4, 650000.00, 'Bus, hotel 4 estrellas, excursiones', 25, 23, 'NATIONAL', 'MEDIUM', 'ADVENTURE', 'AVAILABLE', TRUE),
('Europa Clasica',               'Paris-Roma-Madrid', 'Recorrido por las 3 capitales europeas mas iconicas',                       '2026-11-01', '2026-11-15', 14, 3500000.00, 'Vuelos, hoteles 4 estrellas, tours guiados', 30, 30, 'INTERNATIONAL', 'LOW', 'CULTURAL', 'AVAILABLE', TRUE),
('Disney Family Trip',           'Orlando, USA',    'Paquete familiar con entradas a parques Disney',                              '2026-12-20', '2026-12-30', 10, 2800000.00, 'Vuelos, hotel Disney, entradas, traslados', 40, 38, 'INTERNATIONAL', 'HIGH', 'FAMILY', 'AVAILABLE', TRUE),
('Buenos Aires Express',         'Buenos Aires, Argentina', 'Fin de semana en Buenos Aires con tours y gastronomia',               '2026-07-25', '2026-07-28', 3, 450000.00, 'Vuelos, hotel boutique, city tour', 20, 0, 'INTERNATIONAL', 'MEDIUM', 'CULTURAL', 'SOLD_OUT', TRUE),
('Crucero por el Mediterraneo',  'Italia-Grecia-Turquia', 'Crucero de lujo 10 dias en el Mediterraneo',                            '2025-06-01', '2025-06-11', 10, 4200000.00, 'Crucero todo incluido, excursiones', 50, 50, 'INTERNATIONAL', 'HIGH', 'LUXURY', 'EXPIRED', FALSE);

-- ---------------------------------------------------------------------
-- 3. CONFIGURACIONES DE DESCUENTOS
-- ---------------------------------------------------------------------
INSERT INTO discount_config (name, description, value_type, discount_value, max_limit, discount_status, start_date, end_date, application_criteria, is_stackable) VALUES
('Descuento por grupo',           'Descuento al reservar para 4 o mas pasajeros', 'PERCENTAGE', 10.00, 500000.00, 'ACTIVE', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'GROUP',         TRUE),
('Cliente frecuente',             'Descuento para clientes con 3+ reservas pagadas', 'PERCENTAGE', 5.00, 300000.00, 'ACTIVE', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'FREQUENT',      TRUE),
('Multi-paquete',                 'Descuento al comprar mas de un paquete', 'PERCENTAGE', 7.00, 400000.00, 'ACTIVE', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'MULTI_PACKAGE', TRUE),
('Promo verano 2026',             'Promocion temporada alta', 'FIXED_AMOUNT', 50000.00, 50000.00, 'ACTIVE', '2026-12-01 00:00:00', '2027-03-31 23:59:59', 'PROMO',         FALSE);

-- ---------------------------------------------------------------------
-- 4. RESERVAS DE EJEMPLO
-- Juan ya tiene una reserva confirmada en San Pedro
-- ---------------------------------------------------------------------
INSERT INTO booking (user_id, package_id, passenger_count, gross_amount, discount_amount, final_amount, booking_status, booking_code) VALUES
(3, 3, 2, 1300000.00, 0,        1300000.00, 'CONFIRMED', 'BK-2026-0001'),
(4, 5, 4, 11200000.00, 1120000.00, 10080000.00, 'CONFIRMED', 'BK-2026-0002'),
(3, 1, 1, 850000.00,  0,        850000.00,  'PENDING',   'BK-2026-0003');

-- ---------------------------------------------------------------------
-- 5. PAGOS DE LAS RESERVAS CONFIRMADAS
-- ---------------------------------------------------------------------
INSERT INTO payment (booking_id, amount_paid, payment_method, payment_status, internal_transaction_id, masked_card_number, card_expiration_date) VALUES
(1, 1300000.00,  'CREDIT_CARD', 'APPROVED', 'TXN-2026-0001', '**** **** **** 4444', '12/27'),
(2, 10080000.00, 'CREDIT_CARD', 'APPROVED', 'TXN-2026-0002', '**** **** **** 1111', '08/28');

-- ---------------------------------------------------------------------
-- 6. FACTURAS
-- ---------------------------------------------------------------------
INSERT INTO invoice (payment_id, folio_number, invoice_type, snapshot_details) VALUES
(1, 'F-2026-00001', 'RECEIPT', 'Reserva BK-2026-0001 - San Pedro de Atacama - 2 pasajeros'),
(2, 'F-2026-00002', 'INVOICE', 'Reserva BK-2026-0002 - Disney Family Trip - 4 pasajeros');

-- ---------------------------------------------------------------------
-- 7. APLICACION DE DESCUENTO A LA RESERVA #2 (grupo de 4)
-- ---------------------------------------------------------------------
INSERT INTO booking_discount (booking_id, discount_id, applied_amount) VALUES
(2, 1, 1120000.00);