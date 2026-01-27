package com.tarea.facturacion_api.controller;

import com.tarea.facturacion_api.model.MetodoPago;
import com.tarea.facturacion_api.repository.MetodoPagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/metodos-pago")
public class MetodoPagoController {

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    @GetMapping
    public List<MetodoPago> listarMetodosPago() {
        return metodoPagoRepository.findAll();
    }
}
