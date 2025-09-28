package com.tarea.facturacion_api.controller; // <-- Nombre corregido

import com.tarea.facturacion_api.model.Factura; // <-- Nombre corregido
import com.tarea.facturacion_api.service.FacturaService; // <-- Nombre corregido
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/facturas")
public class FacturaController {

    @Autowired
    private FacturaService facturaService;

    // GET /facturas -> Listar todas
    @GetMapping
    public List<Factura> listarFacturas() {
        return facturaService.listarFacturas();
    }

    // GET /facturas/{id} -> Obtener una
    @GetMapping("/{id}")
    public ResponseEntity<Factura> obtenerFacturaPorId(@PathVariable Long id) {
        return facturaService.obtenerFacturaPorId(id)
                .map(factura -> new ResponseEntity<>(factura, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // POST /facturas -> Crear una factura
    // Este endpoint es el más complejo de usar, lo veremos en Postman.
    @PostMapping
    public ResponseEntity<?> crearFactura(@RequestBody Factura factura) {
        try {
            Factura nuevaFactura = facturaService.crearFactura(factura);
            return new ResponseEntity<>(nuevaFactura, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            // Captura errores de negocio (ej. "Stock insuficiente")
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST); // 400 Bad Request
        }
    }

    // Omitimos PUT y DELETE para facturas según la especificación mínima.
}