package com.tarea.facturacion_api.service;

import com.tarea.facturacion_api.dto.SolicitudCompraDto;
import com.tarea.facturacion_api.model.Cliente;
import com.tarea.facturacion_api.model.DetalleVenta;
import com.tarea.facturacion_api.model.Producto;
import com.tarea.facturacion_api.model.Venta;
import com.tarea.facturacion_api.repository.ClienteRepository;
import com.tarea.facturacion_api.repository.DetalleVentaRepository;
import com.tarea.facturacion_api.repository.ProductoRepository;
import com.tarea.facturacion_api.repository.VentaRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private com.tarea.facturacion_api.repository.FacturaRepository facturaRepository;
    @Autowired
    private com.tarea.facturacion_api.repository.DetalleFacturaRepository detalleFacturaRepository;
    @Autowired
    private SriService sriService;

    @Transactional
    public Venta realizarVenta(SolicitudCompraDto solicitud) {
        // 1. Validar Cliente
        Cliente cliente = clienteRepository.findById(solicitud.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // 2. Crear Venta inicial
        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setFecha(LocalDateTime.now());
        venta.setEstado(Venta.EstadoVenta.COMPLETED);

        // 3. Procesar Items
        List<DetalleVenta> detalles = new ArrayList<>();
        double totalVenta = 0.0;

        for (SolicitudCompraDto.ItemCompraDto item : solicitud.getItems()) {
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + item.getProductoId()));

            // Validar Stock
            if (producto.getStock() < item.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre());
            }

            // Actualizar Stock
            producto.setStock(producto.getStock() - item.getCantidad());
            productoRepository.save(producto);

            // Crear Detalle
            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio() * item.getCantidad());

            detalles.add(detalle);
            totalVenta += detalle.getSubtotal();
        }

        // 4. Guardar Venta con Total
        venta.setTotal(totalVenta);
        Venta ventaGuardada = ventaRepository.save(venta);

        // 5. Guardar Detalles
        for (DetalleVenta detalle : detalles) {
            detalle.setVenta(ventaGuardada);
            detalleVentaRepository.save(detalle);
        }

        // --- 6. INTEGRACIÓN SRI: GENERAR FACTURA AUTOMÁTICA ---
        try {
            System.out.println(">>> [VENTA SERVICE] Iniciando generación automática de Factura SRI...");
            generarFacturaSriDesdeVenta(ventaGuardada, detalles);
        } catch (Exception e) {
            System.err.println(">>> [VENTA SERVICE] Error al generar factura SRI: " + e.getMessage());
            // No lanzamos excepción para no revertir la venta si falla el SRI (podría ser
            // un fallo de conexión)
            // Opcionalmente, podríamos marcar la venta con un flag "factura_pendiente".
        }

        return ventaGuardada;
    }

    private void generarFacturaSriDesdeVenta(Venta venta, List<DetalleVenta> detallesVenta) {
        // A. Crear la Factura (Espejo de la Venta)
        com.tarea.facturacion_api.model.Factura factura = new com.tarea.facturacion_api.model.Factura();
        factura.setCliente(venta.getCliente());
        factura.setFecha(venta.getFecha());
        factura.setTotal(venta.getTotal());
        factura.setEstadoSri("PENDIENTE"); // Estado inicial

        // Guardar Factura
        com.tarea.facturacion_api.model.Factura facturaGuardada = facturaRepository.save(factura);

        // B. Crear Detalles de Factura
        List<com.tarea.facturacion_api.model.DetalleFactura> detallesFactura = new ArrayList<>();
        for (DetalleVenta dv : detallesVenta) {
            com.tarea.facturacion_api.model.DetalleFactura df = new com.tarea.facturacion_api.model.DetalleFactura();
            df.setFactura(facturaGuardada);
            df.setProducto(dv.getProducto());
            df.setCantidad(dv.getCantidad());
            df.setPrecioUnitario(dv.getPrecioUnitario());

            // Nota: Aquí NO descontamos stock, porque ya lo hizo la Venta arriba.
            detallesFactura.add(df);
            detalleFacturaRepository.save(df);
        }

        // --- CORRECCIÓN IMPORTANTE ---
        // Actualizamos la lista de detalles en el objeto Factura en memoria
        // para que cuando SriService lo lea (en la misma transacción), no tenga la
        // lista nula.
        facturaGuardada.setDetalles(detallesFactura);

        // C. Enviar al SRI
        sriService.procesarFacturaElectronica(facturaGuardada.getId());
    }
}
