package com.tarea.facturacion_api.controller;

import com.tarea.facturacion_api.model.Factura;
import com.tarea.facturacion_api.repository.FacturaRepository;
import com.tarea.facturacion_api.service.FacturaService;
import com.tarea.facturacion_api.service.SriService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/facturas")
public class FacturaController {

    @Autowired
    private FacturaService facturaService;

    @Autowired
    private SriService sriService;

    // --- ESTA ES LA VARIABLE QUE FALTABA O DABA ERROR ---
    @Autowired
    private FacturaRepository facturaRepository; 
    // ----------------------------------------------------

    @GetMapping
    public List<Factura> listarFacturas() {
        return facturaService.listarFacturas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Factura> obtenerFacturaPorId(@PathVariable Long id) {
        return facturaService.obtenerFacturaPorId(id)
                .map(factura -> new ResponseEntity<>(factura, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Endpoint de Filtro
    @GetMapping("/filtro")
    public List<Factura> filtrarFacturas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        
        // Convertimos LocalDate a LocalDateTime (Inicio del día -> Fin del día)
        return facturaRepository.findByFechaBetween(inicio.atStartOfDay(), fin.atTime(LocalTime.MAX));
    }

    @PostMapping
    public ResponseEntity<?> crearFactura(@RequestBody Factura factura) {
        try {
            Factura nuevaFactura = facturaService.crearFactura(factura);
            Factura facturaAutorizada = sriService.procesarFacturaElectronica(nuevaFactura.getId());
            return new ResponseEntity<>(facturaAutorizada, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}