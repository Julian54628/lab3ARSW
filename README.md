# Blueprints REST API

API REST para la gestión de planos (blueprints) desarrollada con Java 21 y Spring Boot 3.3.x.
Este proyecto implementa un sistema de persistencia con PostgreSQL, filtros de procesamiento de puntos,
y documentación automática con OpenAPI/Swagger.

## Tabla de Contenidos
- [Tecnologías](#tecnologías)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Configuración y Ejecución](#configuración-y-ejecución)
- [API Endpoints](#api-endpoints)
- [Ejemplos de Uso](#ejemplos-de-uso)
- [Filtros de Procesamiento](#filtros-de-procesamiento)
- [Base de Datos](#base-de-datos)
- [Buenas Prácticas Implementadas](#buenas-prácticas-implementadas)
- [Documentación Swagger](#documentación-swagger)

## Tecnologías

- Java 21 - Lenguaje de programación
- Spring Boot 3.3.x - Framework principal
- PostgreSQL - Base de datos relacional
- Maven - Gestión de dependencias
- Springdoc OpenAPI - Documentación automática
- JdbcTemplate - Acceso a datos

## Estructura del Proyecto
src/main/java/edu/eci/arsw/blueprints/
├── config/ # Configuración de Swagger/OpenAPI
├── controllers/ # Controladores REST y DTOs
│   ├── ApiResponse.java
│   └── BlueprintsAPIController.java
├── filters/ # Filtros de procesamiento
│   ├── BlueprintsFilter.java
│   ├── IdentityFilter.java
│   ├── RedundancyFilter.java
│   └── UndersamplingFilter.java
├── model/ # Entidades de dominio
│   ├── Blueprint.java
│   └── Point.java
├── persistence/ # Capa de persistencia
│   ├── BlueprintPersistence.java
│   ├── BlueprintNotFoundException.java
│   ├── BlueprintPersistenceException.java
│   ├── impl/
│   │   ├── InMemoryBlueprintPersistence.java
│   │   └── PostgresBlueprintPersistence.java
└── services/ # Lógica de negocio
    └── BlueprintsServices.java

## Configuración y Ejecución

### Prerrequisitos
- Java 21
- Maven 3.9+
- PostgreSQL (puede usarse Docker)
- Docker (opcional, para base de datos)

### Configuración de Base de Datos

1. Iniciar PostgreSQL con Docker:
```bash
docker run --name blueprints-db \
  -e POSTGRES_DB=blueprints \
  -e POSTGRES_USER=user \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15
```

2. Ejecutar script de inicialización (ejemplo):
```sql
CREATE TABLE IF NOT EXISTS blueprints (
    author VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (author, name)
);

CREATE TABLE IF NOT EXISTS points (
    id SERIAL PRIMARY KEY,
    author VARCHAR(100) NOT NULL,
    bpname VARCHAR(100) NOT NULL,
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    FOREIGN KEY (author, bpname) REFERENCES blueprints(author, name) ON DELETE CASCADE
);
```

3. Ejemplo de configuración de datasource (application.yml):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/blueprints
    username: user
    password: password
    driver-class-name: org.postgresql.Driver
  profiles:
    active: redundancy  # Opciones: redundancy, undersampling, (default vacío para IdentityFilter)
```

### Compilar y Ejecutar
```bash
# Compilar el proyecto
mvn clean install

# Ejecutar la aplicación
mvn spring-boot:run
```

## API Endpoints
La API está versionada bajo /api/v1/blueprints

Método  Endpoint                                           Descripción                     Códigos HTTP
GET     /api/v1/blueprints                                  Obtener todos los planos        200 OK
GET     /api/v1/blueprints/{author}                         Planos por autor                200 OK, 404 Not Found
GET     /api/v1/blueprints/{author}/{name}                  Plano específico                200 OK, 404 Not Found
POST    /api/v1/blueprints                                  Crear nuevo plano               201 Created, 400 Bad Request
PUT     /api/v1/blueprints/{author}/{name}/points           Agregar punto                   202 Accepted, 404 Not Found

Formato de Respuesta Uniforme:
```json
{
  "code": 200,
  "message": "ok",
  "data": { ... }
}
```

## Ejemplos de Uso

Obtener todos los planos:
```bash
curl -s http://localhost:8080/api/v1/blueprints | jq
```

Crear un nuevo plano:
```bash
curl -i -X POST http://localhost:8080/api/v1/blueprints \
  -H 'Content-Type: application/json' \
  -d '{
    "author":"john",
    "name":"kitchen",
    "points":[
      {"x":1,"y":1},
      {"x":2,"y":2}
    ]
  }'
```

Agregar un punto a un plano existente:
```bash
curl -i -X PUT http://localhost:8080/api/v1/blueprints/john/kitchen/points \
  -H 'Content-Type: application/json' \
  -d '{"x":3,"y":3}'
```

## Filtros de Procesamiento

El sistema implementa filtros configurables mediante perfiles de Spring:

1. IdentityFilter (Default)  
   No aplica ninguna transformación. Activo cuando no hay perfil específico.

2. RedundancyFilter (Perfil: redundancy)  
   Elimina puntos consecutivos duplicados.  
   Ejemplo: [(1,1), (1,1), (2,2)] → [(1,1), (2,2)]

3. UndersamplingFilter (Perfil: undersampling)  
   Conserva 1 de cada 2 puntos (índices pares).  
   Ejemplo: [P1, P2, P3, P4, P5] → [P1, P3, P5]

Activar filtros:
```bash
# Activar filtro de redundancia
mvn spring-boot:run -Dspring-boot.run.profiles=redundancy

# Activar filtro de undersampling
mvn spring-boot:run -Dspring-boot.run.profiles=undersampling
```

## Base de Datos

Esquema PostgreSQL:
```sql
-- Tabla de planos
CREATE TABLE blueprints (
    author VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (author, name)
);

-- Tabla de puntos con relación a planos
CREATE TABLE points (
    id SERIAL PRIMARY KEY,
    author VARCHAR(100) NOT NULL,
    bpname VARCHAR(100) NOT NULL,
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    FOREIGN KEY (author, bpname) REFERENCES blueprints(author, name) ON DELETE CASCADE
);
```

## Buenas Prácticas Implementadas

1. Versionamiento de API  
   Prefijo /api/v1/ en todos los endpoints.

2. Códigos HTTP Apropiados  
   200 OK - Consultas exitosas  
   201 Created - Creación de recursos  
   202 Accepted - Actualizaciones aceptadas  
   400 Bad Request - Datos de entrada inválidos  
   404 Not Found - Recurso no existente

3. Respuesta Uniforme  
   Clase genérica ApiResponse<T> para todas las respuestas. Consistencia en el formato de errores.

4. Manejo de Excepciones  
   Excepciones específicas del dominio (BlueprintNotFoundException, BlueprintPersistenceException) mapeadas a códigos HTTP.

5. Validación de Entrada  
   Uso de Bean Validation (@NotBlank, @Valid) y manejo centralizado de errores de validación.

6. Inyección de Dependencias  
   Constructor injection (mejor para testing) e interfaces para desacoplamiento.

7. Patrón Repository  
   Abstracción de la capa de persistencia para facilitar el cambio entre implementaciones.

## Documentación Swagger

La API está documentada automáticamente con OpenAPI 3.0:

Swagger UI: http://localhost:8080/swagger-ui.html  
OpenAPI JSON: http://localhost:8080/v3/api-docs

Características de la documentación:
- Descripción de todos los endpoints
- Modelos de datos (schemas)
- Códigos de respuesta posibles
- Ejemplos de uso
- Anotaciones @Operation y @ApiResponse

## Pruebas
```bash
# Ejecutar pruebas unitarias
mvn test

# Ejecutar pruebas de integración
mvn verify
```

## Métricas con Actuator
Si se activa Spring Boot Actuator:
```bash
# Health check
curl http://localhost:8080/actuator/health

# Métricas de la aplicación
curl http://localhost:8080/actuator/metrics
```

## Licencia
Este proyecto es creado con fines educativos para la Escuela Colombiana de Ingeniería - Arquitecturas de Software.
