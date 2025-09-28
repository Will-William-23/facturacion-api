
# API de Facturación (Spring Boot)

Este proyecto es una API REST para un sistema de facturación simple, construido con Spring Boot, JPA y MySQL. Permite gestionar Clientes, Productos y crear Facturas, validando el stock de productos y calculando totales automáticamente.

## Instrucciones de Ejecución

### 1. Pre-requisitos
* JDK 21 (o 17+) instalado.
* Maven instalado.
* MySQL Server (como MySQL Workbench) ejecutándose.

### 2. Script de Base de Datos
Este proyecto usa JPA (`ddl-auto=update`) para crear las tablas automáticamente. Sin embargo, la base de datos (schema) debe ser creada manualmente.

Ejecute el siguiente comando en MySQL Workbench:
```sql
CREATE DATABASE facturacion_db;

3. Configuración del Proyecto
Abra el archivo src/main/resources/application.properties y asegúrese de que las credenciales de su base de datos sean correctas:
spring.datasource.url=jdbc:mysql://localhost:3306/facturacion_db?serverTimezone=UTC
spring.datasource.username="su usuario"
spring.datasource.password="Su contraseña"
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

4. Ejecutar la Aplicación
Puede ejecutar la aplicación desde su IDE (ejecutando el método main en FacturacionApiApplication.java) o usando Maven:
# Desde la carpeta raíz del proyecto
./mvnw spring-boot:run
La API estará disponible en http://localhost:8080

Documentación de Endpoints REST
URL Base: http://localhost:8080

Endpoints de Clientes (/clientes)
GET /clientes
Descripción: Lista todos los clientes.

Respuesta Exitosa (200 OK): Array de objetos Cliente.

GET /clientes/{id}
Descripción: Obtiene un cliente por su ID.

Respuesta Exitosa (200 OK): Objeto Cliente.

Respuesta de Error (404 Not Found): Si el ID no existe.

POST /clientes
Descripción: Crea un nuevo cliente.

Body (JSON):
{
    "nombre": "Nombre Cliente",
    "apellido": "Apellido Cliente",
    "direccion": "Dirección Cliente",
    "email": "cliente@correo.com"
}
Respuesta Exitosa (201 Created): Objeto Cliente creado (con su ID).

PUT /clientes/{id}
Descripción: Actualiza un cliente existente.

Body (JSON): Mismo formato que el POST.

Respuesta Exitosa (200 OK): Objeto Cliente actualizado.

Respuesta de Error (404 Not Found): Si el ID no existe.

DELETE /clientes/{id}
Descripción: Elimina un cliente por su ID.

Respuesta Exitosa (204 No Content): Sin contenido.

Respuesta de Error (404 Not Found): Si el ID no existe.

Endpoints de Productos (/productos)
GET /productos
Descripción: Lista todos los productos.

Respuesta Exitosa (200 OK): Array de objetos Producto.

GET /productos/{id}
Descripción: Obtiene un producto por su ID.

Respuesta Exitosa (200 OK): Objeto Producto.

Respuesta de Error (404 Not Found): Si el ID no existe.

POST /productos
Descripción: Crea un nuevo producto.

Body (JSON):
{
    "nombre": "Nombre Producto",
    "descripcion": "Descripción del producto",
    "precio": 99.99,
    "stock": 100
}

Respuesta Exitosa (201 Created): Objeto Producto creado (con su ID).

PUT /productos/{id}
Descripción: Actualiza un producto existente.

Body (JSON): Mismo formato que el POST.

Respuesta Exitosa (200 OK): Objeto Producto actualizado.

Respuesta de Error (404 Not Found): Si el ID no existe.

DELETE /productos/{id}
Descripción: Elimina un producto por su ID.

Respuesta Exitosa (204 No Content): Sin contenido.

Respuesta de Error (404 Not Found): Si el ID no existe.

Endpoints de Facturas (/facturas)
GET /facturas
Descripción: Lista todas las facturas (incluyendo cliente y detalles).

Respuesta Exitosa (200 OK): Array de objetos Factura.

GET /facturas/{id}
Descripción: Obtiene una factura por su ID (incluyendo cliente y detalles).

Respuesta Exitosa (200 OK): Objeto Factura.

Respuesta de Error (404 Not Found): Si el ID no existe.

POST /facturas
Descripción: Crea una nueva factura. Valida el stock de productos y calcula el total automáticamente.

Body (JSON):
{
    "cliente": {
        "id": 1 
    },
    "detalles": [
        {
            "producto": { "id": 1 },
            "cantidad": 1
        },
        {
            "producto": { "id": 2 },
            "cantidad": 5
        }
    ]
}
Respuesta Exitosa (201 Created): Objeto Factura creado (con total calculado y fecha).

Respuesta de Error (400 Bad Request): Si el stock es insuficiente o el cliente/producto no existe.

