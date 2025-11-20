# API de Facturación Segura (JWT & Swagger)

Esta API REST permite la gestión de facturación con autenticación segura mediante JSON Web Tokens (JWT).

## 🔒 Seguridad y Acceso
La API está protegida. Para acceder a los endpoints privados (Clientes, Productos, Facturas), debe autenticarse primero.

### Pasos para Autenticarse:
1. **Registrar Usuario (Solo primera vez):**
   - `POST /auth/register`
   - Body: `{"username": "admin", "password": "123", "role": "ADMIN"}`
2. **Iniciar Sesión:**
   - `POST /auth/login`
   - Body: `{"username": "admin", "password": "123"}`
   - **Respuesta:** Recibirá un `jwt` (Token).
3. **Usar el Token:**
   - Copie el token recibido.
   - En cada petición subsiguiente, añada el encabezado `Authorization`: `Bearer <SU_TOKEN>`.

---

## 📚 Documentación Interactiva (Swagger)
Puede probar todos los endpoints y ver la documentación automática ingresando a:
👉 **http://localhost:8080/swagger-ui/index.html**

---

## 🚀 Instrucciones de Ejecución
1. **Base de Datos:** Asegúrese de que MySQL esté corriendo y la DB `facturacion_db` exista.
2. **Ejecutar:**
   ```bash
   ./mvnw spring-boot:run