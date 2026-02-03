package com.tarea.facturacion_api.controller;

import com.tarea.facturacion_api.model.Compra;
import com.tarea.facturacion_api.service.CompraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/compras")
public class CompraController {

    @Autowired private CompraService compraService;

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Compra compra) {
        try {
            return ResponseEntity.ok(compraService.registrarCompra(compra));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}