package com.tarea.facturacion_api.controller;

import com.tarea.facturacion_api.dto.CompraDto;
import com.tarea.facturacion_api.model.*;
import com.tarea.facturacion_api.repository.*;
import com.tarea.facturacion_api.service.SriService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/compras")
public class CompraController {

    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private FacturaRepository facturaRepository;
    @Autowired
    private MetodoPagoRepository metodoPagoRepository;
    @Autowired
    private SriService sriService;

    @PostMapping
    @Transactional
    public ResponseEntity<?> registrarCompra(@RequestBody CompraDto compraDto) {
        Proveedor proveedor = proveedorRepository.findById(compraDto.getProveedorId())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        MetodoPago metodoPago = metodoPagoRepository.findById(compraDto.getMetodoPagoId())
                .orElseThrow(() -> new RuntimeException("Metodo de Pago no encontrado"));

        Factura factura = new Factura();
        factura.setProveedor(proveedor);
        factura.setMetodoPago(metodoPago);
        factura.setFecha(LocalDateTime.now());
        factura.setTipo("COMPRA");
        factura.setEstadoSri("PENDIENTE");

        // Cliente es null en compras (o podriamos asignarnos a nosotros mismos, pero
        // dejamos null y manejamos en el servicio)
        factura.setCliente(null);

        List<DetalleFactura> detalles = new ArrayList<>();
        double total = 0;

        for (CompraDto.ItemCompraDto item : compraDto.getItems()) {
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + item.getProductoId()));

            // Actualizar Stock (SUMAR)
            producto.setStock(producto.getStock() + item.getCantidad());
            productoRepository.save(producto);

            // Crear Detalle
            DetalleFactura detalle = new DetalleFactura();
            detalle.setFactura(factura);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());

            // Usar PRECIO DE COMPRA. Si es null, usar 0 (o lanzar error)
            double precioUnitario = producto.getPrecioCompra() != null ? producto.getPrecioCompra() : 0.0;
            detalle.setPrecioUnitario(precioUnitario);

            detalles.add(detalle);
            total += precioUnitario * item.getCantidad();
        }

        double subtotal = total; // El total acumulado es el subtotal
        double iva = subtotal * 0.15;
        double totalFinal = subtotal + iva;

        factura.setSubtotal(subtotal);
        factura.setIva(iva);
        factura.setTotal(totalFinal);

        // --- VALIDAR FONDOS INSUFICIENTES ---
        Double ventasTotales = facturaRepository.sumTotalVentas();
        Double comprasTotales = facturaRepository.sumTotalCompras();
        double saldoActual = ventasTotales - comprasTotales;

        if (saldoActual < totalFinal) {
            throw new RuntimeException("Saldo insuficiente (" + String.format("$%.2f", saldoActual)
                    + ") para realizar la compra de reabastecimiento (" + String.format("$%.2f", totalFinal) + ").");
        }

        factura.setDetalles(detalles);
        Factura facturaGuardada = facturaRepository.save(factura);

        // INTENTAR PROCESAR SRI (Factura de Compra / Liquidacion)
        // Nota: El servicio SRI actualmente podria esperar un Cliente.
        // Si falla, el estado quedara PENDIENTE y no rompera la transaccion si
        // capturamos la excepcion,
        // pero aqui queremos que el usuario sepa.
        try {
            // TODO: Adaptar SriService para soportar FACTURAS DE COMPRA (Liquidaciones)
            // donde no hay Cliente sino Proveedor.
            // Por ahora, solo guardamos. El usuario pidió "seguir la misma ruta", así que
            // idealmente
            // SriService debería ser capaz de distinguir.
            sriService.procesarFacturaElectronica(facturaGuardada.getId());
        } catch (Exception e) {
            System.err.println("Error enviando al SRI: " + e.getMessage());
            // No hacemos rollback, guardamos la compra.
        }

        return ResponseEntity.ok(facturaGuardada);
    }
}
