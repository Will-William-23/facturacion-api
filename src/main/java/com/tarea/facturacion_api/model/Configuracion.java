package com.tarea.facturacion_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "configuracion")
public class Configuracion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreEmpresa;
    private String ruc;
    private String direccion;
    private String telefono;
    
    // --- NUEVOS CAMPOS ---
    private String email;
    private String sitioWeb;
    private String obligadoContabilidad; // SI o NO
    // ---------------------

    private Double ivaPorcentaje;
}