# Travel Agency - Sistema de Gestión de Reservas

Proyecto desarrollado para el ramo **Métodos/Técnicas de Ingeniería de Software- Evaluación 1 (2026 - 1)** (USACH).
Sistema web full-stack para una agencia de viajes que permite a clientes buscar y reservar paquetes turísticos, y a administradores gestionar el catálogo completo.

## Stack Tecnológico

| Capa             | Tecnología                               |
| ---------------- | ---------------------------------------- |
| Backend          | Spring Boot 3.4 + Java 17                |
| Frontend         | React 19 + Vite + Bootstrap              |
| Base de datos    | MySQL 8                                  |
| Autenticación    | Keycloak 26 (OAuth 2.0 / OpenID Connect) |
| Containerización | Docker + Docker Compose                  |
| Load Balancer    | Nginx                                    |
| Infraestructura  | AWS EC2                                  |
| CI/CD            | GitHub Actions                           |
| Testing          | JUnit 5 + Mockito + JaCoCo               |

## Funcionalidades implementadas

### Para clientes

- Registro público en la plataforma
- Catálogo de paquetes turísticos con filtros (destino, precio, fechas)
- Detalle de paquete con cálculo dinámico de descuentos
- Sistema de reservas con pasajeros
- Procesamiento de pagos simulado con tarjeta de crédito
- Generación automática de boletas/facturas
- Historial de reservas personal

### Para administradores

- CRUD de paquetes turísticos
- Gestión de descuentos (GROUP, FREQUENT, MULTI_PACKAGE, PROMO)
- Administración de reservas (vista global, filtros, cancelación forzada)
- Gestión de usuarios (creación, activación/desactivación)
- Reportes de ventas y ranking de paquetes por período

## Arquitectura

                ┌─────────────────┐
                │   Internet      │
                └────────┬────────┘
                         │
                ┌────────▼─────────┐
                │  Nginx Frontend  │  (Load Balancer puerto 8070)
                └────────┬─────────┘
                         │
          ┌──────────────┼──────────────┐
          │              │              │
     ┌────▼───┐     ┌────▼───┐     ┌────▼───┐
     │Front 1 │     │Front 2 │     │Front 3 │
     └────────┘     └────────┘     └────────┘
                         │
                ┌────────▼─────────┐
                │  Nginx Backend   │  (Load Balancer puerto 8090)
                └────────┬─────────┘
                         │
          ┌──────────────┼──────────────┐
          │              │              │
     ┌────▼───┐     ┌────▼───┐     ┌────▼───┐
     │Back 1  │     │Back 2  │     │Back 3  │
     └────┬───┘     └────┬───┘     └────┬───┘
          │              │              │
          └──────────────┼──────────────┘
                         │
                ┌────────▼─────────┐
                │     MySQL 8      │
                └──────────────────┘
                         │
                ┌────────▼─────────┐
                │   Keycloak 26    │  (puerto 9090)
                └──────────────────┘

## Despliegue

### Desarrollo local

Requisitos: Docker, Docker Compose, Java 17, Node.js 20, MySQL local, Keycloak local.

```bash
git clone https://github.com/ElsucioSteve/TravelAgency.git
cd TravelAgency
docker compose up -d
```

### Producción en AWS EC2

La aplicación está desplegada en AWS EC2 con Docker Compose orquestando 10 contenedores (3 réplicas backend + 3 réplicas frontend + 2 Nginx LB + MySQL + Keycloak).

## Testing

El backend tiene **cobertura ≥90%** en la capa de servicios (lógica de negocio), verificada con JaCoCo:

```bash
cd backend/travel-agency-backend
mvn clean test
```

Reporte HTML en `target/site/jacoco/index.html`.

## Pipeline CI/CD

GitHub Actions automatiza el ciclo:

1. Obtener código del repositorio
2. Ejecutar pruebas unitarias JUnit
3. Generar JAR del backend
4. Construir imágenes Docker
5. Publicar imágenes en Docker Hub

Imágenes publicadas:

- `jordansgr/travel-agency-backend:latest`
- `jordansgr/travel-agency-frontend:latest`

## Autor

**Jordan Steven Gonzalez Riquelme**
Universidad de Santiago de Chile (USACH)
