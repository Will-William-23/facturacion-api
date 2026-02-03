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
    
    private Double subtotal; // Nuevo
    private Double totalIva; // Nuevo
    private Double total;

    // --- CAMPOS SRI ---
    @Column(length = 49, unique = true)
    private String claveAcceso;
    private String estadoSri;
    private LocalDateTime fechaAutorizacion;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String xmlContenido;
    
    private String formaPago; 

    // --- RELACIONES ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<DetalleFactura> detalles;
}