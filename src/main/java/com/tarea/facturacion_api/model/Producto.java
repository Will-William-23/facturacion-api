package com.tarea.facturacion_api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigoPrincipal; // Ej: PROD-001

    private String nombre;
    private String descripcion;
    private Double precio;
    private Integer stock;
    
    // true = Cobra IVA (15%), false = 0%
    private Boolean grabaIva; 
  // NUEVO CAMPO
    @Column(length = 500) // Longitud extra para URLs largas
    private String imagenUrl; 

    
}