package com.tarea.facturacion_api.controller;

import com.tarea.facturacion_api.model.Configuracion;
import com.tarea.facturacion_api.repository.ConfiguracionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/configuracion")
public class ConfiguracionController {

    @Autowired
    private ConfiguracionRepository configRepo;

    @GetMapping
    public ResponseEntity<Configuracion> obtenerConfig() {
        return configRepo.findById(1L)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    Configuracion def = new Configuracion();
                    def.setNombreEmpresa("EMPRESA DEMO");
                    def.setRuc("9999999999001");
                    def.setDireccion("Ecuador");
                    def.setTelefono("0999999999");
                    def.setIvaPorcentaje(15.0);
                    def.setObligadoContabilidad("NO");
                    return ResponseEntity.ok(configRepo.save(def));
                });
    }

    @PutMapping
    public ResponseEntity<Configuracion> actualizar(@RequestBody Configuracion datos) {
        Configuracion conf = configRepo.findById(1L).orElse(new Configuracion());
        
        // Actualizamos TODOS los campos
        conf.setNombreEmpresa(datos.getNombreEmpresa());
        conf.setRuc(datos.getRuc());
        conf.setDireccion(datos.getDireccion());
        conf.setTelefono(datos.getTelefono());
        conf.setIvaPorcentaje(datos.getIvaPorcentaje());
        
        // --- CAMPOS NUEVOS QUE FALTABAN ---
        conf.setEmail(datos.getEmail());
        conf.setSitioWeb(datos.getSitioWeb());
        conf.setObligadoContabilidad(datos.getObligadoContabilidad());
        // ----------------------------------

        return ResponseEntity.ok(configRepo.save(conf));
    }
}