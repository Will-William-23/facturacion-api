package com.tarea.facturacion_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "metodos_pago")
@Data
public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo; // Ejemplo: "01", "19"

    @Column(nullable = false)
    private String descripcion; // Ejemplo: "SIN UTILIZACION DEL SISTEMA FINANCIERO"
}
