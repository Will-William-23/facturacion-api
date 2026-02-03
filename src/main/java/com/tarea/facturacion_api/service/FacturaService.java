package com.tarea.facturacion_api.service;

import com.tarea.facturacion_api.model.Cliente;
import com.tarea.facturacion_api.model.DetalleFactura;
import com.tarea.facturacion_api.model.Factura;
import com.tarea.facturacion_api.model.Producto;
import com.tarea.facturacion_api.model.Configuracion;
import com.tarea.facturacion_api.repository.ClienteRepository;
import com.tarea.facturacion_api.repository.ConfiguracionRepository;
import com.tarea.facturacion_api.repository.FacturaRepository;
import com.tarea.facturacion_api.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FacturaService {

    @Autowired private FacturaRepository facturaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ConfiguracionRepository configRepo;

    public List<Factura> listarFacturas() {
        return facturaRepository.findAll();
    }

    public Optional<Factura> obtenerFacturaPorId(Long id) {
        return facturaRepository.findById(id);
    }

    @Transactional
    public Factura crearFactura(Factura factura) {
        Cliente cliente = clienteRepository.findById(factura.getCliente().getId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        factura.setCliente(cliente);

        Double ivaPorcentaje = 0.15;
        Optional<Configuracion> config = configRepo.findById(1L);
        if (config.isPresent() && config.get().getIvaPorcentaje() != null) {
            ivaPorcentaje = config.get().getIvaPorcentaje() / 100.0;
        }

        double acumuladorSubtotal = 0.0;
        double acumuladorIva = 0.0;

        for (DetalleFactura detalle : factura.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            
            if (producto.getStock() < detalle.getCantidad()) {
                throw new RuntimeException("Stock insuficiente: " + producto.getNombre());
            }

            producto.setStock(producto.getStock() - detalle.getCantidad());
            productoRepository.save(producto);

            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setProducto(producto);
            
            double subtotalLinea = detalle.getCantidad() * detalle.getPrecioUnitario();
            acumuladorSubtotal += subtotalLinea;
            
            if (producto.getGrabaIva() != null && producto.getGrabaIva()) {
                acumuladorIva += subtotalLinea * ivaPorcentaje;
            }
            
            detalle.setFactura(factura);
        }

        // Redondeos seguros
        factura.setSubtotal(round(acumuladorSubtotal));
        factura.setTotalIva(round(acumuladorIva));
        factura.setTotal(round(acumuladorSubtotal + acumuladorIva));
        
        factura.setFecha(LocalDateTime.now());
        factura.setEstadoSri("PENDIENTE");
        
        return facturaRepository.save(factura);
    }

    private Double round(Double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}