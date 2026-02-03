package com.tarea.facturacion_api.controller;

import com.tarea.facturacion_api.model.Cliente;
import com.tarea.facturacion_api.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Cliente cliente) {
        // 1. Validar Cédula Duplicada
        if (clienteRepository.existsByCedula(cliente.getCedula())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("error", "Ya existe un cliente registrado con la Cédula/RUC: " + cliente.getCedula()));
        }

        // 2. Validar Email Duplicado (Opcional, pero recomendado)
        if (cliente.getEmail() != null && !cliente.getEmail().isEmpty()) {
            if (clienteRepository.existsByEmail(cliente.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("error", "El correo electrónico ya está en uso por otro cliente."));
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(clienteRepository.save(cliente));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Cliente datos) {
        return clienteRepository.findById(id)
            .map(c -> {
                c.setNombre(datos.getNombre());
                c.setApellido(datos.getApellido());
                c.setDireccion(datos.getDireccion());
                c.setTelefono(datos.getTelefono());
                c.setEmail(datos.getEmail());
                // No actualizamos la cédula por seguridad
                return ResponseEntity.ok(clienteRepository.save(c));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (clienteRepository.existsById(id)) {
            clienteRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}