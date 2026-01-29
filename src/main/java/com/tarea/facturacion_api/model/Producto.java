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

    private String nombre;
    private String descripcion;
    private Double precio; // Precio de Venta al Público
    private Double precioCompra; // Precio de Compra al Proveedor
    private Integer stock; // Usamos Integer para el stock

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String imagen;

    private String tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "productos" })
    private Proveedor proveedor;
}