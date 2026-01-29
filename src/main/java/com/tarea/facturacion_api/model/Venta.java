package com.tarea.facturacion_api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "metodo_pago_id")
    private MetodoPago metodoPago;

    // Relación con el usuario que realizó la compra (opcional, si queremos rastrear
    // el login)
    // @ManyToOne
    // @JoinColumn(name = "usuario_id")
    // private Usuario usuario;

    private Double subtotal;
    private Double iva;
    private Double total;

    @Enumerated(EnumType.STRING)
    private EstadoVenta estado; // PENDING, COMPLETED, CANCELLED

    public enum EstadoVenta {
        PENDING,
        COMPLETED,
        CANCELLED
    }
}
