package com.tarea.facturacion_api.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "facturas")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha;
    private Double total;

    // --- RELACIONES ---

    // Relación: Muchas facturas pertenecen a UN cliente
    @ManyToOne(fetch = FetchType.EAGER) // EAGER: Cargar el cliente junto con la factura
    @JoinColumn(name = "cliente_id") // Nombre de la columna de la clave foránea en la tabla 'facturas'
    private Cliente cliente;

    // Relación: Una factura tiene MUCHOS detalles
    // 'mappedBy = "factura"' le dice a JPA que la relación ya está definida en el campo 'factura' de la clase 'DetalleFactura'
    // CascadeType.ALL: Si borro una factura, se borran sus detalles.
    // OrphanRemoval=true: Si quito un detalle de esta lista, se borra de la DB.
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference // Ayuda a evitar bucles infinitos al convertir a JSON
    private List<DetalleFactura> detalles;
}