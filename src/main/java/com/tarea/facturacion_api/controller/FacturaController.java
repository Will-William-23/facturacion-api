package com.tarea.facturacion_api.controller;

import com.tarea.facturacion_api.model.Factura;
import com.tarea.facturacion_api.model.DetalleFactura;
import com.tarea.facturacion_api.model.Producto;
import com.tarea.facturacion_api.model.Cliente;
import com.tarea.facturacion_api.dto.FacturaRequest;
import com.tarea.facturacion_api.service.FacturaService;
import com.tarea.facturacion_api.service.SriService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/facturas")
public class FacturaController {

    @Autowired
    private FacturaService facturaService;

    @Autowired
    private SriService sriService; // <--- 1. IMPORTANTE: Inyectamos el servicio del SRI

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
    @PostMapping
    public ResponseEntity<?> crearFactura(@RequestBody FacturaRequest facturaRequest) {
        try {
            System.out.println("=== INICIANDO PROCESO DE FACTURACIÓN ===");
            System.out.println("Cliente ID: " + (facturaRequest.getCliente() != null ? facturaRequest.getCliente().id : "NULL"));
            System.out.println("Detalles: " + (facturaRequest.getDetalles() != null ? facturaRequest.getDetalles().size() : 0));
            System.out.println("Método Pago: " + facturaRequest.getMetodoPago());
            
            // Validar datos básicos
            if (facturaRequest.getCliente() == null || facturaRequest.getCliente().id == null) {
                return new ResponseEntity<>("Cliente no especificado", HttpStatus.BAD_REQUEST);
            }
            if (facturaRequest.getDetalles() == null || facturaRequest.getDetalles().isEmpty()) {
                return new ResponseEntity<>("Detalles de factura vacíos", HttpStatus.BAD_REQUEST);
            }
            
            // 1. Transformar FacturaRequest en Factura
            Factura factura = new Factura();
            
            // Cliente
            Cliente cliente = new Cliente();
            cliente.setId(facturaRequest.getCliente().id);
            factura.setCliente(cliente);
            
            // Detalles
            List<DetalleFactura> detalles = new ArrayList<>();
            for (FacturaRequest.DetalleVenta detVenta : facturaRequest.getDetalles()) {
                if (detVenta.producto == null || detVenta.producto.id == null) {
                    return new ResponseEntity<>("Producto no especificado en detalle", HttpStatus.BAD_REQUEST);
                }
                DetalleFactura detalle = new DetalleFactura();
                Producto producto = new Producto();
                producto.setId(detVenta.producto.id);
                detalle.setProducto(producto);
                detalle.setCantidad(detVenta.cantidad);
                detalle.setFactura(factura);
                detalles.add(detalle);
            }
            factura.setDetalles(detalles);
            
            // Método de Pago
            factura.setMetodoPago(facturaRequest.getMetodoPago());
            
            // Detalles de Pago (convertir a JSON string)
            if (facturaRequest.getDetallesPago() != null) {
                ObjectMapper mapper = new ObjectMapper();
                String detallesJson = mapper.writeValueAsString(facturaRequest.getDetallesPago());
                factura.setDetallesPago(detallesJson);
                System.out.println("Detalles de pago guardados: " + detallesJson);
            }
            
            // 2. Guardar la factura en la base de datos (Stock, Totales)
            System.out.println("Guardando factura en BD...");
            Factura nuevaFactura = facturaService.crearFactura(factura);
            System.out.println("✓ Factura guardada con ID: " + nuevaFactura.getId());
            
            // 3. AUTOMATIZACIÓN: Enviar inmediatamente al SRI (y esto dispara el WhatsApp)
            System.out.println("Enviando al SRI...");
            Factura facturaAutorizada = sriService.procesarFacturaElectronica(nuevaFactura.getId());
            System.out.println("✓ Proceso SRI finalizado. Estado: " + facturaAutorizada.getEstadoSri());

            return new ResponseEntity<>(facturaAutorizada, HttpStatus.CREATED);
            
        } catch (RuntimeException e) {
            System.err.println("ERROR RuntimeException: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            System.err.println("ERROR Exception: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error interno: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}