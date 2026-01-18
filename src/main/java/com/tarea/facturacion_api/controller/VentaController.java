package com.tarea.facturacion_api.controller;

import com.tarea.facturacion_api.dto.SolicitudCompraDto;
import com.tarea.facturacion_api.model.Venta;
import com.tarea.facturacion_api.service.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ventas")
public class VentaController {

    @Autowired
    private VentaService ventaService;

    @Autowired
    private com.tarea.facturacion_api.repository.ClienteRepository clienteRepository;

    @PostMapping("/comprar")
    public ResponseEntity<?> realizarCompra(
            @RequestBody SolicitudCompraDto solicitud,
            org.springframework.security.core.Authentication authentication) {

        try {
            // 1. Obtener usuario autenticado
            String username = authentication.getName(); // El email/usuario del token

            // 2. Buscar si existe un Cliente con ese email/nombre
            // (Asumimos que username del login se mapea a email o nombre del cliente)
            // Como ClienteRepository no tiene findByEmail nativo expuesto aquí, lo buscamos
            // manual o lo agregamos.
            // Para simplificar sin modificar repo, iteramos (ineficiente pero funcional
            // para prototipo)
            // O MEJOR: Usamos Example match o stream.

            com.tarea.facturacion_api.model.Cliente cliente = clienteRepository.findAll().stream()
                    .filter(c -> c.getEmail() != null && c.getEmail().equals(username))
                    .findFirst()
                    .orElse(null);

            // 3. Si no existe, CREARLO AUTOMÁTICAMENTE
            if (cliente == null) {
                cliente = new com.tarea.facturacion_api.model.Cliente();
                cliente.setNombre(username);
                cliente.setApellido("Usuario"); // Placeholder
                cliente.setEmail(username);
                cliente.setDireccion("Dirección por defecto");
                // Generar Cédula ficticia única (Timestamp reverso 10 digitos)
                String dummyCedula = String.valueOf(System.currentTimeMillis()).substring(3, 13);
                cliente.setCedula(dummyCedula);

                cliente = clienteRepository.save(cliente);
                System.out.println(">>> [AUTO-CLIENTE] Cliente creado automáticamente para: " + username);
            }

            // 4. Inyectar el ID real en la solicitud
            solicitud.setClienteId(cliente.getId());

            // 5. Proceder con la venta
            Venta venta = ventaService.realizarVenta(solicitud);
            return new ResponseEntity<>(venta, HttpStatus.CREATED);

        } catch (RuntimeException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
